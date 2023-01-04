package no.fdk.fdk_event_harvester.harvester

import no.fdk.fdk_event_harvester.configuration.ApplicationProperties
import no.fdk.fdk_event_harvester.adapter.EventAdapter
import no.fdk.fdk_event_harvester.model.*
import no.fdk.fdk_event_harvester.rdf.*
import no.fdk.fdk_event_harvester.repository.*
import no.fdk.fdk_event_harvester.service.*
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

private val LOGGER = LoggerFactory.getLogger(EventHarvester::class.java)
private const val dateFormat: String = "yyyy-MM-dd HH:mm:ss Z"

@Service
class EventHarvester(
    private val adapter: EventAdapter,
    private val eventMetaRepository: EventMetaRepository,
    private val catalogMetaRepository: CatalogMetaRepository,
    private val turtleService: TurtleService,
    private val applicationProperties: ApplicationProperties
) {

    fun harvestEvents(source: HarvestDataSource, harvestDate: Calendar, forceUpdate: Boolean): HarvestReport? =
        if (source.id != null && source.url != null) {

            try {
                LOGGER.debug("Starting harvest of ${source.url}")

                when (val jenaWriterType = jenaTypeFromAcceptHeader(source.acceptHeaderValue)) {
                    null -> {
                        LOGGER.error(
                            "Not able to harvest from ${source.url}, no accept header supplied",
                            HarvestException(source.url)
                        )
                        HarvestReport(
                            id = source.id,
                            url = source.url,
                            harvestError = true,
                            errorMessage = "Not able to harvest, no accept header supplied",
                            startTime = harvestDate.formatWithOsloTimeZone(),
                            endTime = formatNowWithOsloTimeZone()
                        )
                    }
                    Lang.RDFNULL -> {
                        LOGGER.error(
                            "Not able to harvest from ${source.url}, header ${source.acceptHeaderValue} is not acceptable",
                            HarvestException(source.url)
                        )
                        HarvestReport(
                            id = source.id,
                            url = source.url,
                            harvestError = true,
                            errorMessage = "Not able to harvest, no accept header supplied",
                            startTime = harvestDate.formatWithOsloTimeZone(),
                            endTime = formatNowWithOsloTimeZone()
                        )
                    }
                    else -> updateIfChanged(
                        parseRDFResponse(adapter.getEvents(source), jenaWriterType, source.url),
                        source.id, source.url, harvestDate, forceUpdate
                    )
                }
            } catch (ex: Exception) {
                LOGGER.error("Harvest of ${source.url} failed", ex)
                HarvestReport(
                    id = source.id,
                    url = source.url,
                    harvestError = true,
                    errorMessage = ex.message,
                    startTime = harvestDate.formatWithOsloTimeZone(),
                    endTime = formatNowWithOsloTimeZone()
                )
            }
        } else {
            LOGGER.error("Harvest source is not defined", HarvestException("undefined"))
            null
        }

    private fun updateIfChanged(harvested: Model, sourceId: String, sourceURL: String, harvestDate: Calendar, forceUpdate: Boolean): HarvestReport {
        val dbData = turtleService.getHarvestSource(sourceURL)
            ?.let { parseRDFResponse(it, Lang.TURTLE, null) }

        return if (!forceUpdate && dbData != null && harvested.isIsomorphicWith(dbData)) {
            LOGGER.info("No changes from last harvest of $sourceURL")
            HarvestReport(
                id = sourceId,
                url = sourceURL,
                harvestError = false,
                startTime = harvestDate.formatWithOsloTimeZone(),
                endTime = formatNowWithOsloTimeZone()
            )
        } else {
            LOGGER.info("Changes detected, saving data from $sourceURL, and updating FDK meta data")
            turtleService.saveAsHarvestSource(harvested, sourceURL)

            updateDB(harvested, sourceId, sourceURL, harvestDate, forceUpdate)
        }
    }

    private fun updateDB(harvested: Model, sourceId: String, sourceURL: String, harvestDate: Calendar, forceUpdate: Boolean): HarvestReport {
        val allEvents = splitEventsFromRDF(harvested, sourceURL)
        return if (allEvents.isEmpty()) {
            LOGGER.warn("No events found in data harvested from $sourceURL")
            HarvestReport(
                id = sourceId,
                url = sourceURL,
                harvestError = true,
                errorMessage = "No events found in data harvested from $sourceURL",
                startTime = harvestDate.formatWithOsloTimeZone(),
                endTime = formatNowWithOsloTimeZone()
            )
        } else {
            val updatedEvents = updateEvents(allEvents, harvestDate, forceUpdate)

            val catalogs = splitCatalogsFromRDF(harvested, allEvents, sourceURL)
            val updatedCatalogs = updateCatalogs(catalogs, harvestDate, forceUpdate)

            return HarvestReport(
                id = sourceId,
                url = sourceURL,
                harvestError = false,
                startTime = harvestDate.formatWithOsloTimeZone(),
                endTime = formatNowWithOsloTimeZone(),
                changedCatalogs = updatedCatalogs,
                changedResources = updatedEvents
            )
        }
    }

    private fun updateCatalogs(catalogs: List<CatalogRDFModel>, harvestDate: Calendar, forceUpdate: Boolean): List<FdkIdAndUri> =
        catalogs
            .map { Pair(it, catalogMetaRepository.findByIdOrNull(it.resourceURI)) }
            .filter { forceUpdate || it.first.hasChanges(it.second?.fdkId) }
            .map {
                val updatedMeta = it.first.mapToMetaDBO(harvestDate, it.second)
                catalogMetaRepository.save(updatedMeta)

                turtleService.saveAsCatalog(
                    model = it.first.harvested,
                    fdkId = updatedMeta.fdkId,
                    withRecords = false
                )

                val fdkUri = "${applicationProperties.eventsUri}/catalogs/${updatedMeta.fdkId}"
                it.first.events.forEach { eventURI -> addIsPartOfToEvents(eventURI, fdkUri) }

                FdkIdAndUri(fdkId = updatedMeta.fdkId, uri = updatedMeta.uri)
            }

    private fun CatalogRDFModel.mapToMetaDBO(
        harvestDate: Calendar,
        dbMeta: CatalogMeta?
    ): CatalogMeta {
        val catalogURI = resourceURI
        val fdkId = dbMeta?.fdkId ?: createIdFromUri(catalogURI)
        val issued = dbMeta?.issued
            ?.let { timestamp -> calendarFromTimestamp(timestamp) }
            ?: harvestDate

        return CatalogMeta(
            uri = catalogURI,
            fdkId = fdkId,
            issued = issued.timeInMillis,
            modified = harvestDate.timeInMillis,
            events = events
        )
    }

    private fun updateEvents(events: List<EventRDFModel>, harvestDate: Calendar, forceUpdate: Boolean): List<FdkIdAndUri> =
        events
            .map { Pair(it, eventMetaRepository.findByIdOrNull(it.eventURI)) }
            .filter { forceUpdate || it.first.hasChanges(it.second?.fdkId) }
            .map {
                val updatedMeta = it.first.mapToMetaDBO(harvestDate, it.second)
                eventMetaRepository.save(updatedMeta)
                turtleService.saveAsEvent(it.first.harvested, updatedMeta.fdkId, false)

                FdkIdAndUri(fdkId = updatedMeta.fdkId, uri = it.first.eventURI)
            }

    private fun EventRDFModel.mapToMetaDBO(
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

    private fun addIsPartOfToEvents(eventURI: String, catalogURI: String) =
        eventMetaRepository.findByIdOrNull(eventURI)
            ?.run { eventMetaRepository.save(copy(isPartOf = catalogURI)) }

    private fun CatalogRDFModel.hasChanges(fdkId: String?): Boolean =
        if (fdkId == null) true
        else harvestDiff(turtleService.getCatalog(fdkId, withRecords = false))

    private fun EventRDFModel.hasChanges(fdkId: String?): Boolean =
        if (fdkId == null) true
        else harvestDiff(turtleService.getEvent(fdkId, withRecords = false))

    private fun formatNowWithOsloTimeZone(): String =
        ZonedDateTime.now(ZoneId.of("Europe/Oslo"))
            .format(DateTimeFormatter.ofPattern(dateFormat))

    private fun Calendar.formatWithOsloTimeZone(): String =
        ZonedDateTime.from(toInstant().atZone(ZoneId.of("Europe/Oslo")))
            .format(DateTimeFormatter.ofPattern(dateFormat))
}
