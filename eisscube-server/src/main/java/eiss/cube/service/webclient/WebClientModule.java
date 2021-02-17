package eiss.cube.service.webclient;

import com.google.inject.AbstractModule;
import io.vertx.ext.web.client.WebClient;

public class WebClientModule extends AbstractModule {

    protected void configure() {
        bind(WebClient.class).toProvider(WebClientProvider.class);
    }

}
