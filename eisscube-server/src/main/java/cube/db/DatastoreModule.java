package cube.db;

import com.google.inject.AbstractModule;
import dev.morphia.Datastore;

public class DatastoreModule extends AbstractModule {

    protected void configure() {
        bind(Datastore.class).annotatedWith(Eiss.class).toProvider(EissDatastoreProvider.class);
        bind(Datastore.class).annotatedWith(Cube.class).toProvider(CubeDatastoreProvider.class);
    }

}
