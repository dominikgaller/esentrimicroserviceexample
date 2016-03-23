package com.esentri.microservice.example;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The class {@code StartUp} is used as common sense to 
 * start the application within an IDE or as java package.
 * @author dominikgaller
 *
 */
public final class StartUp {
	
	/** Path to server verticle. */
	private static final String SERVERPATH = "com.esentri.microservice.example.server.Server";
	
	private static Logger l;
	
	/** private constructor for final classes makes everyone happy. */
	private StartUp() { }
	
	/**
	 * Main method to start the vert.x application.
	 * @param args args
	 */
	public static void main(String[] args) {
		l = LoggerFactory.getLogger(StartUp.class);
		final Vertx vertx = Vertx.vertx();
		l.info("Populating Databases");
		populateDatabases(vertx);
		l.info("Starting server");
		vertx.deployVerticle(SERVERPATH);
	}
	
	/**
	 * Populate the database.
	 * 
	 * @param vertx vertx instance.
	 */
	private static void populateDatabases(Vertx vertx) {
		EntryDatabasePopulator edp = new EntryDatabasePopulator(vertx, "entries");
		edp.populateDatabase();
		
		UserDatabasePopulator udp = new UserDatabasePopulator(vertx, "users");
		udp.populateDatabase();
	}
}	
