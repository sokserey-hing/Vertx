vertx.eventBus().consumer("hello.vertx.addr").handler({ msg ->
    msg.reply("Hello ${msg.body()} from Groovy!")
})