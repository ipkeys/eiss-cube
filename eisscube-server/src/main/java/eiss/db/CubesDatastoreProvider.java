package eiss.db;

import com.google.inject.Provider;
import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import eiss.config.AppConfig;

import javax.inject.Inject;

public class CubesDatastoreProvider implements Provider<Datastore> {

    private static Datastore datastore;

    @Inject
    private AppConfig cfg;

    public Datastore get() {

        if (datastore == null) {
            // custom converters
            // ~custom converters
            datastore = Morphia.createDatastore(MongoClients.create(cfg.getDatabaseConfig().getConnection()), DATABASE.cubes);

            datastore.getMapper().mapPackage(DATABASE.cubes_package);
            datastore.ensureIndexes();
        }

        return datastore;
    }

}
