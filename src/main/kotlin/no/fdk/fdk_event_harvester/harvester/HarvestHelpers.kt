package no.fdk.fdk_event_harvester.harvester

import no.fdk.fdk_event_harvester.Application
import no.fdk.fdk_event_harvester.rdf.CPSV
import no.fdk.fdk_event_harvester.rdf.CPSVNO
import no.fdk.fdk_event_harvester.rdf.CV
import no.fdk.fdk_event_harvester.rdf.parseRDFResponse
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceRequiredException
import org.apache.jena.rdf.model.Statement
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.RDF
import org.slf4j.LoggerFactory
import java.util.*

private val LOGGER = LoggerFactory.getLogger(Application::class.java)

fun EventRDFModel.harvestDiff(dboNoRecords: String?): Boolean =
    if (dboNoRecords == null) true
    else !harvested.isIsomorphicWith(parseRDFResponse(dboNoRecords, Lang.TURTLE, null))

fun splitEventsFromRDF(harvested: Model, sourceURL: String): List<EventRDFModel> {
    val events = harvested.listResourcesWithProperty(RDF.type, CV.Event)
        .toList()
        .filterBlankNodeEvents(sourceURL)
        .map { it.extractEventModel(harvested.nsPrefixMap) }

    val businessEvents = harvested.listResourcesWithProperty(RDF.type, CV.BusinessEvent)
        .toList()
        .filterBlankNodeEvents(sourceURL)
        .map { it.extractEventModel(harvested.nsPrefixMap) }

    val lifeEvents = harvested.listResourcesWithProperty(RDF.type, CV.LifeEvent)
        .toList()
        .filterBlankNodeEvents(sourceURL)
        .map { it.extractEventModel(harvested.nsPrefixMap) }

    return listOf(events, businessEvents, lifeEvents).flatten()
}

private fun List<Resource>.filterBlankNodeEvents(sourceURL: String): List<Resource> =
    filter {
        if (it.isURIResource) true
        else {
            LOGGER.error(
                "Failed harvest of event for $sourceURL, unable to harvest blank node events",
                Exception("unable to harvest blank node events")
            )
            false
        }
    }

private fun Resource.extractEventModel(nsPrefixes: Map<String, String>): EventRDFModel {
    var model = listProperties().toModel()
    model.setNsPrefixes(nsPrefixes)

    listProperties().toList()
        .filter { it.isResourceProperty() }
        .forEach {
            model = model.recursiveAddNonEventOrServiceResource(it.resource, 10)
        }

    return EventRDFModel(
        eventURI = uri,
        harvested = model
    )
}

private fun Model.recursiveAddNonEventOrServiceResource(resource: Resource, recursiveCount: Int): Model {
    val newCount = recursiveCount - 1

    if (resourceShouldBeAdded(resource)) {
        add(resource.listProperties())

        if (newCount > 0) {
            resource.listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach { recursiveAddNonEventOrServiceResource(it.resource, newCount) }
        }
    }

    return this
}

fun Statement.isResourceProperty(): Boolean =
    try {
        resource.isResource
    } catch (ex: ResourceRequiredException) {
        false
    }

fun calendarFromTimestamp(timestamp: Long): Calendar {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    return calendar
}

data class EventRDFModel (
    val eventURI: String,
    val harvested: Model
)

private fun Model.resourceShouldBeAdded(resource: Resource): Boolean {
    val types = resource.listProperties(RDF.type)
        .toList()
        .map { it.`object` }

    return when {
        types.contains(CPSV.PublicService) -> false
        types.contains(CPSVNO.Service) -> false
        types.contains(CV.Event) -> false
        types.contains(CV.BusinessEvent) -> false
        types.contains(CV.LifeEvent) -> false
        !resource.isURIResource -> true
        containsTriple("<${resource.uri}>", "a", "?o") -> false
        else -> true
    }
}

private fun Model.containsTriple(subj: String, pred: String, obj: String): Boolean {
    val askQuery = "ASK { $subj $pred $obj }"

    return try {
        val query = QueryFactory.create(askQuery)
        return QueryExecutionFactory.create(query, this).execAsk()
    } catch (ex: Exception) { false }
}

class HarvestException(url: String) : Exception("Harvest failed for $url")
