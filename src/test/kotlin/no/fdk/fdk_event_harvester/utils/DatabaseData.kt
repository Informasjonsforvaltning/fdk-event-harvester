package no.fdk.fdk_event_harvester.utils

import no.fdk.fdk_event_harvester.model.CatalogMeta
import no.fdk.fdk_event_harvester.model.CatalogTurtle
import no.fdk.fdk_event_harvester.model.EventMeta
import no.fdk.fdk_event_harvester.model.EventTurtle
import no.fdk.fdk_event_harvester.model.FDKCatalogTurtle
import no.fdk.fdk_event_harvester.model.FDKEventTurtle
import no.fdk.fdk_event_harvester.model.HarvestSourceTurtle
import no.fdk.fdk_event_harvester.model.TurtleDBO
import no.fdk.fdk_event_harvester.service.UNION_ID
import no.fdk.fdk_event_harvester.service.gzip
import org.bson.Document

private val responseReader = TestResponseReader()


val CATALOG_META_0 = CatalogMeta(
    uri="http://localhost:5050/fdk-public-service-publisher.ttl#GeneratedCatalog",
    fdkId=CATALOG_ID_0,
    events=setOf("http://testdirektoratet.no/events/0", "http://testdirektoratet.no/events/2", "http://testdirektoratet.no/events/1"),
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis
)

val CATALOG_TURTLE_0 = FDKCatalogTurtle(
    id = CATALOG_ID_0,
    turtle = gzip(responseReader.readFile("catalog_0.ttl"))
)

val CATALOG_TURTLE_0_NO_RECORDS = CatalogTurtle(
    id = CATALOG_ID_0,
    turtle = gzip(responseReader.readFile("no_records_catalog_0.ttl"))
)

val CATALOG_META_1 = CatalogMeta(
    uri="http://test.no/catalogs/0",
    fdkId=CATALOG_ID_1,
    events=setOf("http://test.no/events/1", "http://test.no/events/0"),
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis
)

val CATALOG_TURTLE_1 = FDKCatalogTurtle(
    id = CATALOG_ID_1,
    turtle = gzip(responseReader.readFile("catalog_1.ttl"))
)

val CATALOG_TURTLE_1_NO_RECORDS = CatalogTurtle(
    id = CATALOG_ID_1,
    turtle = gzip(responseReader.readFile("no_records_catalog_1.ttl"))
)

val EVENT_META_0 = EventMeta(
    uri = "http://testdirektoratet.no/events/0",
    fdkId = EVENT_ID_0,
    isPartOf = "http://localhost:5050/events/catalogs/$CATALOG_ID_0",
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis
)


val EVENT_TURTLE_0 = FDKEventTurtle(
    id = EVENT_ID_0,
    turtle = gzip(responseReader.readFile("event_0.ttl"))
)


val EVENT_TURTLE_0_NO_RECORDS = EventTurtle(
    id = EVENT_ID_0,
    turtle = gzip(responseReader.readFile("no_records_event_0.ttl"))
)


val EVENT_META_1 = EventMeta(
    uri = "http://testdirektoratet.no/events/1",
    fdkId = EVENT_ID_1,
    isPartOf = "http://localhost:5050/events/catalogs/$CATALOG_ID_0",
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis
)

val EVENT_TURTLE_1 = FDKEventTurtle(
    id = EVENT_ID_1,
    turtle = gzip(responseReader.readFile("event_1.ttl"))
)

val EVENT_TURTLE_1_NO_RECORDS = EventTurtle(
    id = EVENT_ID_1,
    turtle = gzip(responseReader.readFile("no_records_event_1.ttl"))
)

val REMOVED_EVENT_META = EventMeta(
    uri = "http://testdirektoratet.no/events/removed",
    fdkId = "removed",
    isPartOf = "http://localhost:5050/events/catalogs/$CATALOG_ID_0",
    removed = true,
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis
)

val REMOVED_EVENT_TURTLE = FDKEventTurtle(
    id = "removed",
    turtle = gzip(responseReader.readFile("event_1.ttl"))
)

val REMOVED_EVENT_TURTLE_NO_RECORDS = EventTurtle(
    id = "removed",
    turtle = gzip(responseReader.readFile("no_records_event_1.ttl"))
)

val EVENT_META_2 = EventMeta(
    uri = "http://testdirektoratet.no/events/2",
    fdkId = EVENT_ID_2,
    isPartOf = "http://localhost:5050/events/catalogs/$CATALOG_ID_0",
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis
)


val EVENT_TURTLE_2 = FDKEventTurtle(
    id = EVENT_ID_2,
    turtle = gzip(responseReader.readFile("event_2.ttl"))
)


val EVENT_TURTLE_2_NO_RECORDS = EventTurtle(
    id = EVENT_ID_2,
    turtle = gzip(responseReader.readFile("no_records_event_2.ttl"))
)

val EVENT_META_3 = EventMeta(
    uri = "http://test.no/events/0",
    fdkId = EVENT_ID_3,
    isPartOf = "http://localhost:5050/events/catalogs/$CATALOG_ID_1",
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis
)


val EVENT_TURTLE_3 = FDKEventTurtle(
    id = EVENT_ID_3,
    turtle = gzip(responseReader.readFile("event_3.ttl"))
)


val EVENT_TURTLE_3_NO_RECORDS = EventTurtle(
    id = EVENT_ID_3,
    turtle = gzip(responseReader.readFile("no_records_event_3.ttl"))
)

val EVENT_META_4 = EventMeta(
    uri = "http://test.no/events/1",
    fdkId = EVENT_ID_4,
    isPartOf = "http://localhost:5050/events/catalogs/$CATALOG_ID_1",
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis
)


val EVENT_TURTLE_4 = FDKEventTurtle(
    id = EVENT_ID_4,
    turtle = gzip(responseReader.readFile("event_4.ttl"))
)


val EVENT_TURTLE_4_NO_RECORDS = EventTurtle(
    id = EVENT_ID_4,
    turtle = gzip(responseReader.readFile("no_records_event_4.ttl"))
)

val HARVESTED_DBO = HarvestSourceTurtle(
    id = "http://localhost:5050/fdk-public-service-publisher.ttl",
    turtle = gzip(responseReader.readFile("harvest_response_0.ttl"))
)

val EVENT_UNION_DATA = FDKEventTurtle(
    id = UNION_ID,
    turtle = gzip(responseReader.readFile("all_catalogs.ttl"))
)

val EVENT_UNION_DATA_NO_RECORDS = EventTurtle(
    id = UNION_ID,
    turtle = gzip(responseReader.readFile("no_records_all_events.ttl"))
)

val CATALOG_UNION = FDKCatalogTurtle(
    id = UNION_ID,
    turtle = gzip(responseReader.readFile("all_catalogs.ttl"))
)

val CATALOG_UNION_NO_RECORDS = CatalogTurtle(
    id = UNION_ID,
    turtle = gzip(responseReader.readFile("no_records_all_catalogs.ttl"))
)

fun harvestSourceTurtlePopulation(): List<Document> =
    listOf(HARVESTED_DBO).map { it.mapDBO() }

fun fdkEventTurtlePopulation(): List<Document> =
    listOf(EVENT_UNION_DATA, EVENT_TURTLE_0, EVENT_TURTLE_1, EVENT_TURTLE_2, EVENT_TURTLE_3, EVENT_TURTLE_4, REMOVED_EVENT_TURTLE)
        .map { it.mapDBO() }

fun eventTurtlePopulation(): List<Document> =
    listOf(EVENT_TURTLE_0_NO_RECORDS, EVENT_TURTLE_1_NO_RECORDS, EVENT_UNION_DATA_NO_RECORDS,
        EVENT_TURTLE_2_NO_RECORDS, EVENT_TURTLE_3_NO_RECORDS, EVENT_TURTLE_4_NO_RECORDS, REMOVED_EVENT_TURTLE_NO_RECORDS)
        .map { it.mapDBO() }

fun eventMetaPopulation(): List<Document> =
    listOf(EVENT_META_0, EVENT_META_1, EVENT_META_2, EVENT_META_3, EVENT_META_4, REMOVED_EVENT_META)
        .map { it.mapDBO() }

fun fdkCatalogTurtlePopulation(): List<Document> =
    listOf(CATALOG_UNION, CATALOG_TURTLE_0, CATALOG_TURTLE_1)
        .map { it.mapDBO() }

fun catalogTurtlePopulation(): List<Document> =
    listOf(CATALOG_UNION_NO_RECORDS, CATALOG_TURTLE_0_NO_RECORDS, CATALOG_TURTLE_1_NO_RECORDS)
        .map { it.mapDBO() }

fun catalogMetaPopulation(): List<Document> =
    listOf(CATALOG_META_0, CATALOG_META_1)
        .map { it.mapDBO() }

private fun CatalogMeta.mapDBO(): Document =
    Document()
        .append("_id", uri)
        .append("fdkId", fdkId)
        .append("events", events)
        .append("issued", issued)
        .append("modified", modified)

private fun EventMeta.mapDBO(): Document =
    Document()
        .append("_id", uri)
        .append("fdkId", fdkId)
        .append("isPartOf", isPartOf)
        .append("removed", removed)
        .append("issued", issued)
        .append("modified", modified)

private fun TurtleDBO.mapDBO(): Document =
    Document()
        .append("_id", id)
        .append("turtle", turtle)
