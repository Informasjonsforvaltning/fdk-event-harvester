package no.fdk.fdk_event_harvester.contract

import no.fdk.fdk_event_harvester.utils.*
import org.apache.jena.riot.Lang
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
class CatalogsTest: ApiTestContext() {

    @LocalServerPort
    var port: Int = 0

    private val responseReader = TestResponseReader()

    @Test
    fun findAllWithRecords() {
        val response = apiGet(port, "/events/catalogs?catalogrecords=true", "text/turtle")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("catalog_1.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "TURTLE")

        assertTrue(checkIfIsomorphicAndPrintDiff(actual = responseModel, expected = expected, name = "CatalogsTest.findAll"))
    }

    @Test
    fun findAllNoRecords() {
        val response = apiGet(port, "/events/catalogs", "application/trig")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("harvest_response_1.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.TRIG.name)

        assertTrue(checkIfIsomorphicAndPrintDiff(actual = responseModel, expected = expected, name = "CatalogsTest.findAll"))
    }

    @Test
    fun findSpecificWithRecords() {
        val response = apiGet(port, "/events/catalogs/$CATALOG_ID_1?catalogrecords=true", "application/rdf+json")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("catalog_1.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "RDF/JSON")

        assertTrue(checkIfIsomorphicAndPrintDiff(actual = responseModel, expected = expected, name = "CatalogsTest.findSpecific"))
    }

    @Test
    fun findSpecificNoRecords() {
        val response = apiGet(port, "/events/catalogs/$CATALOG_ID_1", "application/n-quads")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("harvest_response_1.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.NQUADS.name)

        assertTrue(checkIfIsomorphicAndPrintDiff(actual = responseModel, expected = expected, name = "CatalogsTest.findSpecific"))
    }

    @Test
    fun idDoesNotExist() {
        val response = apiGet(port, "/events/catalogs/123", "text/turtle")
        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
    }

}
