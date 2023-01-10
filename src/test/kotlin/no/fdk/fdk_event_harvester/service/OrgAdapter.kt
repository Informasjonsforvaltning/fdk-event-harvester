package no.fdk.fdk_event_harvester.service

import no.fdk.fdk_event_harvester.adapter.OrganizationsAdapter
import no.fdk.fdk_event_harvester.configuration.ApplicationProperties
import no.fdk.fdk_event_harvester.utils.ApiTestContext
import no.fdk.fdk_event_harvester.utils.ORGANIZATION_0
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Tag("unit")
class OrgAdapterTest: ApiTestContext() {
    private val valuesMock: ApplicationProperties = mock()
    private val adapter = OrganizationsAdapter(valuesMock)

    @Test
    fun missingOrg() {
        whenever(valuesMock.organizationsUri)
            .thenReturn("http://localhost:5000/organizations")

        val org = adapter.getOrganization("111222333")

        assertNull(org)
    }

    @Test
    fun getOrg() {
        whenever(valuesMock.organizationsUri)
            .thenReturn("http://localhost:5000/organizations")

        val org = adapter.getOrganization("123456789")

        assertEquals(ORGANIZATION_0, org)
    }
}