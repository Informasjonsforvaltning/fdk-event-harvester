package no.fdk.fdk_event_harvester.contract

import no.fdk.fdk_event_harvester.utils.apiGet
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("contract")
class StatusTest {

    @LocalServerPort
    var port: Int = 0

    @Test
    fun ping() {
        val response = apiGet(port, "/ping", null)

        assertEquals(HttpStatus.OK.value(), response["status"])
    }

    @Test
    @Disabled("TODO: Make sure rabbit is initialized before running this test")
    fun ready() {
        val response = apiGet(port, "/ready", null)

        assertEquals(HttpStatus.OK.value(), response["status"])
    }

}