package no.fdk.fdk_event_harvester.harvester

import no.fdk.fdk_event_harvester.Application
import no.fdk.fdk_event_harvester.rdf.CPSV
import no.fdk.fdk_event_harvester.rdf.CPSVNO
import no.fdk.fdk_event_harvester.rdf.CV
import no.fdk.fdk_event_harvester.rdf.DCATNO
import no.fdk.fdk_event_harvester.rdf.containsTriple
import no.fdk.fdk_event_harvester.rdf.parseRDFResponse
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceRequiredException
import org.apache.jena.rdf.model.Statement
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.RDF
import org.slf4j.LoggerFactory
import java.util.*

private val LOGGER = LoggerFactory.getLogger(Application::class.java)

fun CatalogRDFModel.harvestDiff(dbTurtle: String?): Boolean =
    if (dbTurtle == null) true
    else !harvested.isIsomorphicWith(parseRDFResponse(dbTurtle, Lang.TURTLE, null))

fun EventRDFModel.harvestDiff(dboNoRecords: String?): Boolean =
    if (dboNoRecords == null) true
    else !harvested.isIsomorphicWith(parseRDFResponse(dboNoRecords, Lang.TURTLE, null))

fun splitCatalogsFromRDF(harvested: Model, allEvents: List<EventRDFModel>, sourceURL: String): List<CatalogRDFModel> =
    harvested.listResourcesWithProperty(RDF.type, DCAT.Catalog)
        .toList()
        .excludeBlankNodes(sourceURL)
        .filter { it.hasProperty(DCATNO.containsEvent) }
        .map { resource ->
            val catalogEvents: Set<String> = resource.listProperties(DCATNO.containsEvent)
                .toList()
                .filter { it.isResourceProperty() }
                .map { it.resource }
                .excludeBlankNodes(sourceURL)
                .map { it.uri }
                .toSet()

            val catalogModelWithoutEvents = resource.extractCatalogModel()

            var catalogModel = catalogModelWithoutEvents
            allEvents.filter { catalogEvents.contains(it.eventURI) }
                .forEach { catalogModel = catalogModel.union(it.harvested) }

            CatalogRDFModel(
                resourceURI = resource.uri,
                harvestedWithoutEvents = catalogModelWithoutEvents,
                harvested = catalogModel,
                events = catalogEvents
            )
        }

fun splitEventsFromRDF(harvested: Model, sourceURL: String): List<EventRDFModel> =
    harvested.listResourcesWithEventType()
        .toList()
        .excludeBlankNodes(sourceURL)
        .map { it.extractEventModel(harvested.nsPrefixMap) }

private fun Model.listResourcesWithEventType(): List<Resource> {
    val events = listResourcesWithProperty(RDF.type, CV.Event)
        .toList()
    val businessEvents = listResourcesWithProperty(RDF.type, CV.BusinessEvent)
        .toList()
    val lifeEvents = listResourcesWithProperty(RDF.type, CV.LifeEvent)
        .toList()
    return listOf(events, businessEvents, lifeEvents).flatten()
}

fun Resource.extractCatalogModel(): Model {
    val catalogModelWithoutServices = ModelFactory.createDefaultModel()
    catalogModelWithoutServices.setNsPrefixes(model.nsPrefixMap)

    listProperties()
        .toList()
        .forEach { catalogModelWithoutServices.addCatalogProperties(it) }

    return catalogModelWithoutServices
}

private fun Model.addCatalogProperties(property: Statement): Model =
    when {
        property.predicate != DCATNO.containsEvent && property.isResourceProperty() ->
            add(property).recursiveAddNonEventOrServiceResource(property.resource, 5)
        property.predicate != DCATNO.containsEvent -> add(property)
        property.isResourceProperty() && property.resource.isURIResource -> add(property)
        else -> this
    }

private fun List<Resource>.excludeBlankNodes(sourceURL: String): List<Resource> =
    filter {
        if (it.isURIResource) true
        else {
            LOGGER.warn("Blank node event or catalog filtered when harvesting $sourceURL")
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
        harvested = model,
        isMemberOfAnyCatalog = isMemberOfAnyCatalog()
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
    val harvested: Model,
    val isMemberOfAnyCatalog: Boolean
)

data class CatalogRDFModel(
    val resourceURI: String,
    val harvested: Model,
    val harvestedWithoutEvents: Model,
    val events: Set<String>,
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

private fun Resource.isMemberOfAnyCatalog(): Boolean {
    val askQuery = """ASK {
        ?catalog a <${DCAT.Catalog.uri}> .
        ?catalog <${DCATNO.containsEvent.uri}> <$uri> .
    }""".trimMargin()

    val query = QueryFactory.create(askQuery)
    return QueryExecutionFactory.create(query, model).execAsk()
}

class HarvestException(url: String) : Exception("Harvest failed for $url")
