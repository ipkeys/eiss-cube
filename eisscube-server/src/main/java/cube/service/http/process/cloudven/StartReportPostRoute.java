package cube.service.http.process.cloudven;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.query.Query;
import cube.db.Cube;
import cube.json.messages.cloudven.StartReport;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.experimental.updates.UpdateOperator;
import dev.morphia.query.experimental.updates.UpdateOperators;
import eiss.api.Api;
import cube.models.CubeCommand;
import cube.models.CubeInput;
import cube.models.CubeReport;
import cube.models.CubeSetup;
import cube.models.EISScube;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;

@Slf4j
@Api
@Path("/cloudven/startreport")
public class StartReportPostRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public StartReportPostRoute(Vertx vertx, @Cube Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();

        String json = context.getBodyAsString();

        StartReport req = gson.fromJson(json, StartReport.class);
        if (req == null) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage("Unable to start report for CloudVEN")
                    .end();
            return;
        }

        final String ven = req.getVen();
        final String resource = req.getResource();
        final Integer sampleRateSeconds = req.getSampleRateSeconds();

        vertx.executeBlocking(op -> {

            Query<EISScube> q = datastore.find(EISScube.class);
            if (resource != null && !resource.isEmpty()) {
                q.filter(
                    Filters.and(
                        Filters.eq("settings.VEN", ven),
                        Filters.eq("name", resource)
                    )
                );
            } else {
                q.filter(Filters.eq("settings.VEN", ven));
            }

            List<EISScube> cubes = q.iterator().toList();
            if (cubes != null) {
                cubes.forEach(cube -> {
                    CubeSetup setup = datastore.find(CubeSetup.class).filter(Filters.eq("cubeID", cube.getId())).first();
                    if (setup != null && setup.getInput() != null) {
                        CubeInput input = setup.getInput();
                        if (input != null && input.getConnected()) {
                            CubeCommand cmd = new CubeCommand();
                            cmd.setCubeID(cube.getId());

                            // put command under cube's group
                            cmd.setCubeName(cube.getName());
                            cmd.setGroup(cube.getGroup());
                            cmd.setGroup_id(cube.getGroup_id());
                            // ~put command under cube's group

                            switch (input.getSignal()) {
                                case "p":
                                    cmd.setCommand("icp");
                                    cmd.setTransition(input.getWatch());
                                    cmd.setCompleteCycle(sampleRateSeconds);
                                    break;
                                case "c":
                                    cmd.setCommand("icc");
                                    cmd.setTransition(input.getWatch());
                                    break;
                                default:
                                    break;
                            }

                            cmd.setStatus("Created");
                            cmd.setCreated(Instant.now());

                            CubeCommand new_command = datastore.save(cmd);

                            vertx.eventBus().send("eisscube",
                                new JsonObject()
                                    .put("id", new_command.getId().toString())
                                    .put("to", cube.getDeviceID())
                                    .put("socket", cube.getSocket())
                                    .put("cmd", new_command.toString())
                            );

                            // prepare report record
                            if (cmd.getCommand().startsWith("ic")) {
                                Query<CubeReport> qR = datastore.find(CubeReport.class);
                                qR.filter(Filters.eq("cubeID", cube.getId()));

                                List<UpdateOperator> updates = new ArrayList<>();

                                updates.add(UpdateOperators.setOnInsert(Map.of("cubeID", cube.getId())));
                                updates.add(UpdateOperators.set("type", cmd.getCommand().replace("ic", ""))); // leave just "p" or "c"

                                qR.update(updates.get(0), updates.stream().skip(1).toArray(UpdateOperator[]::new)).execute(new UpdateOptions().upsert(true));
                            }

                        }
                    } else {
                        String msg = String.format("Setup not exists for EISScube for CloudVEN: %s and Name: %s", ven, resource);
                        log.error(msg);
                    }
                });
            } else {
                String msg = String.format("Unable to find an EISScube for CloudVEN: %s and Name: %s", ven, resource);
                log.error(msg);
                op.fail(msg);
            }
            op.complete();
        }, res -> {
            if (res.succeeded()) {
                response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(SC_CREATED)
                        .end();
            } else {
                response.setStatusCode(SC_BAD_REQUEST)
                        .setStatusMessage(res.cause().getMessage())
                        .end();
            }
        });

    }

}
