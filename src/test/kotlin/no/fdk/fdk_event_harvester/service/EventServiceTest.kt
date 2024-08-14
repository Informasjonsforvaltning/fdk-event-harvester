package no.fdk.fdk_event_harvester.service

import no.fdk.fdk_event_harvester.model.EventMeta
import no.fdk.fdk_event_harvester.model.FdkIdAndUri
import no.fdk.fdk_event_harvester.model.HarvestReport
import no.fdk.fdk_event_harvester.rabbit.RabbitMQPublisher
import no.fdk.fdk_event_harvester.repository.EventMetaRepository
import no.fdk.fdk_event_harvester.utils.*
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.capture
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.web.server.ResponseStatusException
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("unit")
class EventServiceTest {
    private val repository: EventMetaRepository = mock()
    private val publisher: RabbitMQPublisher = mock()
    private val turtleService: TurtleService = mock()
    private val service = EventService(repository, publisher, turtleService)

    private val responseReader = TestResponseReader()

    @Nested
    internal inner class AllEvents {

        @Test
        fun responseIsometricWithEmptyModelForEmptyDB() {
            whenever(turtleService.getEventUnion(true))
                .thenReturn(null)
            whenever(turtleService.getEventUnion(false))
                .thenReturn(null)

            val expected = responseReader.parseResponse("", "TURTLE")

            val responseTurtle = service.getAllEvents(Lang.TURTLE, true)
            val responseJsonLD = service.getAllEvents(Lang.JSONLD, false)

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle, "TURTLE")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseJsonLD, "JSON-LD")))
        }

        @Test
        fun getAllHandlesTurtleAndOtherRDF() {
            val allEvents = javaClass.classLoader.getResource("all_catalogs.ttl")!!.readText()
            val allEventsNoRecords = javaClass.classLoader.getResource("no_records_all_events.ttl")!!.readText()

            whenever(turtleService.getEventUnion(true))
                .thenReturn(allEvents)
            whenever(turtleService.getEventUnion(false))
                .thenReturn(allEventsNoRecords)

            val expected = responseReader.parseFile("all_catalogs.ttl", "TURTLE")
            val expectedNoRecords = responseReader.parseFile("no_records_all_events.ttl", "TURTLE")

            val responseTurtle = service.getAllEvents(Lang.TURTLE, true)
            val responseN3 = service.getAllEvents(Lang.N3, true)

            val responseNTriples = service.getAllEvents(Lang.NTRIPLES, false)
            val responseRdfJson = service.getAllEvents(Lang.RDFJSON, false)

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle, "TURTLE")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseN3, "N3")))

            assertTrue(expectedNoRecords.isIsomorphicWith(responseReader.parseResponse(responseNTriples, "N-TRIPLES")))
            assertTrue(expectedNoRecords.isIsomorphicWith(responseReader.parseResponse(responseRdfJson, "RDFJSON")))
        }

    }

    @Nested
    internal inner class EventById {

        @Test
        fun responseIsNullWhenNoModelIsFound() {
            whenever(turtleService.getEvent("123", true))
                .thenReturn(null)

            val response = service.getEventById("123", Lang.TURTLE, true)

            assertNull(response)
        }

        @Test
        fun responseIsIsomorphicWithExpectedModel() {
            whenever(turtleService.getEvent(EVENT_ID_0, true))
                .thenReturn(javaClass.classLoader.getResource("event_0.ttl")!!.readText())
            whenever(turtleService.getEvent(EVENT_ID_0, false))
                .thenReturn(javaClass.classLoader.getResource("no_records_event_0.ttl")!!.readText())

            val responseTurtle = service.getEventById(EVENT_ID_0, Lang.TURTLE, true)
            val responseRDFXML = service.getEventById(EVENT_ID_0, Lang.RDFXML, false)

            val expected = responseReader.parseFile("event_0.ttl", "TURTLE")
            val expectedNoRecords = responseReader.parseFile("no_records_event_0.ttl", "TURTLE")

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle!!, "TURTLE")))
            assertTrue(expectedNoRecords.isIsomorphicWith(responseReader.parseResponse(responseRDFXML!!, "RDF/XML")))
        }

    }

    @Nested
    internal inner class RemoveEventById {

        @Test
        fun throwsResponseStatusExceptionWhenNoMetaFoundInDB() {
            whenever(repository.findAllByFdkId("123"))
                .thenReturn(emptyList())

            assertThrows<ResponseStatusException> { service.removeEvent("123") }
        }

        @Test
        fun throwsResponseStatusExceptionNoNonRemovedMetaFoundInDB() {
            whenever(repository.findAllByFdkId(EVENT_ID_0))
                .thenReturn(listOf(EVENT_META_0.copy(removed = true)))

            assertThrows<ResponseStatusException> { service.removeEvent(EVENT_ID_0) }
        }

        @Test
        fun updatesMetaAndSendsRabbitReportWhenMetaIsFound() {
            whenever(repository.findAllByFdkId(EVENT_ID_0))
                .thenReturn(listOf(EVENT_META_0))

            service.removeEvent(EVENT_META_0.fdkId)

            argumentCaptor<List<EventMeta>>().apply {
                verify(repository, times(1)).saveAll(capture())
                assertEquals(listOf(EVENT_META_0.copy(removed = true)), firstValue)
            }

            val expectedReport = HarvestReport(
                id = "manual-delete-$EVENT_ID_0",
                url = EVENT_META_0.uri,
                harvestError = false,
                startTime = "startTime",
                endTime = "endTime",
                removedResources = listOf(FdkIdAndUri(EVENT_META_0.fdkId, EVENT_META_0.uri))
            )
            argumentCaptor<List<HarvestReport>>().apply {
                verify(publisher, times(1)).send(capture())

                assertEquals(
                    listOf(expectedReport.copy(
                        startTime = firstValue.first().startTime,
                        endTime = firstValue.first().endTime
                    )),
                    firstValue
                )
            }
        }

    }

}
