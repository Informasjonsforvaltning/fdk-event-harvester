package no.fdk.fdk_event_harvester.contract

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.fdk.fdk_event_harvester.model.DuplicateIRI
import no.fdk.fdk_event_harvester.utils.*
import no.fdk.fdk_event_harvester.utils.jwk.Access
import no.fdk.fdk_event_harvester.utils.jwk.JwtToken
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class EventServicesTest : ApiTestContext() {

    @LocalServerPort
    var port: Int = 0

    private val responseReader = TestResponseReader()
    private val mapper = jacksonObjectMapper()

    @Test
    fun findSpecificWithRecords() {
        val response = apiGet(port, "/events/$EVENT_ID_0?catalogrecords=true", "application/rdf+json")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("event_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "RDF/JSON")

        assertTrue(
            checkIfIsomorphicAndPrintDiff(
                actual = responseModel,
                expected = expected,
                name = "ServicesTest.findSpecific"
            )
        )
    }

    @Test
    fun findSpecificNoRecords() {
        val response = apiGet(port, "/events/$EVENT_ID_0", "application/trix")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("no_records_event_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.TRIX.name)

        assertTrue(
            checkIfIsomorphicAndPrintDiff(
                actual = responseModel,
                expected = expected,
                name = "ServicesTest.findSpecific"
            )
        )
    }

    @Test
    fun idDoesNotExist() {
        val response = apiGet(port, "/events/123", "text/turtle")
        assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
    }

    @Nested
    internal inner class RemoveEventById {

        @Test
        fun unauthorizedForNoToken() {
            val response = authorizedRequest(
                port,
                "/events/$EVENT_ID_0/remove",
                null,
                HttpMethod.POST
            )
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
        }

        @Test
        fun forbiddenWithNonSysAdminRole() {
            val response = authorizedRequest(
                port,
                "/events/$EVENT_ID_0/remove",
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )
            assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
        }

        @Test
        fun notFoundWhenIdNotInDB() {
            val response = authorizedRequest(
                port,
                "/events/123/remove",
                JwtToken(Access.ROOT).toString(),
                HttpMethod.POST
            )
            assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
        }

        @Test
        fun okWithSysAdminRole() {
            val response = authorizedRequest(
                port,
                "/events/$EVENT_ID_0/remove",
                JwtToken(Access.ROOT).toString(),
                HttpMethod.POST
            )
            assertEquals(HttpStatus.OK.value(), response["status"])
        }
    }

    @Nested
    internal inner class RemoveDuplicates {

        @Test
        fun unauthorizedForNoToken() {
            val body = listOf(DuplicateIRI(iriToRemove = EVENT_META_0.uri, iriToRetain = EVENT_META_1.uri))
            val response = authorizedRequest(
                port,
                "/events/remove-duplicates",
                null,
                HttpMethod.POST,
                mapper.writeValueAsString(body)
            )
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
        }

        @Test
        fun forbiddenWithNonSysAdminRole() {
            val body = listOf(DuplicateIRI(iriToRemove = EVENT_META_0.uri, iriToRetain = EVENT_META_0.uri))
            val response = authorizedRequest(
                port,
                "/events/remove-duplicates",
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST,
                mapper.writeValueAsString(body)
            )
            assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
        }

        @Test
        fun badRequestWhenRemoveIRINotInDB() {
            val body = listOf(DuplicateIRI(iriToRemove = "https://123.no", iriToRetain = EVENT_META_0.uri))
            val response =
                authorizedRequest(
                    port,
                    "/events/remove-duplicates",
                    JwtToken(Access.ROOT).toString(),
                    HttpMethod.POST,
                    mapper.writeValueAsString(body)
                )
            assertEquals(HttpStatus.BAD_REQUEST.value(), response["status"])
        }

        @Test
        fun okWithSysAdminRole() {
            val body = listOf(DuplicateIRI(iriToRemove = EVENT_META_0.uri, iriToRetain = EVENT_META_0.uri))
            val response = authorizedRequest(
                port,
                "/events/remove-duplicates",
                JwtToken(Access.ROOT).toString(),
                HttpMethod.POST,
                mapper.writeValueAsString(body)
            )
            assertEquals(HttpStatus.OK.value(), response["status"])
        }
    }

    @Nested
    internal inner class PurgeById {

        @Test
        fun unauthorizedForNoToken() {
            val response = authorizedRequest(port, "/events/removed", null, HttpMethod.DELETE)
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
        }

        @Test
        fun forbiddenWithNonSysAdminRole() {
            val response = authorizedRequest(
                port,
                "/events/removed",
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.DELETE
            )
            assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
        }

        @Test
        fun badRequestWhenNotAlreadyRemoved() {
            val response = authorizedRequest(
                port,
                "/events/$EVENT_ID_1",
                JwtToken(Access.ROOT).toString(),
                HttpMethod.DELETE
            )
            assertEquals(HttpStatus.BAD_REQUEST.value(), response["status"])
        }

        @Test
        fun purgingStopsDeepLinking() {
            val pre = apiGet(port, "/events/removed", "text/turtle")
            assertEquals(HttpStatus.OK.value(), pre["status"])

            val response = authorizedRequest(
                port,
                "/events/removed",
                JwtToken(Access.ROOT).toString(),
                HttpMethod.DELETE
            )
            assertEquals(HttpStatus.NO_CONTENT.value(), response["status"])

            val post = apiGet(port, "/events/removed", "text/turtle")
            assertEquals(HttpStatus.NOT_FOUND.value(), post["status"])
        }

    }

}
