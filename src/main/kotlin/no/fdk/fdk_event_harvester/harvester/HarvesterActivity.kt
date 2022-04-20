package no.fdk.fdk_event_harvester.harvester

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import no.fdk.fdk_event_harvester.adapter.HarvestAdminAdapter
import no.fdk.fdk_event_harvester.model.HarvestAdminParameters
import no.fdk.fdk_event_harvester.rabbit.RabbitMQPublisher
import no.fdk.fdk_event_harvester.service.UpdateService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.*

private val LOGGER = LoggerFactory.getLogger(HarvesterActivity::class.java)

@Service
class HarvesterActivity(
    private val harvestAdminAdapter: HarvestAdminAdapter,
    private val harvester: EventHarvester,
    private val publisher: RabbitMQPublisher,
    private val updateService: UpdateService
): CoroutineScope by CoroutineScope(Dispatchers.Default) {

    private val activitySemaphore = Semaphore(1)

    @EventListener
    fun fullHarvestOnStartup(event: ApplicationReadyEvent) = initiateHarvest(null)

    fun initiateHarvest(params: HarvestAdminParameters?) {
        if (params == null) LOGGER.debug("starting harvest of all events")
        else LOGGER.debug("starting harvest with parameters $params")

        launch {
            activitySemaphore.withPermit {
                harvestAdminAdapter.getDataSources(params ?: HarvestAdminParameters())
                    .filter { it.dataType == "publicService" }
                    .filter { it.url != null }
                    .map { async { harvester.harvestEvents(it, Calendar.getInstance()) } }
                    .awaitAll()
                    .filterNotNull()
                    .also { updateService.updateUnionModel() }
                    .also {
                        if (params != null) LOGGER.debug("completed harvest with parameters $params")
                        else LOGGER.debug("completed harvest of all catalogs") }
                    .run { publisher.send(this) }
            }
        }
    }
}
