package no.fdk.fdk_event_harvester.utils

import no.fdk.fdk_event_harvester.model.EventMeta
import no.fdk.fdk_event_harvester.model.EventTurtle
import no.fdk.fdk_event_harvester.model.FDKEventTurtle
import no.fdk.fdk_event_harvester.model.HarvestSourceTurtle
import no.fdk.fdk_event_harvester.model.TurtleDBO
import no.fdk.fdk_event_harvester.service.UNION_ID
import no.fdk.fdk_event_harvester.service.gzip
import org.bson.Document

private val responseReader = TestResponseReader()


val EVENT_META_0 = EventMeta(
    uri = "http://testdirektoratet.no/events/0",
    fdkId = EVENT_ID_0,
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

val EVENT_META_2 = EventMeta(
    uri = "http://testdirektoratet.no/events/2",
    fdkId = EVENT_ID_2,
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

val HARVESTED_DBO = HarvestSourceTurtle(
    id = "http://localhost:5000/fdk-public-service-publisher.ttl",
    turtle = gzip(responseReader.readFile("harvest_response_0.ttl"))
)

val UNION_DATA = FDKEventTurtle(
    id = UNION_ID,
    turtle = gzip(responseReader.readFile("all_events.ttl"))
)

val UNION_DATA_NO_RECORDS = EventTurtle(
    id = UNION_ID,
    turtle = gzip(responseReader.readFile("no_records_all_events.ttl"))
)

fun harvestSourceTurtlePopulation(): List<Document> =
    listOf(HARVESTED_DBO).map { it.mapDBO() }

fun fdkEventTurtlePopulation(): List<Document> =
    listOf(UNION_DATA, EVENT_TURTLE_0, EVENT_TURTLE_1, EVENT_TURTLE_2)
        .map { it.mapDBO() }

fun eventTurtlePopulation(): List<Document> =
    listOf(EVENT_TURTLE_0_NO_RECORDS, EVENT_TURTLE_1_NO_RECORDS, UNION_DATA_NO_RECORDS, EVENT_TURTLE_2_NO_RECORDS)
        .map { it.mapDBO() }

fun eventMetaPopulation(): List<Document> =
    listOf(EVENT_META_0, EVENT_META_1, EVENT_META_2)
        .map { it.mapDBO() }

private fun EventMeta.mapDBO(): Document =
    Document()
        .append("_id", uri)
        .append("fdkId", fdkId)
        .append("issued", issued)
        .append("modified", modified)

private fun TurtleDBO.mapDBO(): Document =
    Document()
        .append("_id", id)
        .append("turtle", turtle)
