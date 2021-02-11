package no.fdk.fdk_event_harvester.utils

import no.fdk.fdk_event_harvester.model.EventMeta
import no.fdk.fdk_event_harvester.model.TurtleDBO
import no.fdk.fdk_event_harvester.service.UNION_ID
import no.fdk.fdk_event_harvester.service.gzip
import org.bson.Document

private val responseReader = TestResponseReader()


val EVENT_META_0 = EventMeta(
    uri = "http://public-service-publisher.fellesdatakatalog.digdir.no/events/1",
    fdkId = EVENT_ID_0,
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis
)


val EVENT_TURTLE_0 = TurtleDBO(
    id = EVENT_ID_0,
    turtle = gzip(responseReader.readFile("event_0.ttl"))
)


val EVENT_META_1 = EventMeta(
    uri = "http://public-service-publisher.fellesdatakatalog.digdir.no/lifeevents/1",
    fdkId = EVENT_ID_1,
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis
)


val EVENT_TURTLE_1 = TurtleDBO(
    id = EVENT_ID_1,
    turtle = gzip(responseReader.readFile("event_1.ttl"))
)

val HARVESTED_DBO = TurtleDBO(
    id = "http://localhost:5000/fdk-public-service-publisher.ttl",
    turtle = gzip(responseReader.readFile("harvest_response_0.ttl"))
)

val UNION_DATA = TurtleDBO(
    id = UNION_ID,
    turtle = gzip(responseReader.readFile("all_events.ttl"))
)

fun turtlePopulation(): List<Document> =
    listOf(UNION_DATA, HARVESTED_DBO, EVENT_TURTLE_0, EVENT_TURTLE_1)
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
