package eiss.cube.db;

import com.google.inject.Provider;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import eiss.cube.config.AppConfig;
import eiss.cube.config.DatabaseConfig;
import eiss.cube.db.converters.JsonObjectConverter;
import dev.morphia.Datastore;
import dev.morphia.Morphia;

import javax.inject.Inject;

public class DatastoreProvider implements Provider<Datastore> {

    private DatabaseConfig cfg;

    private final Morphia morphia = new Morphia();
    private static Datastore datastore;

    @Inject
    public DatastoreProvider(AppConfig cfg) {
        this.cfg = cfg.getDatabaseConfig();
    }

    public Datastore get() {

        if (datastore == null) {
            String host = cfg.getHost();
            Integer port = cfg.getPort();
            String name = cfg.getName();

            morphia.mapPackage(cfg.getDataPackage());
            // custom converters
            morphia.getMapper().getConverters().addConverter(JsonObjectConverter.class);

            MongoClient client = new MongoClient(new ServerAddress(host, port));

            datastore = morphia.createDatastore(client, name);
            datastore.ensureIndexes();
        }

        return datastore;
    }

}
