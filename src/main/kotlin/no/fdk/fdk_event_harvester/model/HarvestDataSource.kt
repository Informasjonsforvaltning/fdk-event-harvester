package no.fdk.fdk_event_harvester.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown=true)
data class HarvestDataSource (
    val dataSourceType: String? = null,
    val dataType: String? = null,
    val url: String? = null,
    val acceptHeaderValue: String? = null
)