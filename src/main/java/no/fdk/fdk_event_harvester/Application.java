package no.fdk.fdk_event_harvester;

import no.fdk.fdk_event_harvester.configuration.ApplicationProperties;
import no.fdk.fdk_event_harvester.configuration.FusekiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@SpringBootApplication
@EnableConfigurationProperties({ApplicationProperties.class, FusekiProperties.class})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}