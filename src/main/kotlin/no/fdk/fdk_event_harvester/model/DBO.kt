package no.fdk.fdk_event_harvester.model

import no.fdk.fdk_event_harvester.rdf.JenaType
import no.fdk.fdk_event_harvester.rdf.parseRDFResponse
import no.fdk.fdk_event_harvester.service.ungzip
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "eventMeta")
data class EventMeta (
    @Id
    val uri: String,

    @Indexed(unique = true)
    val fdkId: String,

    val issued: Long,
    val modified: Long
)

@Document(collection = "turtle")
data class TurtleDBO (
        @Id
        val id: String,
        val turtle: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TurtleDBO

        return when {
            id != other.id -> false
            else -> zippedModelsAreIsometric(turtle, other.turtle)
        }
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + turtle.hashCode()
        return result
    }
}

private fun zippedModelsAreIsometric(zip0: String, zip1: String): Boolean {
    val model0 = parseRDFResponse(ungzip(zip0), JenaType.TURTLE, null)
    val model1 = parseRDFResponse(ungzip(zip1), JenaType.TURTLE, null)

    return when {
        model0 != null && model1 != null -> model0.isIsomorphicWith(model1)
        model0 == null && model1 == null -> true
        else -> false
    }
}
