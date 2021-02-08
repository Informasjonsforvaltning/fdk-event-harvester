package no.fdk.fdk_event_harvester.controller

import no.fdk.fdk_event_harvester.rdf.JenaType
import no.fdk.fdk_event_harvester.rdf.jenaTypeFromAcceptHeader
import no.fdk.fdk_event_harvester.service.EventService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

private val LOGGER = LoggerFactory.getLogger(EventsController::class.java)

@Controller
@CrossOrigin
@RequestMapping(value = ["/events"], produces = ["text/turtle", "text/n3", "application/rdf+json", "application/ld+json", "application/rdf+xml", "application/n-triples"])
open class EventsController(private val eventService: EventService) {

    @GetMapping(value = ["/{id}"])
    fun getEventById(httpServletRequest: HttpServletRequest, @PathVariable id: String): ResponseEntity<String> {
        LOGGER.info("get Event with id $id")
        val returnType = jenaTypeFromAcceptHeader(httpServletRequest.getHeader("Accept"))

        return if (returnType == JenaType.NOT_JENA) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else {
            eventService.getEventById(id, returnType ?: JenaType.TURTLE)
                ?.let { ResponseEntity(it, HttpStatus.OK) }
                ?: ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping()
    fun getCatalogs(httpServletRequest: HttpServletRequest): ResponseEntity<String> {
        LOGGER.info("get all events")
        val returnType = jenaTypeFromAcceptHeader(httpServletRequest.getHeader("Accept"))

        return if (returnType == JenaType.NOT_JENA) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else ResponseEntity(eventService.getAll(returnType ?: JenaType.TURTLE), HttpStatus.OK)
    }

}
