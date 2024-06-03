package no.fdk.fdk_event_harvester.adapter

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.fdk_event_harvester.configuration.ApplicationProperties
import no.fdk.fdk_event_harvester.model.Organization
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

private val logger = LoggerFactory.getLogger(OrganizationsAdapter::class.java)

@Service
class OrganizationsAdapter(private val applicationProperties: ApplicationProperties) {
    fun getOrganization(id: String): Organization? {
        val uri = "${applicationProperties.organizationsUri}/$id"
        with(URI(uri).toURL().openConnection() as HttpURLConnection) {
            try {
                setRequestProperty(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON.toString())

                if (HttpStatus.valueOf(responseCode).is2xxSuccessful) {
                    val body = inputStream.bufferedReader().use(BufferedReader::readText)
                    return jacksonObjectMapper()
                        .readValue<Organization?>(body)
                        ?.copy(uri = uri)
                } else {
                    logger.error("Fetch of organization with id $id failed, status: $responseCode", Exception("Fetch of organization with id $id failed"))
                }
            } catch (ex: Exception) {
                logger.error("Error fetching organization with id $id", ex)
            } finally {
                disconnect()
            }
            return null
        }
    }
}
