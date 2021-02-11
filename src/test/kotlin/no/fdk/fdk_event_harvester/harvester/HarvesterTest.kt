package no.fdk.fdk_event_harvester.harvester

import com.nhaarman.mockitokotlin2.*
import no.fdk.fdk_event_harvester.adapter.EventAdapter
import no.fdk.fdk_event_harvester.configuration.ApplicationProperties
import no.fdk.fdk_event_harvester.model.EventMeta
import no.fdk.fdk_event_harvester.repository.EventMetaRepository
import no.fdk.fdk_event_harvester.service.TurtleService
import no.fdk.fdk_event_harvester.utils.*
import org.apache.jena.rdf.model.Model
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.*

@Tag("unit")
class HarvesterTest {
    private val metaRepository: EventMetaRepository = mock()
    private val turtleService: TurtleService = mock()
    private val valuesMock: ApplicationProperties = mock()
    private val adapter: EventAdapter = mock()

    private val harvester = EventHarvester(adapter, metaRepository, turtleService, valuesMock)
    private val responseReader = TestResponseReader()

    @Test
    fun harvestDataSourceSavedWhenDBIsEmpty() {
        whenever(adapter.getEvents(TEST_HARVEST_SOURCE))
            .thenReturn(responseReader.readFile("harvest_response_0.ttl"))
        whenever(valuesMock.eventsUri)
            .thenReturn("http://localhost:5000/events")

        harvester.harvestEvents(TEST_HARVEST_SOURCE, TEST_HARVEST_DATE)

        argumentCaptor<Model, String>().apply {
            verify(turtleService, times(1)).saveAsHarvestSource(first.capture(), second.capture())
            assertTrue(first.firstValue.isIsomorphicWith(responseReader.parseFile("harvest_response_0.ttl", "TURTLE")))
            assertEquals(TEST_HARVEST_SOURCE.url, second.firstValue)
        }

        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(4)).saveAsEvent(first.capture(), second.capture(), third.capture())
            if (second.firstValue == EVENT_ID_0) {
                assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[0], responseReader.parseFile("no_records_event_0.ttl", "TURTLE"), "harvestDataSourceSavedWhenDBIsEmpty-norecords0"))
                assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[1], responseReader.parseFile("event_0.ttl", "TURTLE"), "harvestDataSourceSavedWhenDBIsEmpty-0"))
                assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[2], responseReader.parseFile("no_records_event_1.ttl", "TURTLE"), "harvestDataSourceSavedWhenDBIsEmpty-norecords1"))
                assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[3], responseReader.parseFile("event_1.ttl", "TURTLE"), "harvestDataSourceSavedWhenDBIsEmpty-1"))
                assertEquals(listOf(EVENT_ID_0, EVENT_ID_0, EVENT_ID_1, EVENT_ID_1), second.allValues)
            } else {
                assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[0], responseReader.parseFile("no_records_event_1.ttl", "TURTLE"), "harvestDataSourceSavedWhenDBIsEmpty-norecords1"))
                assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[1], responseReader.parseFile("event_1.ttl", "TURTLE"), "harvestDataSourceSavedWhenDBIsEmpty-1"))
                assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[2], responseReader.parseFile("no_records_event_0.ttl", "TURTLE"), "harvestDataSourceSavedWhenDBIsEmpty-norecords0"))
                assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[3], responseReader.parseFile("event_0.ttl", "TURTLE"), "harvestDataSourceSavedWhenDBIsEmpty-0"))
                assertEquals(listOf(EVENT_ID_1, EVENT_ID_1, EVENT_ID_0, EVENT_ID_0), second.allValues)
            }
            assertEquals(listOf(false, true, false, true), third.allValues)
        }

        argumentCaptor<EventMeta>().apply {
            verify(metaRepository, times(2)).save(capture())
            assertEquals(listOf(EVENT_META_0, EVENT_META_1), allValues.sortedBy { it.uri })
        }
    }

    @Test
    fun harvestDataSourceNotPersistedWhenNoChangesFromDB() {
        val harvested = responseReader.readFile("harvest_response_0.ttl")
        whenever(adapter.getEvents(TEST_HARVEST_SOURCE))
            .thenReturn(harvested)
        whenever(valuesMock.eventsUri)
            .thenReturn("http://localhost:5000/public-services")
        whenever(turtleService.getHarvestSource(TEST_HARVEST_SOURCE.url!!))
            .thenReturn(harvested)

        harvester.harvestEvents(TEST_HARVEST_SOURCE, TEST_HARVEST_DATE)

        argumentCaptor<Model, String>().apply {
            verify(turtleService, times(0)).saveAsHarvestSource(first.capture(), second.capture())
        }

        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(0)).saveAsEvent(first.capture(), second.capture(), third.capture())
        }

        argumentCaptor<EventMeta>().apply {
            verify(metaRepository, times(0)).save(capture())
        }
    }

    @Test
    fun onlyRelevantUpdatedWhenHarvestedFromDB() {
        whenever(adapter.getEvents(TEST_HARVEST_SOURCE))
            .thenReturn(responseReader.readFile("harvest_response_0.ttl"))
        whenever(valuesMock.eventsUri)
            .thenReturn("http://localhost:5000/events")
        whenever(turtleService.getHarvestSource(TEST_HARVEST_SOURCE.url!!))
            .thenReturn(responseReader.readFile("harvest_response_0_diff.ttl"))
        whenever(metaRepository.findById(EVENT_META_0.uri))
            .thenReturn(Optional.of(EVENT_META_0))
        whenever(metaRepository.findById(EVENT_META_1.uri))
            .thenReturn(Optional.of(EVENT_META_1))
        whenever(turtleService.getEvent(EVENT_ID_0, false))
            .thenReturn(responseReader.readFile("no_records_event_0.ttl"))
        whenever(turtleService.getEvent(EVENT_ID_1, false))
            .thenReturn(responseReader.readFile("no_records_event_1_diff.ttl"))

        harvester.harvestEvents(TEST_HARVEST_SOURCE, NEW_TEST_HARVEST_DATE)

        argumentCaptor<Model, String>().apply {
            verify(turtleService, times(1)).saveAsHarvestSource(first.capture(), second.capture())
            assertTrue(first.firstValue.isIsomorphicWith(responseReader.parseFile("harvest_response_0.ttl", "TURTLE")))
            assertEquals(TEST_HARVEST_SOURCE.url, second.firstValue)
        }

        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(2)).saveAsEvent(first.capture(), second.capture(), third.capture())
            assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[0], responseReader.parseFile("no_records_event_1.ttl", "TURTLE"), "onlyRelevantUpdatedWhenHarvestedFromDB-norecords1"))
            assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[1], responseReader.parseFile("event_1_modified.ttl", "TURTLE"), "onlyRelevantUpdatedWhenHarvestedFromDB-1"))
            assertEquals(listOf(EVENT_ID_1, EVENT_ID_1), second.allValues)
            assertEquals(listOf(false, true), third.allValues)
        }

        argumentCaptor<EventMeta>().apply {
            verify(metaRepository, times(1)).save(capture())
            assertEquals(EVENT_META_1.copy(modified = NEW_TEST_HARVEST_DATE.timeInMillis), firstValue)
        }

    }

    @Test
    fun harvestWithErrorsIsNotPersisted() {
        whenever(adapter.getEvents(TEST_HARVEST_SOURCE))
            .thenReturn(responseReader.readFile("harvest_error_response.ttl"))
        whenever(valuesMock.eventsUri)
            .thenReturn("http://localhost:5000/public-services")

        harvester.harvestEvents(TEST_HARVEST_SOURCE, TEST_HARVEST_DATE)

        argumentCaptor<Model, String>().apply {
            verify(turtleService, times(0)).saveAsHarvestSource(first.capture(), second.capture())
        }

        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(0)).saveAsEvent(first.capture(), second.capture(), third.capture())
        }

        argumentCaptor<EventMeta>().apply {
            verify(metaRepository, times(0)).save(capture())
        }
    }

}