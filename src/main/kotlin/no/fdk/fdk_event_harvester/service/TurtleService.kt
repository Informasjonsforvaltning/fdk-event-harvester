package no.fdk.fdk_event_harvester.service

import no.fdk.fdk_event_harvester.model.CatalogTurtle
import no.fdk.fdk_event_harvester.model.EventTurtle
import no.fdk.fdk_event_harvester.model.FDKCatalogTurtle
import no.fdk.fdk_event_harvester.model.FDKEventTurtle
import no.fdk.fdk_event_harvester.model.HarvestSourceTurtle
import no.fdk.fdk_event_harvester.rdf.createRDFResponse
import no.fdk.fdk_event_harvester.repository.CatalogTurtleRepository
import no.fdk.fdk_event_harvester.repository.EventTurtleRepository
import no.fdk.fdk_event_harvester.repository.FDKCatalogTurtleRepository
import no.fdk.fdk_event_harvester.repository.FDKEventTurtleRepository
import no.fdk.fdk_event_harvester.repository.HarvestSourceTurtleRepository
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.text.Charsets.UTF_8

@Service
class TurtleService(
    private val eventRepository: EventTurtleRepository,
    private val fdkEventRepository: FDKEventTurtleRepository,
    private val catalogRepository: CatalogTurtleRepository,
    private val fdkCatalogRepository: FDKCatalogTurtleRepository,
    private val harvestSourceRepository: HarvestSourceTurtleRepository
) {

    fun saveAsEvent(model: Model, fdkId: String, withRecords: Boolean) {
        if (withRecords) fdkEventRepository.save(model.createFDKEventTurtleDBO(fdkId))
        else eventRepository.save(model.createEventTurtleDBO(fdkId))
    }

    fun getEvent(fdkId: String, withRecords: Boolean): String? =
        if (withRecords) fdkEventRepository.findByIdOrNull(fdkId)
            ?.turtle
            ?.let { ungzip(it) }
        else eventRepository.findByIdOrNull(fdkId)
            ?.turtle
            ?.let { ungzip(it) }

    fun saveAsCatalog(model: Model, fdkId: String, withRecords: Boolean) {
        if (withRecords) {
            fdkCatalogRepository.save(model.createFDKCatalogTurtleDBO(fdkId))
        }
        else {
            catalogRepository.save(model.createCatalogTurtleDBO(fdkId))
        }
    }

    fun getCatalog(fdkId: String, withRecords: Boolean): String? =
        if (withRecords) fdkCatalogRepository.findByIdOrNull(fdkId)
            ?.turtle
            ?.let { ungzip(it) }
        else catalogRepository.findByIdOrNull(fdkId)
            ?.turtle
            ?.let { ungzip(it) }

    fun saveAsHarvestSource(model: Model, uri: String) {
        harvestSourceRepository.save(model.createHarvestSourceTurtleDBO(uri))
    }

    fun getHarvestSource(uri: String): String? =
        harvestSourceRepository.findByIdOrNull(uri)
            ?.turtle
            ?.let { ungzip(it) }

    fun deleteEventFiles(fdkId: String) {
        eventRepository.deleteById(fdkId)
        fdkEventRepository.deleteById(fdkId)
    }

}

private fun Model.createEventTurtleDBO(id: String): EventTurtle =
    EventTurtle(
        id = id,
        turtle = gzip(createRDFResponse(Lang.TURTLE))
    )

private fun Model.createFDKEventTurtleDBO(id: String): FDKEventTurtle =
    FDKEventTurtle(
        id = id,
        turtle = gzip(createRDFResponse(Lang.TURTLE))
    )

private fun Model.createCatalogTurtleDBO(id: String): CatalogTurtle =
    CatalogTurtle(
        id = id,
        turtle = gzip(createRDFResponse(Lang.TURTLE))
    )

private fun Model.createFDKCatalogTurtleDBO(id: String): FDKCatalogTurtle =
    FDKCatalogTurtle(
        id = id,
        turtle = gzip(createRDFResponse(Lang.TURTLE))
    )

private fun Model.createHarvestSourceTurtleDBO(uri: String): HarvestSourceTurtle =
    HarvestSourceTurtle(
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
