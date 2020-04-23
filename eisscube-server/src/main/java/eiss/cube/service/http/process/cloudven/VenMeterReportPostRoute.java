package eiss.cube.service.http.process.cloudven;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.Key;
import dev.morphia.query.Query;
import eiss.cube.db.Cube;
import eiss.cube.input.Conversion;
import eiss.cube.json.messages.CycleAndDutyCycleExtractor;
import eiss.cube.json.messages.cloudven.VenCommand;
import eiss.cube.json.messages.cloudven.VenReport;
import eiss.cube.json.messages.report.ReportRequest;
import eiss.cube.json.messages.report.ReportResponse;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubeCommand;
import eiss.models.cubes.CubeReport;
import eiss.models.cubes.CubeSetup;
import eiss.models.cubes.EISScube;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.time.Instant;
import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@Api
@Path("/cloudven/report")
public class VenMeterReportPostRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;
    private final Conversion conversion;

    @Inject
    public VenMeterReportPostRoute(Vertx vertx, @Cube Datastore datastore, Gson gson, Conversion conversion) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
        this.conversion = conversion;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();

        String json = context.getBodyAsString();

        VenReport req = gson.fromJson(json, VenReport.class);
        if (req == null) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage("Unable to get a report")
                    .end();
            return;
        }

        final String ven = req.getVen();
        final String resource = req.getResource();
        final ReportResponse report_res = new ReportResponse();

        vertx.executeBlocking(op -> {
            if (ven != null && resource != null) {
                Query<EISScube> q = datastore.createQuery(EISScube.class);
                q.and(
                    q.criteria("settings.VEN").equal(ven),
                    q.criteria("name").equal(resource)
                );

                EISScube cube = q.first();
                if (cube != null) {
                    CubeSetup setup = datastore.createQuery(CubeSetup.class).field("cubeID").equal(cube.getId()).first();
                    CubeReport report = datastore.createQuery(CubeReport.class).field("cubeID").equal(cube.getId()).first();
                    if (report != null) {
                        ReportRequest report_req = new ReportRequest();

                        report_req.setUtcOffset(0L);
                        report_req.setCubeID(cube.getId().toString());
                        report_req.setType(report.getType());
                        if (setup != null && setup.getInput() != null) {
                            report_req.setFactor(setup.getInput().getFactor());
                            report_req.setLoad(setup.getInput().getLoad());
                            report_req.setWatch(setup.getInput().getWatch());
                        }
                        report_req.setFrom(req.getFrom());
                        report_req.setTo(req.getTo());
                        report_req.setAggregation(req.getAggregation());

                        conversion.process(report_req, report_res);

                        op.complete(gson.toJson(report_res));
                    }
                }
            } else {
                op.fail("Report not found");
            }
        }, res -> {
            if (res.succeeded()) {
                response.setStatusCode(SC_OK)
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end((String)res.result());
            } else {
                response.setStatusCode(SC_BAD_REQUEST)
                        .setStatusMessage(res.cause().getMessage())
                        .end();
            }
        });

    }

}
