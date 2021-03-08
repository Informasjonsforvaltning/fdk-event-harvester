package no.fdk.fdk_event_harvester.service

import no.fdk.fdk_event_harvester.model.TurtleDBO
import no.fdk.fdk_event_harvester.rdf.createRDFResponse
import no.fdk.fdk_event_harvester.repository.TurtleRepository
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.text.Charsets.UTF_8

private const val NO_RECORDS_ID_PREFIX = "no-records-"
const val UNION_ID = "event-union-graph"


@Service
class TurtleService(private val turtleRepository: TurtleRepository) {

    fun saveAsUnion(model: Model, withRecords: Boolean) =
        turtleRepository.save(model.createUnionTurtleDBO(withRecords))

    fun getUnion(withRecords: Boolean): String? =
        turtleRepository.findByIdOrNull(turtleId(UNION_ID, withRecords))
            ?.turtle
            ?.let { ungzip(it) }

    fun saveAsEvent(model: Model, fdkId: String, withRecords: Boolean) =
        turtleRepository.save(model.createEventTurtleDBO(fdkId, withRecords))

    fun getEvent(fdkId: String, withRecords: Boolean): String? =
        turtleRepository.findByIdOrNull(turtleId(fdkId, withRecords))
            ?.turtle
            ?.let { ungzip(it) }

    fun saveAsHarvestSource(model: Model, uri: String) =
        turtleRepository.save(model.createHarvestSourceTurtleDBO(uri))

    fun getHarvestSource(uri: String): String? =
        turtleRepository.findByIdOrNull(uri)
            ?.turtle
            ?.let { ungzip(it) }

}

private fun Model.createUnionTurtleDBO(withRecords: Boolean): TurtleDBO =
    TurtleDBO(
        id = turtleId(UNION_ID, withRecords),
        turtle = gzip(createRDFResponse(Lang.TURTLE))
    )

private fun Model.createEventTurtleDBO(fdkId: String, withRecords: Boolean): TurtleDBO =
    TurtleDBO(
        id = turtleId(fdkId, withRecords),
        turtle = gzip(createRDFResponse(Lang.TURTLE))
    )

fun turtleId(fdkId: String, withRecords: Boolean): String =
    "${if (withRecords) "" else NO_RECORDS_ID_PREFIX}$fdkId"

private fun Model.createHarvestSourceTurtleDBO(uri: String): TurtleDBO =
    TurtleDBO(
        id = uri,
        turtle = gzip(createRDFResponse(Lang.TURTLE))
    )

fun gzip(content: String): String {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).bufferedWriter(UTF_8).use { it.write(content) }
    return Base64.getEncoder().encodeToString(bos.toByteArray())
}

fun ungzip(base64Content: String): String {
    val content = Base64.getDecoder().decode(base64Content)
    return GZIPInputStream(content.inputStream())
        .bufferedReader(UTF_8)
        .use { it.readText() }
}
