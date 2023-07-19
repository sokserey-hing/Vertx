package com.redhat.demo;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonObject;
import io.vertx.core.Promise;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CSRFHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;

public class MainVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> start) {

        vertx.deployVerticle(new HelloVerticle());
        //vertx.deployVerticle("src/main/resources/Hello.groovy");
        //vertx.deployVerticle("src/main/resources/Hello.js");
        Router router = Router.router(vertx);

        router.route().handler(ctx -> {
            String authToken = ctx.request().getHeader("AUTH_TOKEN");
            if (authToken != null && "mySuperSecretAuthToken".contentEquals(authToken)) {
                ctx.next();
            } else {
                ctx.response().setStatusCode(401).setStatusMessage("UNAUTHORIZED").end();
            }
        });

        SessionStore store = LocalSessionStore.create(vertx);
        router.route().handler(LoggerHandler.create());
        router.route().handler(SessionHandler.create(store));
        router.route().handler(CorsHandler.create("localhost"));
        router.route().handler(CSRFHandler.create(vertx, "b9JnSvQ+8SN04YqKixZXI3lRykJHcq+Q6C1fWw5mvWs7AUFSCBsnaHBmWdEj7yr+7aTLR7kqU91I")); // dd if=/dev/urandom bs=384 count=1 | base64  -> to generate secrete from cli 

        
        router.get("/api/v1/hello").handler(this::helloHandler);
        router.get("/api/v1/hello/:name").handler(this::helloByNameHandler);
        router.route().handler(StaticHandler.create("src/main/resources/web").setIndexPage("index.html"));

        ConfigStoreOptions defaultConfig = new ConfigStoreOptions()
                .setType("file")
                .setFormat("json")
                .setConfig(new JsonObject().put("path", "src/main/resources/config.json"));
                
        ConfigStoreOptions cliConfig = new ConfigStoreOptions()
                .setType("json")
                .setConfig(config());

        ConfigRetrieverOptions opts = new ConfigRetrieverOptions()
                .addStore(defaultConfig)
                .addStore(cliConfig);
        ConfigRetriever cfgRetriever = ConfigRetriever.create(vertx, opts);

        Handler<AsyncResult<JsonObject>> handler = asyncResullt -> this.handleConfigResult(start, router, asyncResullt);
        cfgRetriever.getConfig(handler);

    }

    void handleConfigResult(Promise<Void> start, Router router, AsyncResult<JsonObject> asyncResult) {
        if (asyncResult.succeeded()) {
            JsonObject config = asyncResult.result();
            JsonObject http = config.getJsonObject("http");
            int httpPort = http.getInteger("port");
            vertx.createHttpServer().requestHandler(router).listen(httpPort);
            start.complete();
        } else {
            start.fail("Unable to load the configuration");
        }

    }

    void helloHandler(RoutingContext ctx) {
        vertx.eventBus().request("hello.vertx.addr", "", reply -> {
            ctx.request().response().end((String) reply.result().body());
        });
    }

    void helloByNameHandler(RoutingContext ctx) {
        String name = ctx.pathParam("name");
        vertx.eventBus().request("hello.name.addr", name, reply -> {
            ctx.request().response().end((String) reply.result().body());
        });
    }

}
