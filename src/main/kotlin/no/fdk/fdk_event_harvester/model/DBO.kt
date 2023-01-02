package no.fdk.fdk_event_harvester.model

import no.fdk.fdk_event_harvester.rdf.parseRDFResponse
import no.fdk.fdk_event_harvester.service.ungzip
import org.apache.jena.riot.Lang
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

@Document(collection = "harvestSourceTurtle")
data class HarvestSourceTurtle(
    @Id override val id: String,
    override val turtle: String
) : TurtleDBO()

@Document(collection = "eventTurtle")
data class EventTurtle(
    @Id override val id: String,
    override val turtle: String
) : TurtleDBO()

@Document(collection = "fdkEventTurtle")
data class FDKEventTurtle(
    @Id override val id: String,
    override val turtle: String
) : TurtleDBO()

abstract class TurtleDBO {
    abstract val id: String
    abstract val turtle: String
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TurtleDBO

        return when {
            id != other.id -> false
            else -> zippedModelsAreIsomorphic(turtle, other.turtle)
        }
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + turtle.hashCode()
        return result
    }
}

private fun zippedModelsAreIsomorphic(zip0: String, zip1: String): Boolean {
    val model0 = try {
        parseRDFResponse(ungzip(zip0), Lang.TURTLE, null)
    } catch (ex: Exception) { null }
    val model1 = try {
        parseRDFResponse(ungzip(zip1), Lang.TURTLE, null)
    } catch (ex: Exception) { null }

    return when {
        model0 != null && model1 != null -> model0.isIsomorphicWith(model1)
        model0 == null && model1 == null -> true
        else -> false
    }
}
