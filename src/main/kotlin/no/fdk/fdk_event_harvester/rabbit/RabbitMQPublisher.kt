package no.fdk.fdk_event_harvester.rabbit

import no.fdk.fdk_event_harvester.model.UpdateSearchMessage
import org.slf4j.LoggerFactory
import org.springframework.amqp.AmqpException
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

private val LOGGER = LoggerFactory.getLogger(RabbitMQPublisher::class.java)

@Service
class RabbitMQPublisher(private val template: RabbitTemplate) {
    fun send(dbId: String?) {
        try {
            template.convertAndSend("harvests", "events.harvester.UpdateSearchTrigger", UpdateSearchMessage(dbId))
            template.convertAndSend("harvests", "events.harvested", UpdateSearchMessage(dbId))
            LOGGER.debug("Successfully sent UpdateSearchTrigger for $dbId")
        } catch (e: AmqpException) {
            LOGGER.error("Could not trigger search update", e)
        }
    }
}
