package eiss.db;

import com.google.inject.AbstractModule;
import dev.morphia.Datastore;

public class DatastoreModule extends AbstractModule {

    protected void configure() {
        bind(Datastore.class).annotatedWith(Users.class).toProvider(UsersDatastoreProvider.class);
        bind(Datastore.class).annotatedWith(Cubes.class).toProvider(CubesDatastoreProvider.class);
    }

}
