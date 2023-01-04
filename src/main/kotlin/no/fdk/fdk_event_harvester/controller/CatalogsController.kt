package no.fdk.fdk_event_harvester.controller

import no.fdk.fdk_event_harvester.rdf.jenaTypeFromAcceptHeader
import no.fdk.fdk_event_harvester.service.EventService
import org.apache.jena.riot.Lang
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
@Controller
@CrossOrigin
@RequestMapping(
    value = ["/events/catalogs"],
    produces = ["text/turtle", "text/n3", "application/rdf+json", "application/ld+json", "application/rdf+xml",
        "application/n-triples", "application/n-quads", "application/trig", "application/trix"]
)
open class CatalogsController(private val eventService: EventService) {

    @GetMapping("/{id}")
    fun getCatalogById(
        @RequestHeader(HttpHeaders.ACCEPT) accept: String?,
        @PathVariable id: String,
        @RequestParam(value = "catalogrecords", required = false) catalogRecords: Boolean = false
    ): ResponseEntity<String> {
        val returnType = jenaTypeFromAcceptHeader(accept)

        return if (returnType == Lang.RDFNULL) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else {
            eventService.getCatalogById(id, returnType ?: Lang.TURTLE, catalogRecords)
                ?.let { ResponseEntity(it, HttpStatus.OK) }
                ?: ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping
    fun getCatalogs(
        @RequestHeader(HttpHeaders.ACCEPT) accept: String?,
        @RequestParam(value = "catalogrecords", required = false) catalogRecords: Boolean = false
    ): ResponseEntity<String> {
        val returnType = jenaTypeFromAcceptHeader(accept)

        return if (returnType == Lang.RDFNULL) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else ResponseEntity(eventService.getCatalogs(returnType ?: Lang.TURTLE, catalogRecords), HttpStatus.OK)
    }

}
