package no.fdk.fdk_event_harvester.harvester

import no.fdk.fdk_event_harvester.Application
import no.fdk.fdk_event_harvester.model.Organization
import no.fdk.fdk_event_harvester.rdf.CPSV
import no.fdk.fdk_event_harvester.rdf.CPSVNO
import no.fdk.fdk_event_harvester.rdf.CV
import no.fdk.fdk_event_harvester.rdf.DCATNO
import no.fdk.fdk_event_harvester.rdf.containsTriple
import no.fdk.fdk_event_harvester.rdf.createIdFromString
import no.fdk.fdk_event_harvester.rdf.createRDFResponse
import no.fdk.fdk_event_harvester.rdf.safeParseRDF
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.rdf.model.ResourceRequiredException
import org.apache.jena.rdf.model.Statement
import org.apache.jena.riot.Lang
import org.apache.jena.util.ResourceUtils
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS
import org.slf4j.LoggerFactory
import java.util.Calendar

private val LOGGER = LoggerFactory.getLogger(Application::class.java)

fun CatalogRDFModel.harvestDiff(dbTurtle: String?): Boolean =
    if (dbTurtle == null) true
    else !harvested.isIsomorphicWith(safeParseRDF(dbTurtle, Lang.TURTLE))

fun EventRDFModel.harvestDiff(dboNoRecords: String?): Boolean =
    if (dboNoRecords == null) true
    else !harvested.isIsomorphicWith(safeParseRDF(dboNoRecords, Lang.TURTLE))

fun splitCatalogsFromRDF(harvested: Model, allEvents: List<EventRDFModel>, sourceURL: String, organization: Organization?): List<CatalogRDFModel> {
    val harvestedCatalogs = harvested.listResourcesWithProperty(RDF.type, DCAT.Catalog)
        .toList()
        .excludeBlankNodes(sourceURL)
        .map { resource ->
            val catalogEvents: Set<String> = resource.listProperties(DCATNO.containsEvent)
                .toList()
                .filter { it.isResourceProperty() }
                .map { it.resource }
                .excludeBlankNodes(sourceURL)
                .map { it.uri }
                .toSet()

            val catalogModelWithoutEvents = resource.extractCatalogModel()
                .recursiveBlankNodeSkolem(resource.uri)

            val catalogModel = ModelFactory.createDefaultModel()
            allEvents.filter { catalogEvents.contains(it.eventURI) }
                .forEach { catalogModel.add(it.harvested) }

            CatalogRDFModel(
                resourceURI = resource.uri,
                harvestedWithoutEvents = catalogModelWithoutEvents,
                harvested = catalogModel.union(catalogModelWithoutEvents),
                events = catalogEvents
            )
        }

    return harvestedCatalogs.plus(generatedCatalog(
        allEvents.filterNot { it.isMemberOfAnyCatalog },
        sourceURL,
        organization)
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
            add(property).recursiveAddNonEventOrServiceResource(property.resource)
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
    val model = listProperties().toModel()
    model.setNsPrefixes(nsPrefixes)

    listProperties().toList()
        .filter { it.isResourceProperty() }
        .forEach {
            model.recursiveAddNonEventOrServiceResource(it.resource)
        }

    return EventRDFModel(
        eventURI = uri,
        harvested = model.recursiveBlankNodeSkolem(uri),
        isMemberOfAnyCatalog = isMemberOfAnyCatalog()
    )
}

private fun generatedCatalog(
    events: List<EventRDFModel>,
    sourceURL: String,
    organization: Organization?
): CatalogRDFModel {
    val eventURIs = events.map { it.eventURI }.toSet()
    val generatedCatalogURI = "$sourceURL#GeneratedCatalog"
    val catalogModelWithoutEvents = createModelForHarvestSourceCatalog(generatedCatalogURI, eventURIs, organization)

    val catalogModel = ModelFactory.createDefaultModel()
    events.forEach { catalogModel.add(it.harvested) }

    return CatalogRDFModel(
        resourceURI = generatedCatalogURI,
        harvestedWithoutEvents = catalogModelWithoutEvents,
        harvested = catalogModel.union(catalogModelWithoutEvents),
        events = eventURIs
    )
}

private fun createModelForHarvestSourceCatalog(
    catalogURI: String,
    events: Set<String>,
    organization: Organization?
): Model {
    val catalogModel = ModelFactory.createDefaultModel()
    catalogModel.createResource(catalogURI)
        .addProperty(RDF.type, DCAT.Catalog)
        .addPublisherForGeneratedCatalog(organization?.uri)
        .addLabelForGeneratedCatalog(organization)
        .addEventsForGeneratedCatalog(events)

    return catalogModel
}

private fun Resource.addPublisherForGeneratedCatalog(publisherURI: String?): Resource {
    if (publisherURI != null) {
        addProperty(
            DCTerms.publisher,
            ResourceFactory.createResource(publisherURI)
        )
    }

    return this
}

private fun Resource.addLabelForGeneratedCatalog(organization: Organization?): Resource {
    val nb: String? = organization?.prefLabel?.nb ?: organization?.name
    if (!nb.isNullOrBlank()) {
        val label = model.createLiteral("$nb - Hendelsekatalog", "nb")
        addProperty(RDFS.label, label)
    }

    val nn: String? = organization?.prefLabel?.nn ?: organization?.name
    if (!nb.isNullOrBlank()) {
        val label = model.createLiteral("$nn - Hendingskatalog", "nn")
        addProperty(RDFS.label, label)
    }

    val en: String? = organization?.prefLabel?.en ?: organization?.name
    if (!en.isNullOrBlank()) {
        val label = model.createLiteral("$en - Event catalog", "en")
        addProperty(RDFS.label, label)
    }

    return this
}

private fun Resource.addEventsForGeneratedCatalog(services: Set<String>): Resource {
    services.forEach { addProperty(DCATNO.containsEvent, model.createResource(it)) }
    return this
}

private fun Model.recursiveAddNonEventOrServiceResource(resource: Resource): Model {
    if (resourceShouldBeAdded(resource)) {
        add(resource.listProperties())

        resource.listProperties().toList()
            .filter { it.isResourceProperty() }
            .forEach { recursiveAddNonEventOrServiceResource(it.resource) }
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

fun List<EventRDFModel>.containsFreeServices(): Boolean =
    firstOrNull { !it.isMemberOfAnyCatalog } != null

private fun Model.recursiveBlankNodeSkolem(baseURI: String): Model {
    val anonSubjects = listSubjects().toList().filter { it.isAnon }
    return if (anonSubjects.isEmpty()) this
    else {
        anonSubjects
            .filter { it.doesNotContainAnon() }
            .forEach {
                ResourceUtils.renameResource(it, "$baseURI/.well-known/skolem/${it.createSkolemID()}")
            }
        this.recursiveBlankNodeSkolem(baseURI)
    }
}

private fun Resource.doesNotContainAnon(): Boolean =
    listProperties().toList()
        .filter { it.isResourceProperty() }
        .map { it.resource }
        .filter { it.listProperties().toList().size > 0 }
        .none { it.isAnon }

private fun Resource.createSkolemID(): String =
    createIdFromString(
        listProperties().toModel()
            .createRDFResponse(Lang.N3)
            .replace("\\s".toRegex(), "")
            .toCharArray()
            .sorted()
            .toString()
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
