package no.fdk.fdk_event_harvester.adapter

import no.fdk.fdk_event_harvester.harvester.HarvestException
import no.fdk.fdk_event_harvester.model.HarvestDataSource
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URI

private val LOGGER = LoggerFactory.getLogger(EventAdapter::class.java)
private const val TEN_MINUTES = 600000

@Service
class EventAdapter {

    fun getEvents(source: HarvestDataSource): String {
        val connection = URI(source.url).toURL().openConnection() as HttpURLConnection
        try {
            connection.setRequestProperty("Accept", source.acceptHeaderValue)
            connection.connectTimeout = TEN_MINUTES
            connection.readTimeout = TEN_MINUTES

            return if (connection.responseCode != HttpStatus.OK.value()) {
                throw HarvestException("${source.url} responded with ${connection.responseCode}, harvest will be aborted")
            } else {
                connection
                    .inputStream
                    .bufferedReader()
                    .use(BufferedReader::readText)
            }

        } finally {
            connection.disconnect()
        }
    }

}
