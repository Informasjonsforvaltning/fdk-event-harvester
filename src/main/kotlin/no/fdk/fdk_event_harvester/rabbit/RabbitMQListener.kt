package no.fdk.fdk_event_harvester.rabbit

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import no.fdk.fdk_event_harvester.harvester.HarvesterActivity
import no.fdk.fdk_event_harvester.model.HarvestAdminParameters
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

private val logger = LoggerFactory.getLogger(RabbitMQListener::class.java)
private val ALLOWED_FIELDS = listOf("publisherId", "dataType")

@Service
class RabbitMQListener(
    private val objectMapper: ObjectMapper,
    private val harvesterActivity: HarvesterActivity
) {

    private fun createQueryParams(body: JsonNode?): HarvestAdminParameters? =
        try {
            val params = objectMapper.convertValue(body, object : TypeReference<HarvestAdminParameters>() {})
            if (params.publisherId != null || params.dataType != null || params.dataSourceType != null) {
                params
            } else null
        } catch (ex: Exception) {
            logger.info("exception when converting query params", ex)
            null
        }

    @RabbitListener(queues = ["#{receiverQueue.name}"])
    fun receiveServicesHarvestTrigger(@Payload body: JsonNode?, message: Message) {
        logger.info("Received message with key ${message.messageProperties.receivedRoutingKey}")

        harvesterActivity.initiateHarvest(createQueryParams(body))
    }

}
