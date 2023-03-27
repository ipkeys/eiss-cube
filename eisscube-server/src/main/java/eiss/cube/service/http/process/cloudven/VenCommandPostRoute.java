package eiss.cube.service.http.process.cloudven;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import eiss.cube.json.messages.CycleAndDutyCycleExtractor;
import eiss.cube.json.messages.cloudven.VenCommand;
import dev.morphia.query.filters.Filters;
import eiss.api.Api;
import eiss.models.cubes.CubeCommand;
import eiss.models.cubes.EISScube;
import eiss.db.Cubes;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import java.time.Instant;
import java.util.List;

import static dev.morphia.query.filters.Filters.and;
import static dev.morphia.query.filters.Filters.eq;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static jakarta.servlet.http.HttpServletResponse.SC_CREATED;

@Slf4j
@Api
@Path("/cloudven/command")
public class VenCommandPostRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public VenCommandPostRoute(Vertx vertx, @Cubes Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();

        String json = context.body().asString();

        VenCommand req = gson.fromJson(json, VenCommand.class);
        if (req == null) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage("Unable to create a cube command for CloudVEN")
                    .end();
            return;
        }

        String ven = req.getVen();
        String resource = req.getResource();

        vertx.executeBlocking(op -> {
            Query<EISScube> q = datastore.find(EISScube.class);
            if (resource != null && !resource.isEmpty()) {
                q.filter(and(
                    eq("settings.VEN", ven),
                    eq("name", resource)
                ));
            } else {
                q.filter(eq("settings.VEN", ven));
            }

            List<EISScube> cubes = q.iterator().toList();
            if (cubes != null) {
                cubes.forEach(cube -> {
                    CubeCommand cmd = new CubeCommand();
                    cmd.setCubeID(cube.getId());

                    // put command under cube's group
                    cmd.setCubeName(cube.getName());
                    cmd.setGroup(cube.getGroup());
                    cmd.setGroup_id(cube.getGroup_id());
                    // ~put command under cube's group

                    cmd.setCommand(req.getCommand());

                    // use START and END timestamp
                    cmd.setStartTime(req.getStart());
                    cmd.setEndTime(req.getEnd());

                    CycleAndDutyCycleExtractor cdc = gson.fromJson(json, CycleAndDutyCycleExtractor.class);
                    if (cdc.getCycleAndDutyCycle() != null && !cdc.getCycleAndDutyCycle().isEmpty()) {
                        String[] a = cdc.getCycleAndDutyCycle().split("/");
                        if (a.length == 2) {
                            cmd.setCompleteCycle(Integer.valueOf(a[0]));
                            cmd.setDutyCycle(Integer.valueOf(a[1]));
                        }
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
                });
                op.complete();
            } else {
                String msg = String.format("Unable to find an EISScube for VEN: %s and Name: %s", ven, resource);
                log.error(msg);
                op.fail(msg);
            }
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
