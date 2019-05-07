package eiss.cube.service.client;

import com.google.inject.Provider;
import eiss.client.EISSClient;
import eiss.client.api.EventHandler;
import eiss.client.api.ReportHandler;
import eiss.client.api.VenConfiguration;

import javax.inject.Inject;

public class ClientProvider implements Provider<EISSClient> {

    private static EISSClient client;
    private EventHandler eventHandler;
    private ReportHandler reportHandler;
    private VenConfiguration venConfiguration;

    @Inject
    public ClientProvider(EventHandler eventHandler, ReportHandler reportHandler, VenConfiguration venConfiguration) {
        this.eventHandler = eventHandler;
        this.reportHandler = reportHandler;
        this.venConfiguration = venConfiguration;
    }

    public EISSClient get() {
        if (client == null) {
            client = new EISSClient();
            EISSClient.configureLogging();

            client.setEventHandler(eventHandler);
            client.setReportHandler(reportHandler);
            client.setVenConfiguration(venConfiguration);
        }

        return client;
    }

}
