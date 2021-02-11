package no.fdk.fdk_event_harvester.contract

import no.fdk.fdk_event_harvester.utils.*
import org.junit.jupiter.api.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
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
    fun findAll() {
        val response = apiGet(port, "/events", "text/turtle")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("all_events.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "TURTLE")

        assertTrue(checkIfIsomorphicAndPrintDiff(actual = responseModel, expected = expected, name = "ServicesTest.findAll"))
    }

    @Test
    fun findSpecific() {
        val response = apiGet(port, "/events/$EVENT_ID_0", "application/rdf+json")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("event_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "RDF/JSON")

        assertTrue(checkIfIsomorphicAndPrintDiff(actual = responseModel, expected = expected, name = "ServicesTest.findSpecific"))
    }

    @Test
    fun idDoesNotExist() {
        val response = apiGet(port, "/public-services/123", "text/turtle")
        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
    }

}