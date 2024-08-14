package no.fdk.fdk_event_harvester.harvester

import no.fdk.fdk_event_harvester.configuration.ApplicationProperties
import no.fdk.fdk_event_harvester.adapter.EventAdapter
import no.fdk.fdk_event_harvester.adapter.OrganizationsAdapter
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

@Service
class EventHarvester(
    private val adapter: EventAdapter,
    private val orgAdapter: OrganizationsAdapter,
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
                        parseRDFResponse(adapter.getEvents(source), jenaWriterType),
                        source.id, source.url, harvestDate, source.publisherId, forceUpdate
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

    private fun updateIfChanged(harvested: Model, sourceId: String, sourceURL: String, harvestDate: Calendar,
                                publisherId: String?, forceUpdate: Boolean): HarvestReport {
        val dbData = turtleService.getHarvestSource(sourceURL)
            ?.let { safeParseRDF(it, Lang.TURTLE) }

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

            updateDB(harvested, sourceId, sourceURL, harvestDate, publisherId, forceUpdate)
        }
    }

    private fun updateDB(harvested: Model, sourceId: String, sourceURL: String, harvestDate: Calendar,
                         publisherId: String?, forceUpdate: Boolean): HarvestReport {
        val allEvents = splitEventsFromRDF(harvested, sourceURL)
        val updatedEvents = updateEvents(allEvents, harvestDate, forceUpdate)

        val organization = if (publisherId != null && allEvents.containsFreeServices()) {
            orgAdapter.getOrganization(publisherId)
        } else null

        val catalogs = splitCatalogsFromRDF(harvested, allEvents, sourceURL, organization)
        val updatedCatalogs = updateCatalogs(catalogs, harvestDate, forceUpdate)

        val removedEvents = getEventsRemovedThisHarvest(
            updatedCatalogs.map { catalogFdkUri(it.fdkId) },
            allEvents.map { it.eventURI }
        )
        removedEvents.map { it.copy(removed = true) }
            .run { eventMetaRepository.saveAll(this) }

        return HarvestReport(
            id = sourceId,
            url = sourceURL,
            harvestError = false,
            startTime = harvestDate.formatWithOsloTimeZone(),
            endTime = formatNowWithOsloTimeZone(),
            changedCatalogs = updatedCatalogs,
            changedResources = updatedEvents,
            removedResources = removedEvents.map { FdkIdAndUri(fdkId = it.fdkId, uri = it.uri) }
        )
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

                it.first.events.forEach { eventURI -> addIsPartOfToEvents(eventURI, catalogFdkUri(updatedMeta.fdkId)) }

                FdkIdAndUri(fdkId = updatedMeta.fdkId, uri = updatedMeta.uri)
            }

    private fun CatalogRDFModel.mapToMetaDBO(
        harvestDate: Calendar,
        dbMeta: CatalogMeta?
    ): CatalogMeta {
        val catalogURI = resourceURI
        val fdkId = dbMeta?.fdkId ?: createIdFromString(catalogURI)
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
        events.mapNotNull {
            it.updateDBOs(harvestDate, forceUpdate)
                ?.let { meta -> FdkIdAndUri(fdkId = meta.fdkId, uri = it.eventURI) }
        }

    private fun EventRDFModel.updateDBOs(harvestDate: Calendar, forceUpdate: Boolean): EventMeta? {
        val dbMeta = eventMetaRepository.findByIdOrNull(eventURI)
        return when {
            dbMeta == null || dbMeta.removed || hasChanges(dbMeta.fdkId) -> {
                val updatedMeta = mapToMetaDBO(harvestDate, dbMeta)
                eventMetaRepository.save(updatedMeta)
                turtleService.saveAsEvent(harvested, updatedMeta.fdkId, false)

                updatedMeta
            }
            forceUpdate -> {
                turtleService.saveAsEvent(
                    model = harvested,
                    fdkId = dbMeta.fdkId,
                    withRecords = false
                )
                dbMeta
            }
            else -> null
        }
    }

    private fun EventRDFModel.mapToMetaDBO(
        harvestDate: Calendar,
        dbMeta: EventMeta?
    ): EventMeta {
        val fdkId = dbMeta?.fdkId ?: createIdFromString(eventURI)
        val issued = dbMeta?.issued
            ?.let { timestamp -> calendarFromTimestamp(timestamp) }
            ?: harvestDate

        return EventMeta(
            uri = eventURI,
            fdkId = fdkId,
            isPartOf = dbMeta?.isPartOf,
            issued = issued.timeInMillis,
            modified = harvestDate.timeInMillis
        )
    }

    private fun catalogFdkUri(fdkId: String): String =
        "${applicationProperties.eventsUri}/catalogs/${fdkId}"

    private fun getEventsRemovedThisHarvest(catalogs: List<String>, events: List<String>): List<EventMeta> =
        catalogs.flatMap { eventMetaRepository.findAllByIsPartOf(it) }
            .filter { !it.removed && !events.contains(it.uri) }

    private fun addIsPartOfToEvents(eventURI: String, catalogURI: String) =
        eventMetaRepository.findByIdOrNull(eventURI)
            ?.run { if (isPartOf != catalogURI) eventMetaRepository.save(copy(isPartOf = catalogURI)) }

    private fun CatalogRDFModel.hasChanges(fdkId: String?): Boolean =
        if (fdkId == null) true
        else harvestDiff(turtleService.getCatalog(fdkId, withRecords = false))

    private fun EventRDFModel.hasChanges(fdkId: String?): Boolean =
        if (fdkId == null) true
        else harvestDiff(turtleService.getEvent(fdkId, withRecords = false))
}
