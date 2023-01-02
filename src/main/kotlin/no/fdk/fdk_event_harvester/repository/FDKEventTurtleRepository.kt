package no.fdk.fdk_event_harvester.repository

import no.fdk.fdk_event_harvester.model.FDKEventTurtle
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface FDKEventTurtleRepository : MongoRepository<FDKEventTurtle, String>
