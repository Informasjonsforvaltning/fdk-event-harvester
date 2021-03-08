package no.fdk.fdk_event_harvester.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import no.fdk.fdk_event_harvester.utils.*
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
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
            whenever(turtleService.getUnion(true))
                .thenReturn(null)
            whenever(turtleService.getUnion(false))
                .thenReturn(null)

            val expected = responseReader.parseResponse("", "TURTLE")

            val responseTurtle = service.getAll(Lang.TURTLE, true)
            val responseJsonLD = service.getAll(Lang.JSONLD, false)

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle, "TURTLE")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseJsonLD, "JSON-LD")))
        }

        @Test
        fun getAllHandlesTurtleAndOtherRDF() {
            val allEvents = javaClass.classLoader.getResource("all_events.ttl")!!.readText()
            val allEventsNoRecords = javaClass.classLoader.getResource("no_records_all_events.ttl")!!.readText()

            whenever(turtleService.getUnion(true))
                .thenReturn(allEvents)
            whenever(turtleService.getUnion(false))
                .thenReturn(allEventsNoRecords)

            val expected = responseReader.parseFile("all_events.ttl", "TURTLE")
            val expectedNoRecords = responseReader.parseFile("no_records_all_events.ttl", "TURTLE")

            val responseTurtle = service.getAll(Lang.TURTLE, true)
            val responseN3 = service.getAll(Lang.N3, true)

            val responseNTriples = service.getAll(Lang.NTRIPLES, false)
            val responseRdfJson = service.getAll(Lang.RDFJSON, false)

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