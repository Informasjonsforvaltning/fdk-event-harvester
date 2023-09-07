package no.fdk.fdk_event_harvester.service

import no.fdk.fdk_event_harvester.configuration.ApplicationProperties
import no.fdk.fdk_event_harvester.harvester.calendarFromTimestamp
import no.fdk.fdk_event_harvester.model.*
import no.fdk.fdk_event_harvester.rdf.DCATNO
import no.fdk.fdk_event_harvester.rdf.containsTriple
import no.fdk.fdk_event_harvester.rdf.safeParseRDF
import no.fdk.fdk_event_harvester.rdf.safeAddProperty
import no.fdk.fdk_event_harvester.repository.*
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.springframework.stereotype.Service

@Service
class UpdateService(
    private val applicationProperties: ApplicationProperties,
    private val eventMetaRepository: EventMetaRepository,
    private val catalogMetaRepository: CatalogMetaRepository,
    private val turtleService: TurtleService
) {

    fun updateUnionModels() {
        val eventUnion = ModelFactory.createDefaultModel()
        val eventUnionNoRecords = ModelFactory.createDefaultModel()

        eventMetaRepository.findAll()
            .forEach {
                turtleService.getEvent(it.fdkId, withRecords = true)
                    ?.let { dboTurtle -> safeParseRDF(dboTurtle, Lang.TURTLE) }
                    ?.run { eventUnion.add(this) }

                turtleService.getEvent(it.fdkId, withRecords = false)
                    ?.let { dboTurtle -> safeParseRDF(dboTurtle, Lang.TURTLE) }
                    ?.run { eventUnionNoRecords.add(this) }
            }

        turtleService.saveAsEventUnion(eventUnion, true)
        turtleService.saveAsEventUnion(eventUnionNoRecords, false)

        val catalogUnion = ModelFactory.createDefaultModel()
        val catalogUnionNoRecords = ModelFactory.createDefaultModel()

        catalogMetaRepository.findAll()
            .filter { it.events.isNotEmpty() }
            .forEach {
                turtleService.getCatalog(it.fdkId, withRecords = true)
                    ?.let { turtle -> safeParseRDF(turtle, Lang.TURTLE) }
                    ?.run { catalogUnion.add(this) }

                turtleService.getCatalog(it.fdkId, withRecords = false)
                    ?.let { turtle -> safeParseRDF(turtle, Lang.TURTLE) }
                    ?.run { catalogUnionNoRecords.add(this) }
            }

        turtleService.saveAsCatalogUnion(catalogUnion, true)
        turtleService.saveAsCatalogUnion(catalogUnionNoRecords, false)
    }

    fun updateMetaData() {
        eventMetaRepository.findAll()
            .forEach { event ->
                val eventMeta = event.createMetaModel()

                turtleService.getEvent(event.fdkId, withRecords = false)
                    ?.let { eventNoRecords -> safeParseRDF(eventNoRecords, Lang.TURTLE) }
                    ?.let { eventModelNoRecords -> eventMeta.union(eventModelNoRecords) }
                    ?.run { turtleService.saveAsEvent(this, fdkId = event.fdkId, withRecords = true) }
            }

        catalogMetaRepository.findAll()
            .forEach { catalog ->
                val catalogNoRecords = turtleService.getCatalog(catalog.fdkId, withRecords = false)
                    ?.let { safeParseRDF(it, Lang.TURTLE) }

                if (catalogNoRecords != null) {
                    val fdkCatalogURI = "${applicationProperties.eventsUri}/catalogs/${catalog.fdkId}"
                    val catalogMeta = catalog.createMetaModel()

                    eventMetaRepository.findAllByIsPartOf(fdkCatalogURI)
                        .filter { it.catalogContainsEvent(catalog.uri, catalogNoRecords) }
                        .forEach { event -> catalogMeta.add(event.createMetaModel()) }

                    turtleService.saveAsCatalog(
                        catalogMeta.union(catalogNoRecords),
                        fdkId = catalog.fdkId,
                        withRecords = true
                    )
                }
            }

        updateUnionModels()
    }

    private fun CatalogMeta.createMetaModel(): Model {
        val fdkUri = "${applicationProperties.eventsUri}/catalogs/$fdkId"

        val metaModel = ModelFactory.createDefaultModel()

        metaModel.createResource(fdkUri)
            .addProperty(RDF.type, DCAT.CatalogRecord)
            .addProperty(DCTerms.identifier, fdkId)
            .addProperty(FOAF.primaryTopic, metaModel.createResource(uri))
            .addProperty(DCTerms.issued, metaModel.createTypedLiteral(calendarFromTimestamp(issued)))
            .addProperty(DCTerms.modified, metaModel.createTypedLiteral(calendarFromTimestamp(modified)))

        return metaModel
    }

    private fun EventMeta.createMetaModel(): Model {
        val fdkUri = "${applicationProperties.eventsUri}/$fdkId"

        val metaModel = ModelFactory.createDefaultModel()

        metaModel.createResource(fdkUri)
            .addProperty(RDF.type, DCAT.CatalogRecord)
            .addProperty(DCTerms.identifier, fdkId)
            .addProperty(FOAF.primaryTopic, metaModel.createResource(uri))
            .safeAddProperty(DCTerms.isPartOf, isPartOf)
            .addProperty(DCTerms.issued, metaModel.createTypedLiteral(calendarFromTimestamp(issued)))
            .addProperty(DCTerms.modified, metaModel.createTypedLiteral(calendarFromTimestamp(modified)))

        return metaModel
    }

    private fun EventMeta.catalogContainsEvent(catalogURI: String, catalogModel: Model): Boolean =
        catalogModel.containsTriple("<$catalogURI>", "<${DCATNO.containsEvent.uri}>", "<$uri>")

}
