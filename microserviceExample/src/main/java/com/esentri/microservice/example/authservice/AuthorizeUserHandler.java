package com.esentri.microservice.example.authservice;

import java.util.List;

import com.esentri.microservice.example.authservice.entities.User;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;

/**
 * The class @code AuthorizeUserHandler manages user authorization as
 * asynchrony event.
 * 
 * @author dominikgaller
 *
 */
class AuthorizeUserHandler implements Handler<AsyncResult<Boolean>> {
	
	/** Address for answering login requests. */
	private static final String loginreply = "esentri.login.reply";
	
	/** Users session id. */
	private String sessionId;
	
	/** Users login credentials. */
	private JsonObject loginCreds;
	
	/** JDBCClient for performing sql queries. */
	private JDBCClient client;
	
	/** Eventbus instance. */
	private EventBus eb;
	
	private Logger l;
	
	/** 
	 * Constructor
	 * 
	 * @param client JDBCClient
	 * @param eb Eventbus
	 * @param sessionId users session id
	 * @param loginCreds users login credentials
	 */
	public AuthorizeUserHandler(JDBCClient client, EventBus eb, String sessionId, JsonObject loginCreds) {
		l = LoggerFactory.getLogger(AuthorizeUserHandler.class);
		this.sessionId = sessionId;
		this.loginCreds = loginCreds;
		this.eb = eb;
		this.client = client;
	}
	
	/**
	 * Handles the asynchrony event and checks whether a user exists and can authorized.
	 * After authorization the client is informed about the result.
	 */
	@Override
	public void handle(AsyncResult<Boolean> exists) {
		JsonObject reply = new JsonObject();
		if(exists.succeeded() && exists.result()) {
			Future<User> userFut = Future.factory.future();
			userFut.setHandler(new UserObjectHandler(sessionId));
			authorizeUser(loginCreds, userFut);	
		} else {
			reply.put("loggedIn", false);
			l.info("User Login was not successful, because user does not exist. "
					+ "Answernig with payload: " + reply.toString());
			eb.send(loginreply + ":" + sessionId, reply.toString());
		}
	}
	
	/**
	 * Authorizes a user against the database by his credentials. This method
	 * is asynchrony, therefore it requires a future, where the user informations can be stored.
	 * The future contains the user, if he exists, and thus succeeds, or fails otherwise.
	 * 
	 * @param loginCreds users credentials in JSON notation.
	 * @param fut the future to store the async result.
	 */
	private void authorizeUser(JsonObject loginCreds, Future<User> fut) {
		this.client.getConnection(conn -> {
			if(conn.failed()) {
				throw new IllegalArgumentException(conn.cause().getMessage());
			}
			SQLConnection connection = conn.result();
			getUser(connection, loginCreds, fut);
		});
	}
	
	/**
	 * Gets the user from the database. If there is more then one user, 
	 * the future fails. By design usernames are unique.
	 * 
	 * @param connection the SQLConnection for the query.
	 * @param loginCreds the user credentials as JSON object.
	 * @param fut the future for the result.
	 */
	private void getUser(SQLConnection connection, JsonObject loginCreds,Future<User> fut) {
		String query = "SELECT Id, Name, Password FROM User WHERE Name =? AND Password =?";	
		JsonArray params = new JsonArray().add(loginCreds.getString("username"))
				.add(loginCreds.getString("password"));
		connection.queryWithParams(query, params, rs -> {
			if(rs.succeeded()) {
				ResultSet resultSet = rs.result();
				List<JsonArray> results = resultSet.getResults();
				if(results.size() > 1) {
					fut.fail("There is no user with this username");
				} else {
					for(JsonArray row : results) {
						fut.complete(new User(row.getLong(0), row.getString(1), row.getString(2)));
					}
				}
			} else {
				throw new RuntimeException(rs.cause().getMessage());
			}
		});
	}
	
	/**
	 * Private class for handling user objects. Only works
	 * on existing users.
	 * 
	 * @author dominikgaller
	 *
	 */
	private class UserObjectHandler implements Handler<AsyncResult<User>> {
		
		/** The users session id. */
		private String sessionId;
		
		/**
		 * Constructor for a new handler.
		 * 
		 * @param sessionId the users session id.
		 */
		public UserObjectHandler(String sessionId) {
			this.sessionId = sessionId;
		}
		

		/**
		 * Handles the user event.
		 */
		@Override
		public void handle(AsyncResult<User> user) {
			JsonObject reply = new JsonObject();
			if(user.succeeded()) {
				User u = user.result();
				reply.put("loggedIn", true).put("user", userToReply(u));
				l.info("Login was successful. Answering with payload: " + reply.toString());
			} else {
				reply.put("loggedIn", false);
				l.info("User login was not successful, because user entered wrong password. "
						+ "Answering request with: " + reply.toString());
			}
			eb.send(loginreply + ":" + sessionId, reply.toString());
			
		}
		
		/**
		 * Creates the reply for the client side.
		 * 
		 * @param u user
		 * @return reply as JSON for client side.
		 */
		private JsonObject userToReply(User u) {
			return new JsonObject().put("name", u.getName()).put("id", u.getId());
		}
	}
}
