package no.fdk.fdk_event_harvester.rdf

import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Property

class CPSV {

    companion object {
        private val m = ModelFactory.createDefaultModel()
        const val uri = "http://purl.org/vocab/cpsv#"
        val PublicService: Property = m.createProperty("${uri}PublicService")
    }

}

class CPSVNO {

    companion object {
        private val m = ModelFactory.createDefaultModel()
        const val uri = "https://data.norge.no/vocabulary/cpsvno#"
        val Service: Property = m.createProperty("${uri}Service")
    }

}

class CV {

    companion object {
        private val m = ModelFactory.createDefaultModel()
        const val uri = "http://data.europa.eu/m8g/"
        val Event: Property = m.createProperty("${uri}Event")
        val BusinessEvent: Property = m.createProperty("${uri}BusinessEvent")
        val LifeEvent: Property = m.createProperty("${uri}LifeEvent")
    }

}
