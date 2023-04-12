package no.fdk.fdk_event_harvester.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("application")
data class ApplicationProperties(
    val organizationsUri: String,
    val eventsUri: String,
    val harvestAdminRootUrl: String
)
