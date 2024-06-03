package no.fdk.fdk_event_harvester.adapter

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.fdk_event_harvester.configuration.ApplicationProperties
import no.fdk.fdk_event_harvester.model.HarvestAdminParameters
import no.fdk.fdk_event_harvester.model.HarvestDataSource
import org.slf4j.LoggerFactory
import org.springframework.http.*
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

private val logger = LoggerFactory.getLogger(HarvestAdminAdapter::class.java)

@Service
class HarvestAdminAdapter(private val applicationProperties: ApplicationProperties) {

    fun urlWithParameters(params: HarvestAdminParameters): URL {
        val pathString: String = when {
            params.publisherId.isNullOrBlank() -> "/internal/datasources"
            else -> "/internal/organizations/${params.publisherId}/datasources"
        }

        val paramString: String = when {
            !params.dataType.isNullOrBlank() && !params.dataSourceType.isNullOrBlank() -> {
                "?dataType=${params.dataType}&dataSourceType=${params.dataSourceType}"
            }
            !params.dataType.isNullOrBlank() -> "?dataType=${params.dataType}"
            !params.dataSourceType.isNullOrBlank() -> "?dataSourceType=${params.dataSourceType}"
            else -> ""
        }

        return URI("${applicationProperties.harvestAdminRootUrl}$pathString$paramString").toURL()
    }

    private fun urlForSingleDataSource(params: HarvestAdminParameters): URL {
        val path = "/internal/organizations/${params.publisherId}/datasources/${params.dataSourceId}"
        return URI("${applicationProperties.harvestAdminRootUrl}$path").toURL()
    }

    fun getDataSources(params: HarvestAdminParameters): List<HarvestDataSource> =
        if (!params.dataSourceId.isNullOrBlank() && !params.publisherId.isNullOrBlank()) {
            harvestAdminGet(urlForSingleDataSource(params))
                ?.let { jacksonObjectMapper().readValue(it) as HarvestDataSource }
                ?.let { listOf(it) }
                ?: emptyList()
        } else {
            harvestAdminGet(urlWithParameters(params))
                ?.let { jacksonObjectMapper().readValue(it) as List<HarvestDataSource> }
                ?: emptyList()
        }

    private fun harvestAdminGet(url: URL): String? {
        with(url.openConnection() as HttpURLConnection) {
            try {
                setRequestProperty(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON.toString())
                setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                setRequestProperty("X-API-KEY", applicationProperties.harvestAdminApiKey)

                if (HttpStatus.valueOf(responseCode).is2xxSuccessful) {
                    return inputStream.bufferedReader().use(BufferedReader::readText)
                } else {
                    logger.error(
                        "Fetch of from $url failed, status: $responseCode",
                        Exception("Fetch from harvest admin failed")
                    )
                }
            } catch (ex: Exception) {
                logger.error("Error fetching from $url", ex)
            } finally {
                disconnect()
            }
            return null
        }
    }
}
