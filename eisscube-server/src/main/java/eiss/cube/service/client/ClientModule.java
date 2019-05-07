package eiss.cube.service.client;

import com.google.inject.AbstractModule;
import eiss.client.EISSClient;
import eiss.client.api.EventHandler;
import eiss.client.api.ReportHandler;
import eiss.client.api.VenConfiguration;

public class ClientModule extends AbstractModule {

    protected void configure() {
        bind(EISSClient.class).toProvider(ClientProvider.class);

        bind(EventHandler.class).to(ClientEventHandler.class);
        bind(ReportHandler.class).to(ClientReportHandler.class);
        bind(VenConfiguration.class).to(ClientVenConfigurationHandler.class);

        bind(VEN.class).asEagerSingleton();
    }

}
