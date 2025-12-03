package no.fdk.fdk_event_harvester.service

import no.fdk.fdk_event_harvester.model.DuplicateIRI
import no.fdk.fdk_event_harvester.model.EventMeta
import no.fdk.fdk_event_harvester.model.FdkIdAndUri
import no.fdk.fdk_event_harvester.model.HarvestReport
import no.fdk.fdk_event_harvester.rabbit.RabbitMQPublisher
import no.fdk.fdk_event_harvester.repository.EventMetaRepository
import no.fdk.fdk_event_harvester.utils.*
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.web.server.ResponseStatusException
import java.util.*
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("unit")
class EventServiceTest {
    private val repository: EventMetaRepository = mock()
    private val publisher: RabbitMQPublisher = mock()
    private val turtleService: TurtleService = mock()
    private val service = EventService(repository, publisher, turtleService)

    private val responseReader = TestResponseReader()

    @Nested
    internal inner class EventById {

        @Test
        fun responseIsNullWhenNoModelIsFound() {
            whenever(turtleService.getEvent("123", true))
                .thenReturn(null)

            val response = service.getEventById("123", Lang.TURTLE, true)

            assertNull(response)
        }

        @Test
        fun responseIsIsomorphicWithExpectedModel() {
            whenever(turtleService.getEvent(EVENT_ID_0, true))
                .thenReturn(javaClass.classLoader.getResource("event_0.ttl")!!.readText())
            whenever(turtleService.getEvent(EVENT_ID_0, false))
                .thenReturn(javaClass.classLoader.getResource("no_records_event_0.ttl")!!.readText())

            val responseTurtle = service.getEventById(EVENT_ID_0, Lang.TURTLE, true)
            val responseRDFXML = service.getEventById(EVENT_ID_0, Lang.RDFXML, false)

            val expected = responseReader.parseFile("event_0.ttl", "TURTLE")
            val expectedNoRecords = responseReader.parseFile("no_records_event_0.ttl", "TURTLE")

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle!!, "TURTLE")))
            assertTrue(expectedNoRecords.isIsomorphicWith(responseReader.parseResponse(responseRDFXML!!, "RDF/XML")))
        }

    }

    @Nested
    internal inner class RemoveEventById {

        @Test
        fun throwsResponseStatusExceptionWhenNoMetaFoundInDB() {
            whenever(repository.findAllByFdkId("123"))
                .thenReturn(emptyList())

            assertThrows<ResponseStatusException> { service.removeEvent("123") }
        }

        @Test
        fun throwsResponseStatusExceptionNoNonRemovedMetaFoundInDB() {
            whenever(repository.findAllByFdkId(EVENT_ID_0))
                .thenReturn(listOf(EVENT_META_0.copy(removed = true)))

            assertThrows<ResponseStatusException> { service.removeEvent(EVENT_ID_0) }
        }

        @Test
        fun updatesMetaAndSendsRabbitReportWhenMetaIsFound() {
            whenever(repository.findAllByFdkId(EVENT_ID_0))
                .thenReturn(listOf(EVENT_META_0))

            service.removeEvent(EVENT_META_0.fdkId)

            argumentCaptor<List<EventMeta>>().apply {
                verify(repository, times(1)).saveAll(capture())
                assertEquals(listOf(EVENT_META_0.copy(removed = true)), firstValue)
            }

            val expectedReport = HarvestReport(
                id = "manual-delete-$EVENT_ID_0",
                url = EVENT_META_0.uri,
                harvestError = false,
                startTime = "startTime",
                endTime = "endTime",
                removedResources = listOf(FdkIdAndUri(EVENT_META_0.fdkId, EVENT_META_0.uri))
            )
            argumentCaptor<List<HarvestReport>>().apply {
                verify(publisher, times(1)).send(capture())

                assertEquals(
                    listOf(expectedReport.copy(
                        startTime = firstValue.first().startTime,
                        endTime = firstValue.first().endTime
                    )),
                    firstValue
                )
            }
        }

    }

    @Nested
    internal inner class RemoveDuplicates {

        @Test
        fun throwsExceptionWhenRemoveIRINotFoundInDB() {
            whenever(repository.findById("https://123.no"))
                .thenReturn(Optional.empty())
            whenever(repository.findById(EVENT_META_1.uri))
                .thenReturn(Optional.of(EVENT_META_1))

            val duplicateIRI = DuplicateIRI(
                iriToRemove = "https://123.no",
                iriToRetain = EVENT_META_1.uri
            )
            assertThrows<ResponseStatusException> { service.removeDuplicates(listOf(duplicateIRI)) }
        }

        @Test
        fun createsNewMetaWhenRetainIRINotFoundInDB() {
            whenever(repository.findById(EVENT_META_0.uri))
                .thenReturn(Optional.of(EVENT_META_0))
            whenever(repository.findById(EVENT_META_1.uri))
                .thenReturn(Optional.empty())

            val duplicateIRI = DuplicateIRI(
                iriToRemove = EVENT_META_0.uri,
                iriToRetain = EVENT_META_1.uri
            )
            service.removeDuplicates(listOf(duplicateIRI))

            argumentCaptor<List<EventMeta>>().apply {
                verify(repository, times(1)).saveAll(capture())
                assertEquals(listOf(EVENT_META_0.copy(removed = true), EVENT_META_0.copy(uri = EVENT_META_1.uri)), firstValue)
            }

            verify(publisher, times(0)).send(any())
        }

        @Test
        fun sendsRabbitReportWithRetainFdkIdWhenKeepingRemoveFdkId() {
            whenever(repository.findById(EVENT_META_0.uri))
                .thenReturn(Optional.of(EVENT_META_0))
            whenever(repository.findById(EVENT_META_1.uri))
                .thenReturn(Optional.of(EVENT_META_1))

            val duplicateIRI = DuplicateIRI(
                iriToRemove = EVENT_META_0.uri,
                iriToRetain = EVENT_META_1.uri
            )
            service.removeDuplicates(listOf(duplicateIRI))

            argumentCaptor<List<EventMeta>>().apply {
                verify(repository, times(1)).saveAll(capture())
                assertEquals(listOf(
                    EVENT_META_0.copy(removed = true),
                    EVENT_META_0.copy(uri = EVENT_META_1.uri, isPartOf = EVENT_META_1.isPartOf)
                ), firstValue)
            }

            val expectedReport = HarvestReport(
                id = "duplicate-delete",
                url = "https://fellesdatakatalog.digdir.no/duplicates",
                harvestError = false,
                startTime = "startTime",
                endTime = "endTime",
                removedResources = listOf(FdkIdAndUri(EVENT_META_1.fdkId, EVENT_META_1.uri))
            )
            argumentCaptor<List<HarvestReport>>().apply {
                verify(publisher, times(1)).send(capture())

                assertEquals(
                    listOf(expectedReport.copy(
                        startTime = firstValue.first().startTime,
                        endTime = firstValue.first().endTime
                    )),
                    firstValue
                )
            }
        }

        @Test
        fun sendsRabbitReportWithRemoveFdkIdWhenNotKeepingRemoveFdkId() {
            whenever(repository.findById(EVENT_META_0.uri))
                .thenReturn(Optional.of(EVENT_META_0))
            whenever(repository.findById(EVENT_META_1.uri))
                .thenReturn(Optional.of(EVENT_META_1))

            val duplicateIRI = DuplicateIRI(
                iriToRemove = EVENT_META_1.uri,
                iriToRetain = EVENT_META_0.uri,
                keepRemovedFdkId = false
            )
            service.removeDuplicates(listOf(duplicateIRI))

            argumentCaptor<List<EventMeta>>().apply {
                verify(repository, times(1)).saveAll(capture())
                assertEquals(listOf(
                    EVENT_META_1.copy(removed = true),
                    EVENT_META_0
                ), firstValue)
            }

            val expectedReport = HarvestReport(
                id = "duplicate-delete",
                url = "https://fellesdatakatalog.digdir.no/duplicates",
                harvestError = false,
                startTime = "startTime",
                endTime = "endTime",
                removedResources = listOf(FdkIdAndUri(EVENT_META_1.fdkId, EVENT_META_1.uri))
            )
            argumentCaptor<List<HarvestReport>>().apply {
                verify(publisher, times(1)).send(capture())

                assertEquals(
                    listOf(expectedReport.copy(
                        startTime = firstValue.first().startTime,
                        endTime = firstValue.first().endTime
                    )),
                    firstValue
                )
            }
        }

        @Test
        fun throwsExceptionWhenTryingToReportAlreadyRemovedAsRemoved() {
            whenever(repository.findById(EVENT_META_0.uri))
                .thenReturn(Optional.of(EVENT_META_0.copy(removed = true)))
            whenever(repository.findById(EVENT_META_1.uri))
                .thenReturn(Optional.of(EVENT_META_1))

            val duplicateIRI = DuplicateIRI(
                iriToRemove = EVENT_META_0.uri,
                iriToRetain = EVENT_META_1.uri,
                keepRemovedFdkId = false
            )

            assertThrows<ResponseStatusException> { service.removeDuplicates(listOf(duplicateIRI)) }

            whenever(repository.findById(EVENT_META_0.uri))
                .thenReturn(Optional.of(EVENT_META_0))
            whenever(repository.findById(EVENT_META_1.uri))
                .thenReturn(Optional.of(EVENT_META_1.copy(removed = true)))

            assertThrows<ResponseStatusException> { service.removeDuplicates(listOf(duplicateIRI.copy(keepRemovedFdkId = true))) }
        }

    }

}
