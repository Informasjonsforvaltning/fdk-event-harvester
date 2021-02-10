package no.fdk.fdk_event_harvester.adapter

import no.fdk.fdk_event_harvester.configuration.FusekiProperties
import no.fdk.fdk_event_harvester.rdf.JenaType
import no.fdk.fdk_event_harvester.rdf.createRDFResponse
import org.apache.jena.rdf.model.Model
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

private val LOGGER = LoggerFactory.getLogger(FusekiAdapter::class.java)

@Service
class FusekiAdapter(private val fusekiProperties: FusekiProperties) {

    fun storeUnionModel(model: Model) =
        try {
            LOGGER.debug("Updating fuseki graph")
            with(URL(fusekiProperties.unionGraphUri).openConnection() as HttpURLConnection) {
                setRequestProperty("Content-type", "application/rdf+xml")
                requestMethod = "PUT"
                doOutput = true

                OutputStreamWriter(outputStream).use {
                    it.write(model.createRDFResponse(JenaType.RDF_XML))
                    it.flush()
                }

                if (HttpStatus.valueOf(responseCode).is2xxSuccessful) {
                    LOGGER.debug("Save to fuseki completed, status: $responseCode")
                } else {
                    LOGGER.error("Save to fuseki failed, status: $responseCode")
                }
            }
        } catch (ex: Exception) {
            LOGGER.error("Error when saving to fuseki", ex)
        }

}
