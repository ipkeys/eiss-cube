package eiss.cube.service.client;

import eiss.client.EISSClient;
import io.vertx.core.AbstractVerticle;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Properties;

@Slf4j
public class VEN extends AbstractVerticle {

    private EISSClient client;

    @Inject
    public VEN(EISSClient client) {
        this.client = client;
    }

    @Override
    public void start() throws Exception {
        Properties properties = EISSClient.readProperties();
        if (!client.initialize(properties)) {
            log.error("Failed to start VEN: {}", client.getErrorMessage());
        } else {
            log.info("Start VEN");
            client.start();
        }
    }

    @Override
    public void stop() throws Exception {
        log.info("Stop VEN");
        client.stop();
    }

}
