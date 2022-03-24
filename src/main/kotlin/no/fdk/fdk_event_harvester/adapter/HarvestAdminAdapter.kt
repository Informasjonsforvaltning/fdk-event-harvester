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
import java.net.URL

private val logger = LoggerFactory.getLogger(HarvestAdminAdapter::class.java)

@Service
class HarvestAdminAdapter(private val applicationProperties: ApplicationProperties) {

    fun urlWithParameters(params: HarvestAdminParameters): URL {
        val pathString: String = when {
            !params.dataSourceId.isNullOrBlank() && !params.publisherId.isNullOrBlank() ->
                "/organizations/${params.publisherId}/datasources/${params.dataSourceId}"
            params.publisherId.isNullOrBlank() -> "/datasources"
            else -> "/organizations/${params.publisherId}/datasources"
        }

        val paramString: String = when {
            !params.dataType.isNullOrBlank() && !params.dataSourceType.isNullOrBlank() -> {
                "?dataType=${params.dataType}&dataSourceType=${params.dataSourceType}"
            }
            !params.dataType.isNullOrBlank() -> "?dataType=${params.dataType}"
            !params.dataSourceType.isNullOrBlank() -> "?dataSourceType=${params.dataSourceType}"
            else -> ""
        }

        return URL("${applicationProperties.harvestAdminRootUrl}$pathString$paramString")
    }

    fun getDataSources(params: HarvestAdminParameters): List<HarvestDataSource> {
        val url = urlWithParameters(params)
        with(url.openConnection() as HttpURLConnection) {
            try {
                setRequestProperty(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON.toString())
                setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())

                if (HttpStatus.valueOf(responseCode).is2xxSuccessful) {
                    val body = inputStream.bufferedReader().use(BufferedReader::readText)
                    return jacksonObjectMapper().readValue(body)
                } else {
                    logger.error("Fetch of harvest urls from $url failed, status: $responseCode", Exception("Fetch of data sources failed"))
                }
            } catch (ex: Exception) {
                logger.error("Error fetching harvest urls from $url", ex)
            } finally {
                disconnect()
            }
            return emptyList()
        }
    }

}
