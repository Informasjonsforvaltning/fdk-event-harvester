package no.fdk.fdk_event_harvester.service

import no.fdk.fdk_event_harvester.harvester.formatNowWithOsloTimeZone
import no.fdk.fdk_event_harvester.model.FdkIdAndUri
import no.fdk.fdk_event_harvester.model.HarvestReport
import no.fdk.fdk_event_harvester.rabbit.RabbitMQPublisher
import no.fdk.fdk_event_harvester.rdf.createRDFResponse
import no.fdk.fdk_event_harvester.rdf.parseRDFResponse
import no.fdk.fdk_event_harvester.repository.EventMetaRepository
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class EventService(
    private val eventMetaRepository: EventMetaRepository,
    private val rabbitPublisher: RabbitMQPublisher,
    private val turtleService: TurtleService
) {

    fun getAllEvents(returnType: Lang, withRecords: Boolean): String =
        turtleService.getEventUnion(withRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE).createRDFResponse(returnType)
            }
            ?: ModelFactory.createDefaultModel().createRDFResponse(returnType)

    fun getEventById(id: String, returnType: Lang, withRecords: Boolean): String? =
        turtleService.getEvent(id, withRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE).createRDFResponse(returnType)
            }

    fun getCatalogs(returnType: Lang, withRecords: Boolean): String =
        turtleService.getCatalogUnion(withRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE).createRDFResponse(returnType)
            }
            ?: ModelFactory.createDefaultModel().createRDFResponse(returnType)

    fun getCatalogById(id: String, returnType: Lang, withRecords: Boolean): String? =
        turtleService.getCatalog(id, withRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE).createRDFResponse(returnType)
            }

    fun removeEvent(id: String) {
        val start = formatNowWithOsloTimeZone()
        val meta = eventMetaRepository.findAllByFdkId(id)
        if (meta.isEmpty()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "No event found with fdkID $id")
        } else if (meta.none { !it.removed }) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Event with fdkID $id has already been removed")
        } else {
            eventMetaRepository.saveAll(meta.map { it.copy(removed = true) })

            val uri = meta.first().uri
            rabbitPublisher.send(listOf(
                HarvestReport(
                    id = "manual-delete-$id",
                    url = uri,
                    harvestError = false,
                    startTime = start,
                    endTime = formatNowWithOsloTimeZone(),
                    removedResources = listOf(FdkIdAndUri(fdkId = id, uri = uri))
                )
            ))
        }
    }

}
