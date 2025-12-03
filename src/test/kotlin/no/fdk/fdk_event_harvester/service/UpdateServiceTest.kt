package no.fdk.fdk_event_harvester.service

import no.fdk.fdk_event_harvester.configuration.ApplicationProperties
import no.fdk.fdk_event_harvester.repository.CatalogMetaRepository
import no.fdk.fdk_event_harvester.repository.EventMetaRepository
import no.fdk.fdk_event_harvester.utils.*
import org.apache.jena.rdf.model.Model
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

@Tag("unit")
class UpdateServiceTest {
    private val metaRepository: EventMetaRepository = mock()
    private val catalogMetaRepository: CatalogMetaRepository = mock()
    private val valuesMock: ApplicationProperties = mock()
    private val turtleService: TurtleService = mock()
    private val updateService = UpdateService(valuesMock, metaRepository, catalogMetaRepository, turtleService)

    private val responseReader = TestResponseReader()

    @Nested
    internal inner class UpdateMetaData {

        @Test
        fun catalogRecordsIsRecreatedFromMetaDBO() {
            whenever(catalogMetaRepository.findAll())
                .thenReturn(listOf(CATALOG_META_0))
            whenever(turtleService.getCatalog(CATALOG_ID_0, false))
                .thenReturn(responseReader.readFile("no_records_catalog_0.ttl"))
            whenever(metaRepository.findAllByIsPartOf("http://localhost:5050/events/catalogs/$CATALOG_ID_0"))
                .thenReturn(listOf(EVENT_META_0, EVENT_META_1))
            whenever(turtleService.getEvent(EVENT_ID_0, false))
                .thenReturn(responseReader.readFile("no_records_event_0.ttl"))
            whenever(turtleService.getEvent(EVENT_ID_1, false))
                .thenReturn(responseReader.readFile("no_records_event_1.ttl"))
            whenever(valuesMock.eventsUri)
                .thenReturn("http://localhost:5050/events")

            updateService.updateMetaData()

            val expectedInfoModel0 = responseReader.parseFile("event_0.ttl", "TURTLE")
            val expectedInfoModel1 = responseReader.parseFile("event_1.ttl", "TURTLE")

            argumentCaptor<Model, String, Boolean>().apply {
                verify(turtleService, times(2)).saveAsEvent(first.capture(), second.capture(), third.capture())
                assertTrue(checkIfIsomorphicAndPrintDiff(first.firstValue, expectedInfoModel0, "diffInMetaDataUpdatesTurtle-event0"))
                assertTrue(checkIfIsomorphicAndPrintDiff(first.secondValue, expectedInfoModel1, "diffInMetaDataUpdatesTurtle-event1"))
                assertEquals(listOf(EVENT_ID_0, EVENT_ID_1), second.allValues)
                assertEquals(listOf(true, true), third.allValues)
            }
        }

    }
}