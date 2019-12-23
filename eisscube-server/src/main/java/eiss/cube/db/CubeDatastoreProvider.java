package eiss.cube.db;

import com.google.inject.Provider;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import eiss.cube.config.AppConfig;
import eiss.cube.config.CubeDatabaseConfig;
import eiss.cube.db.converters.JsonObjectConverter;
import dev.morphia.Datastore;
import dev.morphia.Morphia;

import javax.inject.Inject;

public class CubeDatastoreProvider implements Provider<Datastore> {

    private CubeDatabaseConfig cfg;

    private final Morphia morphia = new Morphia();
    private static Datastore datastore;

    @Inject
    public CubeDatastoreProvider(AppConfig cfg) {
        this.cfg = cfg.getCubeDatabaseConfig();
    }

    public Datastore get() {

        if (datastore == null) {
            String host = cfg.getHost();
            int port = Integer.parseInt(cfg.getPort());
            String name = cfg.getName();

            // custom converters
            morphia.getMapper().getConverters().addConverter(JsonObjectConverter.class);

            morphia.mapPackage(cfg.getDataPackage());

            MongoClient client = new MongoClient(new ServerAddress(host, port));

            datastore = morphia.createDatastore(client, name);
            datastore.ensureIndexes();
        }

        return datastore;
    }

}
