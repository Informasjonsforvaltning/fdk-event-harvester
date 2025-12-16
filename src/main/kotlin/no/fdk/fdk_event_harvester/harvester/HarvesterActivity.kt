package no.fdk.fdk_event_harvester.harvester

import io.micrometer.core.instrument.Metrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import no.fdk.fdk_event_harvester.model.HarvestReport
import no.fdk.fdk_event_harvester.model.HarvestTrigger
import no.fdk.fdk_event_harvester.rabbit.RabbitMQPublisher
import no.fdk.fdk_event_harvester.service.UpdateService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Calendar
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration

private val LOGGER = LoggerFactory.getLogger(HarvesterActivity::class.java)

@Service
class HarvesterActivity(
    private val harvester: EventHarvester,
    private val publisher: RabbitMQPublisher,
    private val updateService: UpdateService
) : CoroutineScope by CoroutineScope(Dispatchers.Default) {

    private val activitySemaphore = Semaphore(1)

    fun initiateHarvest(trigger: HarvestTrigger) {
        LOGGER.debug("starting harvest of datasource ${trigger.dataSourceId} (${trigger.dataSourceUrl}), force update: ${trigger.forceUpdate}")

        launch {
            activitySemaphore.withPermit {
                try {
                    val harvestDate = Calendar.getInstance()
                    val (report, timeElapsed) = measureTimedValue {
                        harvester.harvestEvents(trigger, harvestDate)
                    }
                    Metrics.counter("harvest_count",
                        "status", if (report?.harvestError == false) { "success" }  else { "error" },
                        "type", "event",
                        "force_update", "${trigger.forceUpdate}",
                        "datasource_id", trigger.dataSourceId,
                        "datasource_url", trigger.dataSourceUrl
                    ).increment()
                    if (report?.harvestError == false) {
                        Metrics.counter("harvest_changed_resources_count",
                            "type", "event",
                            "force_update", "${trigger.forceUpdate}",
                            "datasource_id", trigger.dataSourceId,
                            "datasource_url", trigger.dataSourceUrl
                        ).increment(report.changedResources.size.toDouble())
                        Metrics.counter("harvest_removed_resources_count",
                            "type", "event",
                            "force_update", "${trigger.forceUpdate}",
                            "datasource_id", trigger.dataSourceId,
                            "datasource_url", trigger.dataSourceUrl
                        ).increment(report.removedResources.size.toDouble())
                        Metrics.timer("harvest_time",
                            "type", "event",
                            "force_update", "${trigger.forceUpdate}",
                            "datasource_id", trigger.dataSourceId,
                            "datasource_url", trigger.dataSourceUrl).record(timeElapsed.toJavaDuration())
                    }
                    report?.let {
                        updateService.updateMetaData()
                        sendRabbitMessages(listOf(it))
                        LOGGER.debug("completed harvest of datasource ${trigger.dataSourceId}, forced update: ${trigger.forceUpdate}")
                    }
                } catch (ex: Exception) {
                    LOGGER.error("harvest failure", ex)
                }
            }
        }
    }

    private fun sendRabbitMessages(reports: List<HarvestReport>) {
        publisher.send(reports)
        LOGGER.debug("Successfully sent harvest completed message")
    }
}
