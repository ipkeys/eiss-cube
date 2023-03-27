package eiss.cube.service.http.process.eiss_api.reports;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.morphia.query.filters.Filters;
import eiss.cube.input.Conversion;
import eiss.cube.json.messages.reports.Report;
import eiss.cube.json.messages.reports.ReportRequest;
import eiss.cube.json.messages.reports.ReportResponse;
import eiss.cube.service.http.process.meters.MeterRequest;
import eiss.cube.service.http.process.meters.MeterResponse;
import eiss.api.Api;
import eiss.db.Cubes;
import eiss.models.cubes.CubeSetup;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import static dev.morphia.query.filters.Filters.eq;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static java.util.stream.Collectors.toList;
import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@Api
@Path("/eiss-api/report")
public class ReportForDeviceRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Gson gson;
    private final Conversion conversion;
    private final Datastore datastore;

    @Inject
    public ReportForDeviceRoute(Vertx vertx, Gson gson, Conversion conversion, @Cubes Datastore datastore) {
        this.vertx = vertx;
        this.gson = gson;
        this.conversion = conversion;
        this.datastore = datastore;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();
        String jsonBody = context.body().asString();

        if (jsonBody != null && !jsonBody.isEmpty()) {
            vertx.executeBlocking(op -> {
                try {
                    ReportRequest req = gson.fromJson(jsonBody, ReportRequest.class);
                    if (req == null) {
                        op.fail("Bad request");
                    } else {
                        ReportResponse res = getMeterReport(req);
                        op.complete(res);
                    }
                } catch (Exception e) {
                    op.fail(e.getMessage());
                }
            }, res -> {
                if (res.succeeded()) {
                    response.setStatusCode(SC_OK)
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(gson.toJson(res.result()));
                } else {
                    response.setStatusCode(SC_INTERNAL_SERVER_ERROR)
                        .setStatusMessage(res.cause().getMessage())
                        .end();
                }
            });
        } else {
            response.setStatusCode(SC_BAD_REQUEST)
                .end();
        }
    }

    private ReportResponse getMeterReport(ReportRequest req) {
        // tune up for ElectSolv portal (if needed!)
        MeterResponse meterResponse = new MeterResponse();
        MeterRequest meterRequest = copyFromReportRequest(req);

        conversion.process(meterRequest, meterResponse);

        return ReportResponse.builder()
            .usage(
                meterResponse.getUsage().stream().map(meter -> Report.builder().t(meter.getT()).v(meter.getV()).build()).collect(toList())
            ).build();
    }

    private MeterRequest copyFromReportRequest(ReportRequest req) {
        MeterRequest r = new MeterRequest();

        r.setCubeID(req.getDeviceID());
        r.setFrom(req.getFrom());
        r.setTo(req.getTo());
        r.setAggregation(req.getAggregation());

        Query<CubeSetup> q = datastore.find(CubeSetup.class);
        q.filter(eq("cubeID", new ObjectId(req.getDeviceID())));
        CubeSetup setup = q.first();

        if (setup != null && setup .getInput() != null) {
            r.setType(setup.getInput().getSignal()); // Signal type is a type of report - pulse or cycle
            r.setFactor(setup.getInput().getFactor());
            r.setWatch(setup.getInput().getWatch());
            r.setLoad(setup.getInput().getLoad());
            r.setMeter(setup.getInput().getMeter());
            r.setUnit(setup.getInput().getUnit());
        } else { //by default
            r.setType("p"); // type of report - pulse
            r.setFactor(1000F);
            r.setWatch("r");
            r.setLoad(1000F);
            r.setMeter("e"); // Electric meter
            r.setUnit("kWh");
        }

        return r;
    }
}
