package no.fdk.fdk_event_harvester.service

import no.fdk.fdk_event_harvester.utils.*
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("unit")
class EventServiceTest {
    private val turtleService: TurtleService = mock()
    private val service = EventService(turtleService)

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

}