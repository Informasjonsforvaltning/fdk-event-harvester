package no.fdk.fdk_event_harvester.adapter

import no.fdk.fdk_event_harvester.model.HarvestDataSource
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

private val LOGGER = LoggerFactory.getLogger(EventAdapter::class.java)

@Service
class EventAdapter {

    fun getEvents(source: HarvestDataSource): String? {
        val connection = URL(source.url).openConnection() as HttpURLConnection
        try {
            connection.setRequestProperty("Accept", source.acceptHeaderValue)

            return if (connection.responseCode != HttpStatus.OK.value()) {
                LOGGER.error(Exception("${source.url} responded with ${connection.responseCode}, harvest will be aborted").stackTraceToString())
                null
            } else {
                connection
                    .inputStream
                    .bufferedReader()
                    .use(BufferedReader::readText)
            }

        } catch (ex: Exception) {
            LOGGER.error("${ex.stackTraceToString()}: Error when harvesting from ${source.url}")
            return null
        } finally {
            connection.disconnect()
        }
    }

}
