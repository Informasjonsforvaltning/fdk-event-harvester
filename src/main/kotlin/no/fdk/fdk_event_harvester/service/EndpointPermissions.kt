package no.fdk.fdk_event_harvester.service;

import no.fdk.fdk_event_harvester.configuration.ApplicationProperties
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest

@Component
class EndpointPermissions(
    private val applicationProperties: ApplicationProperties
) {

    fun isFromFDKCluster(httpServletRequest: HttpServletRequest): Boolean =
        when (httpServletRequest.getHeader("X-API-KEY")) {
            null -> false
            applicationProperties.fdkApiKey -> true
            else -> false
        }
}