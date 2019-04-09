package eiss.cube.db;

import com.google.inject.AbstractModule;
import dev.morphia.Datastore;

public class DatastoreModule extends AbstractModule {

    protected void configure() {
        bind(Datastore.class).toProvider(DatastoreProvider.class);
    }

}
