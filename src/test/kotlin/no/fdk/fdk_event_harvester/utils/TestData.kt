package no.fdk.fdk_event_harvester.utils

import no.fdk.fdk_event_harvester.model.HarvestDataSource
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap
import java.util.*

const val LOCAL_SERVER_PORT = 5000

const val MONGO_USER = "testuser"
const val MONGO_PASSWORD = "testpassword"
const val MONGO_PORT = 27017
const val MONGO_COLLECTION = "eventHarvester"

val MONGO_ENV_VALUES: Map<String, String> = ImmutableMap.of(
    "MONGO_INITDB_ROOT_USERNAME", MONGO_USER,
    "MONGO_INITDB_ROOT_PASSWORD", MONGO_PASSWORD
)

const val EVENT_ID_0 = "fa7176b4-7743-3543-8c71-86c46e7f3654"
const val EVENT_ID_1 = "4a9dae51-52ba-3a8c-b3da-e55ccaa4639d"

val TEST_HARVEST_DATE: Calendar = Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).setDate(2020, 9, 5).setTimeOfDay(13, 15, 39, 831).build()
val NEW_TEST_HARVEST_DATE: Calendar = Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).setDate(2020, 9, 15).setTimeOfDay(11, 52, 16, 122).build()

val TEST_HARVEST_SOURCE = HarvestDataSource(
    id = "test-source",
    url = "http://localhost:5000/fdk-public-service-publisher.ttl",
    acceptHeaderValue = "text/turtle",
    dataType = "publicService",
    dataSourceType = "CPSV-AP-NO"
)
