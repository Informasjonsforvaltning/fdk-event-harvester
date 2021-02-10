package no.fdk.fdk_event_harvester.service;

import no.fdk.fdk_event_harvester.configuration.ApplicationProperties
import org.springframework.stereotype.Component

@Component
class EndpointPermissions(
    private val applicationProperties: ApplicationProperties
) {
    fun isFromFDKCluster(apiKey: String?): Boolean =
        when (apiKey) {
            null -> false
            applicationProperties.fdkApiKey -> true
            else -> false
        }
}
