# FDK Event Harvester

This application provides an API for events harvested from different sources.

For a broader understanding of the system’s context, refer to
the [architecture documentation](https://github.com/Informasjonsforvaltning/architecture-documentation) wiki. For more
specific context on this application, see the **Harvesting** subsystem section.

The harvest process is triggered by messages from RabbitMQ with the routing key `publicservice.*.HarvestTrigger`, a
message will call the method `initiateHarvest` in the class `HarvesterActivity`. The actual harvest will start when
`activitySemaphore` has an available permit, when there are no available permits all messages will be queued by the
semaphore.

The body of the trigger message has 3 relevant parameters:

- `dataSourceId` - Triggers the harvest of a specific source from fdk-harvest-admin
- `publisherId` - Triggers the harvest of all sources for the specified organization number.
- `forceUpdate` - Indicates that the harvest should be performed, even when no changes are detected in the source

A triggered harvest will download all relevant sources from fdk-harvest-admin, download everything from the source and
try to read it as a RDF graph via a jena Model. If the source is successfully parsed as a jena Model it will be compared
to the last harvest of the same source. The harvest process will continue if the source is not isomorphic to the last
harvest or `forceUpdate` is true.

The actual harvest process will first find events, resources with the type `cv:Event`, `cv:BusinessEvent` or
`cv:LifeEvent`, and catalogs, resources with the type `dcat:Catalog`, blank node events and catalogs will be ignored. A
catalog will also be generated from data about the source from fdk-harvest-admin, any events in the source that is not
part of any catalog, indicated by the predicate `dcatno:containsEvent`, will be added to this catalog.
When all catalogs and events have been found a recursive function will create a graph with every contained triple for
all catalogs and events.

The process will save metadata for both events and catalogs:

- `uri` - The IRI for the resource, is used as the database id
- `fdkId` - The UUID used for the resource used in the context of FDK, is a generated hash of the uri if nothing else is
  set.
- `isPartOf` - Only relevant for events, is the uri of the catalog it belongs to.
- `removed` - Only relevant for events, is set to true if the event has been removed from the source.
- `issued` - The timestamp of the first time the resource was harvested
- `modified` - The timestamp of the last time a harvest of the resource found changes in the resource graph

All blank nodes will be [skolemized](https://www.w3.org/wiki/BnodeSkolemization) in the resource graphs.

When all sources from the trigger has been processed a new rabbit message will be published with the routing key
`events.harvested`, the message body will be a list of harvest reports, one report for each source from
fdk-harvest-admin.

When the rabbit message has been published the semaphore permit is released and a new harvest trigger can be processed.

## Getting Started

These instructions will give you a copy of the project up and running on your local machine for development and testing
purposes.

### Prerequisites

Ensure you have the following installed:

- Java 17
- Maven
- Docker

### Running locally

#### Clone the repository

```sh
git clone https://github.com/Informasjonsforvaltning/fdk-event-harvester.git
cd fdk-event-harvester
```

Start MongoDB, RabbitMQ and the application (either through your IDE using the dev profile, or via CLI):

```sh
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=develop
```

### API Documentation (OpenAPI)

The API documentation is available at ```resources/specification```.

### Running tests

```sh
mvn verify
```
