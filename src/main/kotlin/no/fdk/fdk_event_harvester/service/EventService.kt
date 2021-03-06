package no.fdk.fdk_event_harvester.service

import no.fdk.fdk_event_harvester.rdf.createRDFResponse
import no.fdk.fdk_event_harvester.rdf.parseRDFResponse
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.springframework.stereotype.Service

@Service
class EventService(private val turtleService: TurtleService) {

    fun getAll(returnType: Lang, withRecords: Boolean): String =
        turtleService.getUnion(withRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE, null)?.createRDFResponse(returnType)
            }
            ?: ModelFactory.createDefaultModel().createRDFResponse(returnType)

    fun getEventById(id: String, returnType: Lang, withRecords: Boolean): String? =
        turtleService.getEvent(id, withRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE, null)?.createRDFResponse(returnType)
            }

}
