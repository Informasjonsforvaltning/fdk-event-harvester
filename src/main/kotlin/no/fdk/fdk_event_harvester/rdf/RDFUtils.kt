package no.fdk.fdk_event_harvester.rdf

import no.fdk.fdk_event_harvester.Application
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.util.*

private val logger = LoggerFactory.getLogger(Application::class.java)
const val BACKUP_BASE_URI = "http://example.com/"

enum class JenaType(val value: String){
    TURTLE("TURTLE"),
    RDF_XML("RDF/XML"),
    RDF_JSON("RDF/JSON"),
    JSON_LD("JSON-LD"),
    NTRIPLES("N-TRIPLES"),
    N3("N3"),
    NOT_JENA("NOT-JENA")
}

fun jenaTypeFromAcceptHeader(accept: String?): JenaType? =
    when {
        accept == null -> null
        accept.contains("text/turtle") -> JenaType.TURTLE
        accept.contains("application/rdf+xml") -> JenaType.RDF_XML
        accept.contains("application/rdf+json") -> JenaType.RDF_JSON
        accept.contains("application/ld+json") -> JenaType.JSON_LD
        accept.contains("application/n-triples") -> JenaType.NTRIPLES
        accept.contains("text/n3") -> JenaType.N3
        accept.contains("*/*") -> null
        else -> JenaType.NOT_JENA
    }

fun parseRDFResponse(responseBody: String, rdfLanguage: JenaType, rdfSource: String?): Model? {
    val responseModel = ModelFactory.createDefaultModel()

    try {
        responseModel.read(StringReader(responseBody), BACKUP_BASE_URI, rdfLanguage.value)
    } catch (ex: Exception) {
        logger.error("Parse from $rdfSource has failed: ${ex.message}")
        return null
    }

    return responseModel
}

fun Model.createRDFResponse(responseType: JenaType): String =
    ByteArrayOutputStream().use{ out ->
        write(out, responseType.value)
        out.flush()
        out.toString("UTF-8")
    }

fun createIdFromUri(uri: String): String =
    UUID.nameUUIDFromBytes(uri.toByteArray())
        .toString()
