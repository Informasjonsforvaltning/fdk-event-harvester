package no.fdk.fdk_event_harvester.harvester

import no.fdk.fdk_event_harvester.adapter.EventAdapter
import no.fdk.fdk_event_harvester.configuration.ApplicationProperties
import no.fdk.fdk_event_harvester.model.EventMeta
import no.fdk.fdk_event_harvester.model.FdkIdAndUri
import no.fdk.fdk_event_harvester.model.HarvestReport
import no.fdk.fdk_event_harvester.repository.EventMetaRepository
import no.fdk.fdk_event_harvester.service.TurtleService
import no.fdk.fdk_event_harvester.utils.*
import org.apache.jena.rdf.model.Model
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
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

        val report = harvester.harvestEvents(TEST_HARVEST_SOURCE, TEST_HARVEST_DATE, false)

        argumentCaptor<Model, String>().apply {
            verify(turtleService, times(1)).saveAsHarvestSource(first.capture(), second.capture())
            assertTrue(first.firstValue.isIsomorphicWith(responseReader.parseFile("harvest_response_0.ttl", "TURTLE")))
            assertEquals(TEST_HARVEST_SOURCE.url, second.firstValue)
        }

        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(6)).saveAsEvent(first.capture(), second.capture(), third.capture())
            val index0 = second.allValues.indexOf(EVENT_ID_0)
            assertEquals(second.allValues[index0 + 1], EVENT_ID_0)
            assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[index0], responseReader.parseFile("no_records_event_0.ttl", "TURTLE"), "harvestDataSourceSavedWhenDBIsEmpty-norecords0"))
            assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[index0 + 1], responseReader.parseFile("event_0.ttl", "TURTLE"), "harvestDataSourceSavedWhenDBIsEmpty-0"))

            val index1 = second.allValues.indexOf(EVENT_ID_1)
            assertEquals(second.allValues[index1 + 1], EVENT_ID_1)
            assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[index1], responseReader.parseFile("no_records_event_1.ttl", "TURTLE"), "harvestDataSourceSavedWhenDBIsEmpty-norecords1"))
            assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[index1 + 1], responseReader.parseFile("event_1.ttl", "TURTLE"), "harvestDataSourceSavedWhenDBIsEmpty-1"))

            val index2 = second.allValues.indexOf(EVENT_ID_2)
            assertEquals(second.allValues[index2 + 1], EVENT_ID_2)
            assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[index2], responseReader.parseFile("no_records_event_2.ttl", "TURTLE"), "harvestDataSourceSavedWhenDBIsEmpty-norecords2"))
            assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[index2 + 1], responseReader.parseFile("event_2.ttl", "TURTLE"), "harvestDataSourceSavedWhenDBIsEmpty-2"))

            assertEquals(listOf(false, true, false, true, false, true), third.allValues)
        }

        argumentCaptor<EventMeta>().apply {
            verify(metaRepository, times(3)).save(capture())
            assertEquals(listOf(EVENT_META_0, EVENT_META_1, EVENT_META_2), allValues.sortedBy { it.uri })
        }

        val expectedReport = HarvestReport(
            id="test-source",
            url="http://localhost:5000/fdk-public-service-publisher.ttl",
            dataType="event",
            harvestError=false,
            startTime = "2020-10-05 15:15:39 +0200",
            endTime = report!!.endTime,
            changedResources = listOf(
                FdkIdAndUri(fdkId= EVENT_ID_2, uri=EVENT_META_2.uri),
                FdkIdAndUri(fdkId= EVENT_ID_0, uri=EVENT_META_0.uri),
                FdkIdAndUri(fdkId= EVENT_ID_1, uri=EVENT_META_1.uri))
        )

        kotlin.test.assertEquals(expectedReport, report)
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

        val report = harvester.harvestEvents(TEST_HARVEST_SOURCE, TEST_HARVEST_DATE, false)

        verify(turtleService, times(0)).saveAsHarvestSource(any(), any())
        verify(turtleService, times(0)).saveAsEvent(any(), any(), any())
        verify(metaRepository, times(0)).save(any())

        val expectedReport = HarvestReport(
            id="test-source",
            url="http://localhost:5000/fdk-public-service-publisher.ttl",
            dataType="event",
            harvestError=false,
            startTime = "2020-10-05 15:15:39 +0200",
            endTime = report!!.endTime
        )

        kotlin.test.assertEquals(expectedReport, report)
    }

    @Test
    fun noChangesIgnoredWhenForceUpdateIsTrue() {
        val harvested = responseReader.readFile("harvest_response_0.ttl")
        whenever(adapter.getEvents(TEST_HARVEST_SOURCE))
            .thenReturn(harvested)
        whenever(valuesMock.eventsUri)
            .thenReturn("http://localhost:5000/public-services")
        whenever(turtleService.getHarvestSource(TEST_HARVEST_SOURCE.url!!))
            .thenReturn(harvested)

        val report = harvester.harvestEvents(TEST_HARVEST_SOURCE, TEST_HARVEST_DATE, true)

        verify(turtleService, times(1)).saveAsHarvestSource(any(), any())
        verify(turtleService, times(6)).saveAsEvent(any(), any(), any())
        verify(metaRepository, times(3)).save(any())

        val expectedReport = HarvestReport(
            id="test-source",
            url="http://localhost:5000/fdk-public-service-publisher.ttl",
            dataType="event",
            harvestError=false,
            startTime = "2020-10-05 15:15:39 +0200",
            endTime = report!!.endTime,
            changedResources=listOf(
                FdkIdAndUri(fdkId="fb77d4f2-a11c-33e3-8c49-772c4569613b", uri="http://testdirektoratet.no/events/2"),
                FdkIdAndUri(fdkId="cbed84c4-a719-3370-b216-725bfc79978d", uri="http://testdirektoratet.no/events/0"),
                FdkIdAndUri(fdkId="99b00c6c-4087-3c23-9244-6e85b9d02adc", uri="http://testdirektoratet.no/events/1"))
        )

        kotlin.test.assertEquals(expectedReport, report)
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
        whenever(metaRepository.findById(EVENT_META_2.uri))
            .thenReturn(Optional.of(EVENT_META_2))
        whenever(turtleService.getEvent(EVENT_ID_0, false))
            .thenReturn(responseReader.readFile("no_records_event_0.ttl"))
        whenever(turtleService.getEvent(EVENT_ID_1, false))
            .thenReturn(responseReader.readFile("no_records_event_1_diff.ttl"))
        whenever(turtleService.getEvent(EVENT_ID_2, false))
            .thenReturn(responseReader.readFile("no_records_event_2.ttl"))

        val report = harvester.harvestEvents(TEST_HARVEST_SOURCE, NEW_TEST_HARVEST_DATE, false)

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

        val expectedReport = HarvestReport(
            id="test-source",
            url="http://localhost:5000/fdk-public-service-publisher.ttl",
            dataType="event",
            harvestError=false,
            startTime = "2020-10-15 13:52:16 +0200",
            endTime = report!!.endTime,
            changedResources = listOf(FdkIdAndUri(fdkId= EVENT_ID_1, uri= EVENT_META_1.uri))
        )

        kotlin.test.assertEquals(expectedReport, report)
    }

    @Test
    fun harvestWithErrorsIsNotPersisted() {
        whenever(adapter.getEvents(TEST_HARVEST_SOURCE))
            .thenReturn(responseReader.readFile("harvest_error_response.ttl"))
        whenever(valuesMock.eventsUri)
            .thenReturn("http://localhost:5000/public-services")

        val report = harvester.harvestEvents(TEST_HARVEST_SOURCE, TEST_HARVEST_DATE, false)

        verify(turtleService, times(0)).saveAsHarvestSource(any(), any())
        verify(turtleService, times(0)).saveAsEvent(any(), any(), any())
        verify(metaRepository, times(0)).save(any())

        val expectedReport = HarvestReport(
            id="test-source",
            url="http://localhost:5000/fdk-public-service-publisher.ttl",
            dataType="event",
            harvestError=true,
            errorMessage = "[line: 1, col: 76] Undefined prefix: cpsv",
            startTime = "2020-10-05 15:15:39 +0200",
            endTime = report!!.endTime
        )

        kotlin.test.assertEquals(expectedReport, report)
    }

}