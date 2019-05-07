package eiss.cube.service.client;

import eiss.client.api.VenConfiguration;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Slf4j
public class ClientVenConfigurationHandler implements VenConfiguration {

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
        // dummy resources for now
        List<String> resources = Stream.of("device1").collect(toList());

        return resources;
    }

}
