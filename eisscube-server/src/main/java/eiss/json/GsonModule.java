package eiss.json;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;

public class GsonModule extends AbstractModule {

    protected void configure() {
        bind(Gson.class).toProvider(GsonProvider.class);
    }

}
