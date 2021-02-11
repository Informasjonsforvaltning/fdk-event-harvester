package no.fdk.fdk_event_harvester.harvester

import no.fdk.fdk_event_harvester.configuration.ApplicationProperties
import no.fdk.fdk_event_harvester.adapter.EventAdapter
import no.fdk.fdk_event_harvester.model.*
import no.fdk.fdk_event_harvester.rdf.*
import no.fdk.fdk_event_harvester.repository.*
import no.fdk.fdk_event_harvester.service.*
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

private val LOGGER = LoggerFactory.getLogger(EventHarvester::class.java)

@Service
class EventHarvester(
    private val adapter: EventAdapter,
    private val metaRepository: EventMetaRepository,
    private val turtleService: TurtleService,
    private val applicationProperties: ApplicationProperties
) {

    fun harvestEvents(source: HarvestDataSource, harvestDate: Calendar) =
        source.url?.let { sourceURL ->
            LOGGER.debug("Starting harvest of $sourceURL")
            val jenaWriterType = jenaTypeFromAcceptHeader(source.acceptHeaderValue)

            val harvested = when (jenaWriterType) {
                null -> null
                Lang.RDFNULL -> null
                else -> adapter.getEvents(source)?.let { parseRDFResponse(it, jenaWriterType, sourceURL) }
            }

            when {
                jenaWriterType == null -> LOGGER.error("Not able to harvest from $sourceURL, no accept header supplied")
                jenaWriterType == Lang.RDFNULL -> LOGGER.error("Not able to harvest from $sourceURL, header ${source.acceptHeaderValue} is not acceptable ")
                harvested == null -> LOGGER.info("Not able to harvest $sourceURL")
                else -> updateIfHarvestedContainsChanges(harvested, sourceURL, harvestDate)
            }
        } ?: LOGGER.error("Harvest source is not defined")

    private fun updateIfHarvestedContainsChanges(harvested: Model, sourceURL: String, harvestDate: Calendar) {
        val dbData = turtleService.getHarvestSource(sourceURL)
            ?.let { parseRDFResponse(it, Lang.TURTLE, null) }

        if (dbData != null && harvested.isIsomorphicWith(dbData)) {
            LOGGER.info("No changes from last harvest of $sourceURL")
        } else {
            LOGGER.info("Changes detected, saving data from $sourceURL, and updating FDK meta data")
            turtleService.saveAsHarvestSource(harvested, sourceURL)

            val events = splitEventsFromRDF(harvested)

            if (events.isEmpty()) LOGGER.error("No events found in data harvested from $sourceURL")
            else updateDB(events, harvestDate)
        }
    }

    private fun updateDB(events: List<EventRDFModel>, harvestDate: Calendar) {
        events
            .map { Pair(it, metaRepository.findByIdOrNull(it.eventURI)) }
            .filter { it.first.eventHasChanges(it.second?.fdkId) }
            .forEach {
                val updatedMeta = it.first.updateMeta(harvestDate, it.second)
                metaRepository.save(updatedMeta)

                turtleService.saveAsEvent(it.first.harvested, updatedMeta.fdkId, false)

                val fdkUri = "${applicationProperties.eventsUri}/${updatedMeta.fdkId}"

                val metaModel = ModelFactory.createDefaultModel()
                metaModel.createResource(fdkUri)
                    .addProperty(RDF.type, DCAT.CatalogRecord)
                    .addProperty(DCTerms.identifier, updatedMeta.fdkId)
                    .addProperty(FOAF.primaryTopic, metaModel.createResource(updatedMeta.uri))
                    .addProperty(DCTerms.issued, metaModel.createTypedLiteral(calendarFromTimestamp(updatedMeta.issued)))
                    .addProperty(DCTerms.modified, metaModel.createTypedLiteral(harvestDate))

                turtleService.saveAsEvent(metaModel.union(it.first.harvested), updatedMeta.fdkId, true)
            }
    }

    private fun EventRDFModel.updateMeta(
        harvestDate: Calendar,
        dbMeta: EventMeta?
    ): EventMeta {
        val fdkId = dbMeta?.fdkId ?: createIdFromUri(eventURI)
        val issued = dbMeta?.issued
            ?.let { timestamp -> calendarFromTimestamp(timestamp) }
            ?: harvestDate

        return EventMeta(
            uri = eventURI,
            fdkId = fdkId,
            issued = issued.timeInMillis,
            modified = harvestDate.timeInMillis
        )
    }

    private fun EventRDFModel.eventHasChanges(fdkId: String?): Boolean =
        if (fdkId == null) true
        else harvestDiff(turtleService.getEvent(fdkId, withRecords = false))
}
