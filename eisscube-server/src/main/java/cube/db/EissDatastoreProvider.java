package cube.db;

import com.google.inject.Provider;
import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import cube.config.AppConfig;
import cube.config.DatabaseConfig;

import javax.inject.Inject;

public class EissDatastoreProvider implements Provider<Datastore> {

    private final DatabaseConfig cfg;
    private static Datastore datastore;

    @Inject
    public EissDatastoreProvider(AppConfig cfg) {
        this.cfg = cfg.getEissDatabaseConfig();
    }

    public Datastore get() {

        if (datastore == null) {
            String name = cfg.getName();
            String connection = cfg.getConnection();
            String dataPackage = cfg.getDataPackage();

            // custom converters
            // ~custom converters
            datastore = Morphia.createDatastore(MongoClients.create(connection), name);

            datastore.getMapper().mapPackage(dataPackage);
            datastore.ensureIndexes();
        }

        return datastore;
    }

}
