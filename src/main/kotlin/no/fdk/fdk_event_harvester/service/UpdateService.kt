package no.fdk.fdk_event_harvester.service

import no.fdk.fdk_event_harvester.configuration.ApplicationProperties
import no.fdk.fdk_event_harvester.harvester.calendarFromTimestamp
import no.fdk.fdk_event_harvester.harvester.extractCatalogModel
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

    fun updateMetaData() {
        catalogMetaRepository.findAll()
            .forEach { catalog ->
                val catalogNoRecords = turtleService.getCatalog(catalog.fdkId, withRecords = false)
                    ?.let { safeParseRDF(it, Lang.TURTLE) }

                if (catalogNoRecords != null) {
                    val fdkCatalogURI = "${applicationProperties.eventsUri}/catalogs/${catalog.fdkId}"
                    val catalogMeta = catalog.createMetaModel()
                    val completeMetaModel = ModelFactory.createDefaultModel()
                    completeMetaModel.add(catalogMeta)

                    val catalogTriples = catalogNoRecords.getResource(catalog.uri)
                        .extractCatalogModel()
                    catalogTriples.add(catalogMeta)

                    eventMetaRepository.findAllByIsPartOf(fdkCatalogURI)
                        .filter { it.catalogContainsEvent(catalog.uri, catalogNoRecords) }
                        .forEach { event ->
                            val eventMeta = event.createMetaModel()
                            completeMetaModel.add(eventMeta)

                            turtleService.getEvent(event.fdkId, withRecords = false)
                                ?.let { eventNoRecords -> safeParseRDF(eventNoRecords, Lang.TURTLE) }
                                ?.let { eventModelNoRecords -> eventMeta.union(eventModelNoRecords) }
                                ?.let { eventModel -> catalogTriples.union(eventModel) }
                                ?.run { turtleService.saveAsEvent(this, fdkId = event.fdkId, withRecords = true) }
                        }

                    turtleService.saveAsCatalog(
                        completeMetaModel.union(catalogNoRecords),
                        fdkId = catalog.fdkId,
                        withRecords = true
                    )
                }
            }
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
