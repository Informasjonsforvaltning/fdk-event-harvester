package no.fdk.fdk_event_harvester.rdf

import no.fdk.fdk_event_harvester.Application
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.util.*

private val logger = LoggerFactory.getLogger(Application::class.java)
const val BACKUP_BASE_URI = "http://example.com/"

fun jenaTypeFromAcceptHeader(accept: String?): Lang? =
    when {
        accept == null -> null
        accept.contains("text/turtle") -> Lang.TURTLE
        accept.contains("application/rdf+xml") -> Lang.RDFXML
        accept.contains("application/rdf+json") -> Lang.RDFJSON
        accept.contains("application/ld+json") -> Lang.JSONLD
        accept.contains("application/n-triples") -> Lang.NTRIPLES
        accept.contains("text/n3") -> Lang.N3
        accept.contains("*/*") -> null
        else -> Lang.RDFNULL
    }

fun parseRDFResponse(responseBody: String, rdfLanguage: Lang, rdfSource: String?): Model? {
    val responseModel = ModelFactory.createDefaultModel()

    try {
        responseModel.read(StringReader(responseBody), BACKUP_BASE_URI, rdfLanguage.name)
    } catch (ex: Exception) {
        logger.error("Parse from $rdfSource has failed: ${ex.message}")
        return null
    }

    return responseModel
}

fun Model.createRDFResponse(responseType: Lang): String =
    ByteArrayOutputStream().use{ out ->
        write(out, responseType.name)
        out.flush()
        out.toString("UTF-8")
    }

fun createIdFromUri(uri: String): String =
    UUID.nameUUIDFromBytes(uri.toByteArray())
        .toString()
