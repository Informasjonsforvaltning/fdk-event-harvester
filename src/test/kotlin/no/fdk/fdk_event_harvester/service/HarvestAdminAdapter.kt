package no.fdk.fdk_event_harvester.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import no.fdk.fdk_event_harvester.adapter.HarvestAdminAdapter
import no.fdk.fdk_event_harvester.configuration.ApplicationProperties
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.net.URL
import kotlin.test.assertEquals

@Tag("unit")
class HarvestAdminAdapterTest {
    private val valuesMock: ApplicationProperties = mock()
    private val adapter = HarvestAdminAdapter(valuesMock)

    @Nested
    internal inner class UrlWithParameters {

        @Test
        fun nullParameters() {
            whenever(valuesMock.harvestAdminRootUrl)
                .thenReturn("http://www.example.com")

            val url = adapter.urlWithParameters(null)

            assertEquals(URL("http://www.example.com/datasources"), url)
        }

        @Test
        fun emptyParameters() {
            whenever(valuesMock.harvestAdminRootUrl)
                .thenReturn("http://www.example.com")

            val url = adapter.urlWithParameters(emptyMap())

            assertEquals(URL("http://www.example.com/datasources"), url)
        }

        @Test
        fun singleParameter() {
            whenever(valuesMock.harvestAdminRootUrl)
                .thenReturn("http://www.example.com")

            val url = adapter.urlWithParameters(mapOf(Pair("key0", "value0")))

            assertEquals(URL("http://www.example.com/datasources?key0=value0"), url)
        }

        @Test
        fun severalParameters() {
            whenever(valuesMock.harvestAdminRootUrl)
                .thenReturn("http://www.example.com")

            val url = adapter.urlWithParameters(
                mapOf(Pair("key0", "value0"), Pair("key1", "value1"), Pair("key2", "value2")))

            assertEquals(URL("http://www.example.com/datasources?key0=value0&key1=value1&key2=value2"), url)
        }

    }
}