package no.fdk.fdk_event_harvester.contract

import no.fdk.fdk_event_harvester.utils.*
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class EventServicesTest: ApiTestContext() {

    @LocalServerPort
    var port: Int = 0

    private val responseReader = TestResponseReader()

    @Test
    fun findAllWithRecords() {
        val response = apiGet(port, "/events?catalogrecords=true", "text/turtle")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("all_catalogs.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "TURTLE")

        assertTrue(checkIfIsomorphicAndPrintDiff(actual = responseModel, expected = expected, name = "ServicesTest.findAll"))
    }

    @Test
    fun findAllNoRecords() {
        val response = apiGet(port, "/events", "application/trig")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("no_records_all_events.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.TRIG.name)

        assertTrue(checkIfIsomorphicAndPrintDiff(actual = responseModel, expected = expected, name = "ServicesTest.findAll"))
    }

    @Test
    fun findSpecificWithRecords() {
        val response = apiGet(port, "/events/$EVENT_ID_0?catalogrecords=true", "application/rdf+json")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("event_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "RDF/JSON")

        assertTrue(checkIfIsomorphicAndPrintDiff(actual = responseModel, expected = expected, name = "ServicesTest.findSpecific"))
    }

    @Test
    fun findSpecificNoRecords() {
        val response = apiGet(port, "/events/$EVENT_ID_0", "application/trix")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("no_records_event_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.TRIX.name)

        assertTrue(checkIfIsomorphicAndPrintDiff(actual = responseModel, expected = expected, name = "ServicesTest.findSpecific"))
    }

    @Test
    fun idDoesNotExist() {
        val response = apiGet(port, "/public-services/123", "text/turtle")
        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
    }

}
