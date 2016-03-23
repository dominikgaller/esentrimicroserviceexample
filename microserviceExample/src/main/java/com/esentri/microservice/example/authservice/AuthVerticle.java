package com.esentri.microservice.example.authservice;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;

/**
 * The class @code{AuthVerticle} implements a microservice
 * for user authentication. It authorizes a user by his
 * password and username combination. Therefore a SessionID is 
 * required, which is managed by the sessionservice.
 * Furthermore this class implements a basic version of
 * outlogging a user.
 *  
 * @author dominikgaller
 *
 */
public class AuthVerticle extends AbstractVerticle {
	
	/** Address for login requests. */
	private static final String loginreq = "esentri.login.request";
	
	/** Address for logout requests. */
	private static final String logout = "esentri.logout";
	
	/** Name of the data source, here the shared data base. */
	private static final String SHAREDDBNAME = "users";
	
	/** JDBCClient for shared async. data base connection. */
	private JDBCClient client;
	
	/** Eventbus instance. */
	private EventBus eb;
	
	private Logger l;
	
	/** 
	 * This verticles start method. 
	 * The listeners are registered to their addresses here. 
	 * Also an instance of the shared database is created.
	 */
	public void start() {
		l = LoggerFactory.getLogger(AuthVerticle.class);
		this.eb = vertx.eventBus();
		this.client = JDBCClient.createShared(vertx, new JsonObject(), SHAREDDBNAME);
		eb.consumer(loginreq, this::loginHandler);
		eb.consumer(logout, this::logoutHandler);
	}
	
	/**
	 * Handler for login requests. 
	 * 
	 * @param msg the payload send
	 */
	private void loginHandler(Message<String> msg) {
		l.info("Request for login received.");
		l.info("Payload for login request is: " + msg.body());
		JsonObject loginCreds = new JsonObject(msg.body());
		String sessionId = extractSessionId(loginCreds);
		if(loginCreds.containsKey("username") && loginCreds.containsKey("password")) {
			String name = loginCreds.getString("username");
			Future<Boolean> exists = Future.factory.future();
			exists.setHandler(new AuthorizeUserHandler(client, eb, sessionId, loginCreds));
			userExists(name, exists);
		} else {
			throw new IllegalArgumentException("Credential combination is wrong");
		}
		
	}
	
	/**
	 * Checks whether a user exists in the database or not.
	 * 
	 * @param name the users name.
	 * @param exists future where the async result is bind to.
	 */
	private void userExists(String name, Future<Boolean> exists) {
		this.client.getConnection(conn -> {
			if(conn.failed()) {
				throw new IllegalStateException(conn.cause().getMessage());
			}
			SQLConnection connection = conn.result();
			checkUsername(connection, name, exists);
		});
	}
	
	/**
	 * Performs a SQL statement, to check if a given username exists in the user database.
	 * 
	 * @param conn the SQLConnection whete the query is executed.
	 * @param name the users name
	 * @param exists future where the async result is bind to.
	 */
	private void checkUsername(SQLConnection conn, String name, Future<Boolean> exists) {
		String query = "SELECT Name FROM User WHERE Name = ?";
		JsonArray params = new JsonArray().add(name);
		conn.queryWithParams(query, params , rs -> {
			if(rs.failed()) {
				throw new RuntimeException(rs.cause().getMessage());
			}
			int count = rs.result().getResults().size();
			if(count > 0) {
				exists.complete(true);
			} else {
				exists.complete(false);
			}
			conn.close();
		});
	}
	
	/**
	 * Handler for logout request.
	 * Has no functionality here. User access is 
	 * managed by the client side. So only a logout token
	 * is created and send to the client.
	 * 
	 * @param msg the messages payload.
	 */
	private void logoutHandler(Message<String> msg) {
		l.info("Request for logout received.");
		l.info("Payload for logout request is: " + msg.body());
		JsonObject json = new JsonObject().put("logout", true);
		l.info(json.toString());
		msg.reply(json.toString());
	}

	/**
	 * Extracts the session id out of a given JsonObject.
	 * To pass a sessionId to this method, the sessionId key
	 * must be placed in the first level of the JSON object.
	 * E.g.:
	 * 	{
	 * 		...
	 * 		"sessionId" : sessionId,
	 * 		...
	 * 	}
	 * 
	 * @param json the JSON object to extract sessionId from.
	 * @return the sessionId if found.
	 */
	private String extractSessionId(JsonObject json) {
		if(json.containsKey("sessionId")) {
			return json.getString("sessionId");
		} else {
			//TODO implement proper error handling, if message is malformed
			throw new IllegalArgumentException("Message contains no sessionId informations");
		}
	}

}
