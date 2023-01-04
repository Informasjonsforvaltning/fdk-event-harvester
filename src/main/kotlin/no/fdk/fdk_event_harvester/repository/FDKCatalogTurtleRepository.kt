package no.fdk.fdk_event_harvester.repository

import no.fdk.fdk_event_harvester.model.FDKCatalogTurtle
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface FDKCatalogTurtleRepository : MongoRepository<FDKCatalogTurtle, String>
