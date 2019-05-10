package eiss.cube.service.client;

import dev.morphia.Datastore;
import dev.morphia.query.Query;
import eiss.client.EISSClient;
import eiss.client.api.VenConfiguration;
import eiss.models.cubes.EISScube;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toList;

@Slf4j
public class ClientVenConfigurationHandler implements VenConfiguration {

    private Datastore datastore;
    private String ven;

    @Inject
    public ClientVenConfigurationHandler(Datastore datastore) {
        this.datastore = datastore;
        Properties properties = EISSClient.readProperties();
        this.ven = properties.getProperty("venId", "");
    }

    @Override
    public List<String> getMeters() {
        // dummy meters for now
        List<String> meters = Stream.of("meter1").collect(toList());
        return meters;
    }

    @Override
    public List<String> getPrograms() {
        return new ArrayList<>();
    }

    @Override
    public List<String> getResources() {
        List<String> resources = new ArrayList<>();

        Query<EISScube> q = datastore.createQuery(EISScube.class);
        q.criteria("settings.VEN").equal(ven);
        q.project("name", TRUE);

        List<EISScube> cubes = q.asList();
        if (cubes != null) {
            resources = cubes.stream().map(EISScube::getName).collect(toList());
        }

        return resources;
    }

}
