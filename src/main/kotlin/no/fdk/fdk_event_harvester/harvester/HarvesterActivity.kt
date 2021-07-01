package no.fdk.fdk_event_harvester.harvester

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import no.fdk.fdk_event_harvester.adapter.HarvestAdminAdapter
import no.fdk.fdk_event_harvester.rabbit.RabbitMQPublisher
import no.fdk.fdk_event_harvester.service.UpdateService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.*

private val LOGGER = LoggerFactory.getLogger(HarvesterActivity::class.java)
private const val HARVEST_ALL_ID = "all"

@Service
class HarvesterActivity(
    private val harvestAdminAdapter: HarvestAdminAdapter,
    private val harvester: EventHarvester,
    private val publisher: RabbitMQPublisher,
    private val updateService: UpdateService
): CoroutineScope by CoroutineScope(Dispatchers.Default) {

    @EventListener
    fun fullHarvestOnStartup(event: ApplicationReadyEvent) = initiateHarvest(null)

    fun initiateHarvest(params: Map<String, String>?) {
        if (params == null || params.isEmpty()) LOGGER.debug("starting harvest of all events")
        else LOGGER.debug("starting harvest with parameters $params")

        val harvest = launch {
            harvestAdminAdapter.getDataSources(params)
                .filter { it.dataType == "publicService" }
                .filter { it.url != null }
                .forEach {
                    try {
                        harvester.harvestEvents(it, Calendar.getInstance())
                    } catch (exception: Exception) {
                        LOGGER.error("${exception.stackTraceToString()}: Harvest of ${it.url} failed")
                    }
                }
        }

        val onHarvestCompletion = launch {
            harvest.join()
            LOGGER.debug("Updating union model")
            updateService.updateUnionModel()

            if (params == null || params.isEmpty()) LOGGER.debug("completed harvest of all events")
            else LOGGER.debug("completed harvest with parameters $params")

            publisher.send(HARVEST_ALL_ID)

            harvest.cancelChildren()
            harvest.cancel()
        }

        onHarvestCompletion.invokeOnCompletion { onHarvestCompletion.cancel() }
    }
}
