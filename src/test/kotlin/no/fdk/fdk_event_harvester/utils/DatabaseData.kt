package no.fdk.fdk_event_harvester.utils

import no.fdk.fdk_event_harvester.model.EventMeta
import no.fdk.fdk_event_harvester.model.TurtleDBO
import no.fdk.fdk_event_harvester.service.UNION_ID
import no.fdk.fdk_event_harvester.service.gzip
import no.fdk.fdk_event_harvester.service.turtleId
import org.bson.Document

private val responseReader = TestResponseReader()


val EVENT_META_0 = EventMeta(
    uri = "http://public-service-publisher.fellesdatakatalog.digdir.no/events/1",
    fdkId = EVENT_ID_0,
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis
)


val EVENT_TURTLE_0 = TurtleDBO(
    id = turtleId(EVENT_ID_0, true),
    turtle = gzip(responseReader.readFile("event_0.ttl"))
)


val EVENT_TURTLE_0_NO_RECORDS = TurtleDBO(
    id = turtleId(EVENT_ID_0, false),
    turtle = gzip(responseReader.readFile("no_records_event_0.ttl"))
)


val EVENT_META_1 = EventMeta(
    uri = "http://public-service-publisher.fellesdatakatalog.digdir.no/lifeevents/1",
    fdkId = EVENT_ID_1,
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis
)

val EVENT_TURTLE_1 = TurtleDBO(
    id = turtleId(EVENT_ID_1, true),
    turtle = gzip(responseReader.readFile("event_1.ttl"))
)

val EVENT_TURTLE_1_NO_RECORDS = TurtleDBO(
    id = turtleId(EVENT_ID_1, false),
    turtle = gzip(responseReader.readFile("no_records_event_1.ttl"))
)

val HARVESTED_DBO = TurtleDBO(
    id = "http://localhost:5000/fdk-public-service-publisher.ttl",
    turtle = gzip(responseReader.readFile("harvest_response_0.ttl"))
)

val UNION_DATA = TurtleDBO(
    id = turtleId(UNION_ID, true),
    turtle = gzip(responseReader.readFile("all_events.ttl"))
)

val UNION_DATA_NO_RECORDS = TurtleDBO(
    id = turtleId(UNION_ID, false),
    turtle = gzip(responseReader.readFile("no_records_all_events.ttl"))
)

fun turtlePopulation(): List<Document> =
    listOf(
        UNION_DATA, HARVESTED_DBO, EVENT_TURTLE_0, EVENT_TURTLE_1,
        EVENT_TURTLE_0_NO_RECORDS, EVENT_TURTLE_1_NO_RECORDS, UNION_DATA_NO_RECORDS
    )
        .map { it.mapDBO() }

fun eventPopulation(): List<Document> =
    listOf(EVENT_META_0, EVENT_META_1)
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
