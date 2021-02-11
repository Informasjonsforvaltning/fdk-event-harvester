package no.fdk.fdk_event_harvester.service

import no.fdk.fdk_event_harvester.adapter.FusekiAdapter
import no.fdk.fdk_event_harvester.configuration.ApplicationProperties
import no.fdk.fdk_event_harvester.harvester.calendarFromTimestamp
import no.fdk.fdk_event_harvester.model.*
import no.fdk.fdk_event_harvester.rdf.parseRDFResponse
import no.fdk.fdk_event_harvester.repository.*
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.springframework.stereotype.Service

@Service
class UpdateService (
    private val applicationProperties: ApplicationProperties,
    private val fusekiAdapter: FusekiAdapter,
    private val eventMetaRepository: EventMetaRepository,
    private val turtleService: TurtleService
) {

    fun updateUnionModel() {
        var unionModel = ModelFactory.createDefaultModel()

        eventMetaRepository.findAll()
            .forEach {
                turtleService.getEvent(it.fdkId, withRecords = true)
                    ?.let { dboTurtle -> parseRDFResponse(dboTurtle, Lang.TURTLE, null) }
                    ?.run { unionModel = unionModel.union(this) }
            }

        fusekiAdapter.storeUnionModel(unionModel)

        turtleService.saveAsUnion(unionModel)
    }

    fun updateMetaData() {
        eventMetaRepository.findAll()
            .forEach { event ->
                val fdkURI = "${applicationProperties.eventsUri}/${event.fdkId}"
                val catalogMeta = event.createMetaModel()

                turtleService.getEvent(event.fdkId, withRecords = false)
                    ?.let { eventNoRecords -> parseRDFResponse(eventNoRecords, Lang.TURTLE, null) }
                    ?.let { eventModelNoRecords -> catalogMeta.union(eventModelNoRecords) }
                    ?.run { turtleService.saveAsEvent(this, event.fdkId, withRecords = true) }
            }

        updateUnionModel()
    }

    private fun EventMeta.createMetaModel(): Model {
        val fdkUri = "${applicationProperties.eventsUri}/$fdkId"

        val metaModel = ModelFactory.createDefaultModel()

        metaModel.createResource(fdkUri)
            .addProperty(RDF.type, DCAT.CatalogRecord)
            .addProperty(DCTerms.identifier, fdkId)
            .addProperty(FOAF.primaryTopic, metaModel.createResource(uri))
            .addProperty(DCTerms.issued, metaModel.createTypedLiteral(calendarFromTimestamp(issued)))
            .addProperty(DCTerms.modified, metaModel.createTypedLiteral(calendarFromTimestamp(modified)))

        return metaModel
    }
}
