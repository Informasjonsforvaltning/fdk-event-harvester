package no.fdk.fdk_event_harvester.rdf

import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Property

class CPSV {

    companion object {
        private val m = ModelFactory.createDefaultModel()
        val uri = "http://purl.org/vocab/cpsv#"
        val PublicService: Property = m.createProperty( "${uri}PublicService")
    }

}

class CV {

    companion object {
        private val m = ModelFactory.createDefaultModel()
        val uri = "http://data.europa.eu/m8g/"
        val BusinessEvent: Property = m.createProperty( "${uri}BusinessEvent")
        val LifeEvent: Property = m.createProperty( "${uri}LifeEvent")
    }

}
