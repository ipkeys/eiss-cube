package eiss.cube.vertx;

import com.google.inject.AbstractModule;
import io.vertx.core.Vertx;

public class VertxModule extends AbstractModule {

    protected void configure() {
        bind(Vertx.class).toProvider(VertxProvider.class);
    }

}
