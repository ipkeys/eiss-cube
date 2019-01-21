package eiss.cube.service.http.process.commands;

import com.google.gson.Gson;
import com.mongodb.DuplicateKeyException;
import eiss.cube.service.http.process.api.Api;
import eiss.helpers.CycleAndDutyCycleExtractor;
import eiss.models.cubes.CubeCommand;
import eiss.models.cubes.CubeReport;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import xyz.morphia.Datastore;
import xyz.morphia.Key;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.time.Instant;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/commands")
public class PostRoute implements Handler<RoutingContext> {

    private Vertx vertx;
    private Datastore datastore;
    private Gson gson;

    @Inject
    public PostRoute(Vertx vertx, Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        String json = context.getBodyAsString();
        log.info("Create new CubeCommand: {}", json);

        CubeCommand cmd = gson.fromJson(json, CubeCommand.class);
        if (cmd != null) {

            CycleAndDutyCycleExtractor cdc = gson.fromJson(json, CycleAndDutyCycleExtractor.class);

            if (cdc.getCycleAndDutyCycle() != null && !cdc.getCycleAndDutyCycle().isEmpty()) {
                String[] a = cdc.getCycleAndDutyCycle().split("/");
                if (a.length == 2) {
                    cmd.setCompleteCycle(Integer.valueOf(a[0]));
                    cmd.setDutyCycle(Integer.valueOf(a[1]));
                }
            }

            vertx.executeBlocking(op -> {
                cmd.setCreated(Instant.now());

                try {
                    Key<CubeCommand> key = datastore.save(cmd);
                    cmd.setId((ObjectId)key.getId());
                    op.complete(cmd);
                } catch (Exception e) {
                    log.error(e.getMessage());
                    op.fail("Unable to add cube command");
                }
            }, res -> {
                if (res.succeeded()) {
                    // send cmd to EISScube device
                    sendIt(cmd);
                    // prepare report record
                    recordIt(cmd);

                    response
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(SC_CREATED)
                        .end(gson.toJson(cmd));
                } else {
                    response
                        .setStatusCode(SC_BAD_REQUEST)
                        .setStatusMessage(res.cause().getMessage())
                        .end();
                }
            });
        } else {
            response
                .setStatusCode(SC_BAD_REQUEST)
                .setStatusMessage("Unable to add cube command")
                .end();
        }
    }

    private void sendIt(CubeCommand cmd) {
        vertx.eventBus().send("eisscube", new JsonObject()
            .put("id", cmd.getId().toString())
            .put("to", cmd.getCubeID()) // TODO: change to deviceID
            .put("cmd", cmd.toString()));
    }

    private void recordIt(CubeCommand cmd) {

        if (cmd.getCommand().startsWith("i")) {
            if (cmd.getTarget1() != null) {
                saveReportRecord(cmd.getCubeID(), "_Meter_1");
            }

            if (cmd.getTarget2() != null) {
                saveReportRecord(cmd.getCubeID(), "_Meter_2");
            }
        }
    }

    private void saveReportRecord(String deviceID, String reportID) {
        CubeReport report = new CubeReport();
        report.setDeviceID(deviceID);
        report.setReportID(deviceID + reportID);

        vertx.executeBlocking(op -> {
            try {
                Key<CubeReport> key = datastore.save(report);
                op.complete(key);
            } catch (DuplicateKeyException dup) {
                log.error(dup.getMessage());
                op.fail("ReportID already exists");
            } catch (Exception e) {
                log.error(e.getMessage());
                op.fail("Unable to add ReportID");
            }
        }, res -> log.info("Prepare report record for: {}", deviceID + reportID));
    }

}
