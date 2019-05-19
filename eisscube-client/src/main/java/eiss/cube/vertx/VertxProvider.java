package eiss.cube.vertx;

import com.google.inject.Provider;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class VertxProvider implements Provider<Vertx> {

    private static Vertx vertx;

    public Vertx get() {
        if (vertx == null) {
            VertxOptions options = new VertxOptions();
            options.setWorkerPoolSize(2);
            vertx = Vertx.vertx(options);
        }
        return vertx;
    }

}
