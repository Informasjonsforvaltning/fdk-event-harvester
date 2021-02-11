package no.fdk.fdk_event_harvester.contract

import no.fdk.fdk_event_harvester.utils.*
import org.junit.jupiter.api.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class UpdateTest: ApiTestContext() {

    @LocalServerPort
    var port: Int = 0

    private val responseReader = TestResponseReader()

    @Test
    fun forbiddenWhenNoApiKey() {
        val response = apiPost(port, "/update/meta", emptyMap())

        assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
    }

    @Test
    fun forbiddenWhenWrongApiKey() {
        val response = apiPost(port, "/update/meta", mapOf(Pair("X-API-KEY", "wrong-api-key")))

        assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
    }

    @Test
    fun noChangesWhenRunOnCorrectMeta() {
        val all = apiGet(port, "/events", "text/turtle")
        val catalog = apiGet(port, "/events/$EVENT_ID_0", "text/turtle")
        val infoModel = apiGet(port, "/events/$EVENT_ID_1", "text/turtle")

        val response = apiPost(port, "/update/meta", mapOf(Pair("X-API-KEY", "fdk-api-key")))

        assertEquals(HttpStatus.OK.value(), response["status"])

        val expectedAll = responseReader.parseResponse(all["body"] as String, "TURTLE")
        val expectedCatalog = responseReader.parseResponse(catalog["body"] as String, "TURTLE")
        val expectedInfoModel = responseReader.parseResponse(infoModel["body"] as String, "TURTLE")

        val allAfterUpdate = apiGet(port, "/events", "text/turtle")
        val catalogAfterUpdate = apiGet(port, "/events/$EVENT_ID_0", "text/turtle")
        val infoModelAfterUpdate = apiGet(port, "/events/$EVENT_ID_1", "text/turtle")

        val actualAll = responseReader.parseResponse(allAfterUpdate["body"] as String, "TURTLE")
        val actualCatalog = responseReader.parseResponse(catalogAfterUpdate["body"] as String, "TURTLE")
        val actualInfoModel = responseReader.parseResponse(infoModelAfterUpdate["body"] as String, "TURTLE")

        assertTrue(checkIfIsomorphicAndPrintDiff(expectedAll, actualAll, "UpdateMetaAll"))
        assertTrue(checkIfIsomorphicAndPrintDiff(expectedCatalog, actualCatalog, "UpdateMetaCatalog"))
        assertTrue(checkIfIsomorphicAndPrintDiff(expectedInfoModel, actualInfoModel, "UpdateMetaInfo"))
    }

}