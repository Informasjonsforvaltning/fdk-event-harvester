package no.fdk.fdk_event_harvester.utils

import org.slf4j.LoggerFactory
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

abstract class ApiTestContext {

    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            mongoContainer?.let {
                TestPropertyValues.of(
                    "spring.data.mongodb.port=${it.getMappedPort(MONGO_PORT)}"
                ).applyTo(configurableApplicationContext.environment)
            }
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ApiTestContext::class.java)
        var mongoContainer: KGenericContainer? = null

        init {

            startMockServer()

            try {
                mongoContainer = KGenericContainer("mongo:latest")
                    .withEnv(MONGO_ENV_VALUES)
                    .withExposedPorts(MONGO_PORT)
                    .waitingFor(Wait.forListeningPort())

                mongoContainer!!.start()

                populateDB()
            } catch (e: Exception) {
                logger.warn("Failed to start MongoDB container: ${e.message}. Some tests may not work correctly.", e)
                // Continue without MongoDB - some tests may still work with just the mock server
            }

            try {
                val con = URL("http://localhost:5050/ping").openConnection() as HttpURLConnection
                con.connect()
                if (con.responseCode != 200) {
                    logger.debug("Ping to mock server failed")
                    stopMockServer()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }
    }

}

// Hack needed because testcontainers use of generics confuses Kotlin
class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)
