package no.fdk.fdk_event_harvester.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import no.fdk.fdk_event_harvester.rdf.JenaType
import no.fdk.fdk_event_harvester.utils.*
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
            whenever(turtleService.getUnion())
                .thenReturn(null)

            val expected = responseReader.parseResponse("", "TURTLE")

            val responseTurtle = service.getAll(JenaType.TURTLE)
            val responseJsonLD = service.getAll(JenaType.JSON_LD)

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle, "TURTLE")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseJsonLD, "JSON-LD")))
        }

        @Test
        fun getAllHandlesTurtleAndOtherRDF() {
            val allEvents = javaClass.classLoader.getResource("all_events.ttl")!!.readText()

            whenever(turtleService.getUnion())
                .thenReturn(allEvents)

            val expected = responseReader.parseFile("all_events.ttl", "TURTLE")

            val responseTurtle = service.getAll(JenaType.TURTLE)
            val responseN3 = service.getAll(JenaType.N3)
            val responseNTriples = service.getAll(JenaType.NTRIPLES)

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle, "TURTLE")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseN3, "N3")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseNTriples, "N-TRIPLES")))
        }

    }

    @Nested
    internal inner class EventById {

        @Test
        fun responseIsNullWhenNoModelIsFound() {
            whenever(turtleService.getEvent("123", true))
                .thenReturn(null)

            val response = service.getEventById("123", JenaType.TURTLE)

            assertNull(response)
        }

        @Test
        fun responseIsIsomorphicWithExpectedModel() {
            val event = javaClass.classLoader.getResource("event_0.ttl")!!.readText()
            whenever(turtleService.getEvent(EVENT_ID_0, true))
                .thenReturn(event)

            val responseTurtle = service.getEventById(EVENT_ID_0, JenaType.TURTLE)
            val responseRDFXML = service.getEventById(EVENT_ID_0, JenaType.RDF_XML)

            val expected = responseReader.parseFile("event_0.ttl", "TURTLE")

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle!!, "TURTLE")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseRDFXML!!, "RDF/XML")))
        }

    }

}