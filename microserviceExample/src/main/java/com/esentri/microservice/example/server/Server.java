package com.esentri.microservice.example.server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

/**
 * The class {@code Server} implement basic features for a web server. Also 
 * it takes care of the verticle deployment. Furthermore sets up a demo database.
 * @author dominikgaller
 *
 */
public class Server extends AbstractVerticle {
	
	/** Bridge path for the event bus. */
	private static final String BRIDGEPATH = "/eventbus/*";
	
	/** Path to web root directory. */
	private static final String WEBROOT = "../clientUI/app";
	
	/** Port number the web server listens to. */
	private static final int PORT = 8080;
	
	/** The httpServer object. */
	private static HttpServer httpServer;
	
	private Logger l;
	
	/** 
	 * Empty constructor.
	 */
	public Server() {};
	
	/**
	 * The start method for this verticle. Sets up the http server. Also 
	 * connects the eventbus to the defined bridge, for client server communication.
	 * Furthermore data base initialization is started, and the service verticles are deployed here.
	 */
	public void start() {
		l = LoggerFactory.getLogger(Server.class);
		Router router = Router.router(vertx);
		BridgeOptions options = new BridgeOptions()
				.addInboundPermitted(new PermittedOptions())
				.addOutboundPermitted(new PermittedOptions());
		SockJSHandler ebHandler = SockJSHandler.create(vertx).bridge(options);
		router.route(BRIDGEPATH).handler(ebHandler);
		router.route().handler(StaticHandler.create().setWebRoot(WEBROOT));
		httpServer = vertx.createHttpServer().requestHandler(router::accept).listen(PORT);
		l.info("HTTP Server started at port: " + PORT);
		deployVerticles();
	}
	
	/**
	 * Deploys the various verticles.
	 */
	private void deployVerticles() {
		l.info("Deploying verticles.");
		vertx.deployVerticle("com.esentri.microservice.example.phonebookservice.PhonebookVerticle");
		vertx.deployVerticle("com.esentri.microservice.example.authservice.AuthVerticle");
		vertx.deployVerticle("com.esentri.microservice.example.sessionservice.SessionVerticle");
		l.info("Verticles deployed.");
	}
	
	/**
	 * Stop method for this verticle. Calls the close Method, to ensure
	 * the httpServer is shutted down. The stop method is automatically called by
	 * undeploying the verticle.
	 */
	public void stop() {
		l.info("Stopping server");
		Future<Boolean> fut = Future.factory.future();
		close(fut);
	}
	
	/**
	 * Closes the http server.
	 * @param future informs whether closing was successful
	 */
	public void close(Future<Boolean> future) {
		httpServer.close(onclose -> {
			future.complete(onclose.succeeded());
		});
	}
}
