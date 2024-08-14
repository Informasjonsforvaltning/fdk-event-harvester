package no.fdk.fdk_event_harvester.repository

import no.fdk.fdk_event_harvester.model.EventMeta
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface EventMetaRepository : MongoRepository<EventMeta, String> {
    fun findAllByIsPartOf(isPartOf: String): List<EventMeta>
    fun findAllByFdkId(fdkId: String): List<EventMeta>
}
