package no.fdk.fdk_event_harvester.utils

import no.fdk.fdk_event_harvester.model.HarvestDataSource
import no.fdk.fdk_event_harvester.model.Organization
import no.fdk.fdk_event_harvester.model.PrefLabel
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

const val CATALOG_ID_0 = "4d2c9e29-2f9a-304f-9e48-34e30a36d068"
const val CATALOG_ID_1 = "b7c6d34c-624d-3c72-9e30-2b608e433ad7"

const val EVENT_ID_0 = "cbed84c4-a719-3370-b216-725bfc79978d"
const val EVENT_ID_1 = "99b00c6c-4087-3c23-9244-6e85b9d02adc"
const val EVENT_ID_2 = "fb77d4f2-a11c-33e3-8c49-772c4569613b"
const val EVENT_ID_3 = "776fb75b-165f-3aa9-81bc-f322fa855ed7"
const val EVENT_ID_4 = "df191147-039c-3919-a180-acc909e55e47"

val TEST_HARVEST_DATE: Calendar = Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).setDate(2020, 9, 5).setTimeOfDay(13, 15, 39, 831).build()
val NEW_TEST_HARVEST_DATE: Calendar = Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).setDate(2020, 9, 15).setTimeOfDay(11, 52, 16, 122).build()

val TEST_HARVEST_SOURCE = HarvestDataSource(
    id = "test-source",
    url = "http://localhost:5000/fdk-public-service-publisher.ttl",
    acceptHeaderValue = "text/turtle",
    dataType = "publicService",
    dataSourceType = "CPSV-AP-NO",
    publisherId = "123456789"
)

val ORGANIZATION_0 = Organization(
    organizationId = "123456789",
    uri = "http://localhost:5000/organizations/123456789",
    name = "TESTDIREKTORATET",
    prefLabel = PrefLabel(nb = "Testdirektoratet")
)
