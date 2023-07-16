package com.redhat.demo;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class MainVerticle extends AbstractVerticle {

    @Override
    public void start() {

        vertx.deployVerticle(new HelloVerticle());
        Router router = Router.router(vertx);
        router.get("/api/v1/hello").handler(this::helloVertx);
        router.get("/api/v1/hello/:name").handler(this::helloName);
        
        int httpPort;
        try{
            httpPort = Integer.parseInt(System.getProperty("http.port","8080"));
        } catch(NumberFormatException nfe){
            httpPort = 8080;
        }
        vertx.createHttpServer().requestHandler(router).listen(httpPort);
    }

    void helloVertx(RoutingContext ctx) {
        vertx.eventBus().request("hello.vertx.addr","", reply->{
            ctx.request().response().end((String)reply.result().body());
        });
    }

    void helloName(RoutingContext ctx) {
        String name = ctx.pathParam("name");
        vertx.eventBus().request("hello.name.addr", name, reply ->{
            ctx.request().response().end((String)reply.result().body());
        });
    }

}
