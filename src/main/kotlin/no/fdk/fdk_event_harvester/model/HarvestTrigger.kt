package no.fdk.fdk_event_harvester.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown=true)
data class HarvestTrigger(
    val runId: String? = null,
    val dataSourceId: String? = null,
    val dataSourceUrl: String? = null,
    val dataType: String? = null,
    val acceptHeader: String? = null,
    val timestamp: Long? = null,
    val publisherId: String? = null,
    val dataSourceType: String? = null,
    val forceUpdate: Boolean = false
)
