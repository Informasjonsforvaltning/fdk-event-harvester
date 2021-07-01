package no.fdk.fdk_event_harvester.harvester

import no.fdk.fdk_event_harvester.rdf.CPSV
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
import java.util.*

fun EventRDFModel.harvestDiff(dboNoRecords: String?): Boolean =
    if (dboNoRecords == null) true
    else !harvested.isIsomorphicWith(parseRDFResponse(dboNoRecords, Lang.TURTLE, null))

fun splitEventsFromRDF(harvested: Model): List<EventRDFModel> {
    val businessEvents = harvested.listResourcesWithProperty(RDF.type, CV.BusinessEvent)
        .toList()
        .map { it.extractEventModel(harvested.nsPrefixMap) }

    val lifeEvents = harvested.listResourcesWithProperty(RDF.type, CV.LifeEvent)
        .toList()
        .map { it.extractEventModel(harvested.nsPrefixMap) }

    return listOf(businessEvents, lifeEvents).flatten()
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
        types.contains(CV.BusinessEvent) -> false
        types.contains(CV.LifeEvent) -> false
        containsTriple("<${resource.uri}>", "a", "?o") -> false
        else -> true
    }
}

private fun Model.containsTriple(subj: String, pred: String, obj: String): Boolean {
    val askQuery = "ASK { $subj $pred $obj }"

    val query = QueryFactory.create(askQuery)
    return QueryExecutionFactory.create(query, this).execAsk()
}

class HarvestException(url: String) : Exception("Harvest failed for $url")
