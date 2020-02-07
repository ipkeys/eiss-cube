package eiss.cube.db;

import com.google.inject.Provider;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import eiss.cube.config.AppConfig;
import eiss.cube.config.EissDatabaseConfig;

import javax.inject.Inject;

public class EissDatastoreProvider implements Provider<Datastore> {

    private EissDatabaseConfig cfg;

    private final Morphia morphia = new Morphia();
    private static Datastore datastore;

    @Inject
    public EissDatastoreProvider(AppConfig cfg) {
        this.cfg = cfg.getEissDatabaseConfig();
    }

    public Datastore get() {

        if (datastore == null) {
            String host = cfg.getHost();
            int port = Integer.parseInt(cfg.getPort());
            String name = cfg.getName();

            // custom converters
            // ~custom converters

            morphia.mapPackage(cfg.getDataPackage());

            MongoClient client = new MongoClient(new ServerAddress(host, port));

            datastore = morphia.createDatastore(client, name);
            datastore.ensureIndexes();
        }

        return datastore;
    }

}
