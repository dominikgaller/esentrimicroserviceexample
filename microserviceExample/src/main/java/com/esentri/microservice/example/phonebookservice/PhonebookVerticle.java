package com.esentri.microservice.example.phonebookservice;

import com.esentri.microservice.example.phonebookservice.entities.Entry;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;


/**
 * The {@class PhonebookVerticle} implements the phone book service.
 * It manages the phone book entries. Therefore this class can insert
 * new phone book entries to the corresponding table and display all stored entries. 
 * Also functions are provided to delete phone book entries from the table.
 * @author dominikgaller
 *
 */
public class PhonebookVerticle extends AbstractVerticle {
	
	/** Eventbus instance. */
	private EventBus eb;
	
	/** JDBCClient for shared async. data base connection. */
	private JDBCClient client;
	
	/** Name of the data source, here the shared data base. */
	private static final String SHAREDDBNAME = "entries";
	
	/** Address for phone book requests. */
	private static final String request = "esentri.entries.request";
	
	/** Address to display phone book entries. */
	private static final String display = "esentri.entries.display";
	
	/** Address for phone book entry deletion request. */
	private static final String delete = "esentri.entries.delete";
	
	/** Address to add a phone book entry. */
	private static final String add = "esentri.entries.add";
	
	private Logger l;
	
	/**
	 * This verticles start method.
	 */
	public void start() {
		l = LoggerFactory.getLogger(PhonebookVerticle.class);
		l.info("Starting PhonebookVerticle.");
		this.eb = vertx.eventBus();
		this.client = JDBCClient.createShared(vertx, new JsonObject(), SHAREDDBNAME);
		eb.consumer(request, this::displayEntriesHandler);
		eb.consumer(delete, this::deleteEntryHandler);
		eb.consumer(add, this::addEntryHandler);
	}
	
	/**
	 * Handler called by adding request.
	 * @param message the attached message.
	 */
	//TODO Insert error handling
	private void addEntryHandler(Message<String> message) {
		l.info("Request for creating new entry received.");
		l.info("Payload for creating new entry is: " + message.body());
		JsonObject msg = new JsonObject(message.body());
		String sessionId = extractSessionId(msg);
		Entry e = makeEntryFromJson(msg.getJsonObject("entry"));
		addEntry(e, sessionId);
	}
	
	/**
	 * Adds an entry to the data base. 
	 * @param e the entry to add.
	 * @param sessionId the clients session id.
	 */
	private void addEntry(Entry e, String sessionId) {
		this.client.getConnection(conn -> {
			if(conn.failed()) {
				throw new IllegalStateException(conn.cause().getMessage());
			}
			SQLConnection connection = conn.result();
			addEntrySQL(connection, e, sessionId);
		});
	}
	
	/**
	 * Executes the insert into sql query for adding a new entry.
	 * @param conn the SQLConnection for this transaction.
	 * @param e the entry to add
	 * @param sessionId the clients session id
	 */
	private void addEntrySQL(SQLConnection conn, Entry e, String sessionId) {
		conn.execute("insert into entry (name, number) values ('" + e.getName() + "','" + e.getNumber() + "')", rs -> {
			if(rs.failed()) {
				throw new RuntimeException(rs.cause().getMessage());
			}
			l.info("Entry added.");
			conn.close();
			internalRefreshDisplay(sessionId);
		});
	}

	/**
	 * Makes a entry from a given json object,
	 * @param json the json object with informations about the enty
	 * @return the entry
	 */
	//TODO insert error handling
	private Entry makeEntryFromJson(JsonObject json) {
		String name = json.getString("name");
		String number = json.getString("number");
		Entry e = new Entry(name, number);
		if(json.containsKey("id")) {
			e.setId(json.getLong("id"));
		}
		return e;
	}
	
	/**
	 * Handler which is called, when a delete entry request is fired.
	 * Manages deleting the entry and refreshes the clients view.
	 * @param message the message attached to the request.
	 */
	private void deleteEntryHandler(Message<String> message) {
		l.info("Request for deleting existing entry received.");
		l.info(message.body());
		JsonObject msg = new JsonObject(message.body());
		String sessionId = extractSessionId(msg);
		JsonObject jsonEntry = msg.getJsonObject("entry");
		deleteEntry(makeEntryFromJson(jsonEntry), sessionId);
	}
	
	/**
	 * Deletes an entry from the data base.
	 * @param e the entry to delete.
	 * @param sessionId the clients sessionId.
	 */
	private void deleteEntry(Entry e, String sessionId) {
		this.client.getConnection(conn -> {
			if(conn.failed()) {
				throw new IllegalStateException(conn.cause().getMessage());
			}
			SQLConnection connection = conn.result();
			deleteEntrySQL(connection, e, sessionId);
		});
	}
	
	/**
	 * Executes the sql command for deleting an entry.
	 * @param conn the SQLConnection for the transaction
	 * @param e the entry to delete
	 * @param sessionId the clients sessionId
	 */
	private void deleteEntrySQL(SQLConnection conn, Entry e, String sessionId) {
		conn.execute("delete from entry where id = " + e.getId(), rs -> {
			if(rs.failed()) {
				throw new RuntimeException(rs.cause().getMessage());
			}
			conn.close();
			//TODO insert user information
			eb.send("esentri.testreply", new JsonObject().put("succeeded", true).toString());
			internalRefreshDisplay(sessionId);
		});
	}
	
	/**
	 * Extracts the session id out of a given JsonObject.
	 * To pass a sessionId to this method, the sessionId key
	 * must be placed in the first level of the json object.
	 * E.g.:
	 * 	{
	 * 		...
	 * 		"sessionId" : sessionId,
	 * 		...
	 * 	}
	 * 
	 * @param json the json object to extract sessionId from.
	 * @return the sessionId if found.
	 */
	private String extractSessionId(JsonObject json) {
		System.out.println(json.toString());
		if(json.containsKey("sessionId")) {
			return json.getString("sessionId");
		} else {
			throw new IllegalArgumentException("Malformed session token.");
		}
	}
	
	/**
	 * Handler which is called, when a display entries request is fired.
	 * Manages forwarding of display to client.
	 * @param message the message attached to the request.
	 */
	private void displayEntriesHandler(Message<String> message) {
		l.info("Request for displaying entries received.");
		l.info("Payload for displaying entries is: " + message.body());
		String sessionId = extractSessionId(new JsonObject(message.body()));
		this.client.getConnection(conn -> {
			if(conn.failed()) {
				throw new IllegalStateException(conn.cause().getMessage());
			}
			SQLConnection connection = conn.result();
			selectAllEntries(connection, sessionId);
		});
	}
	
	/**
	 * Refreshes the displayed entries for a specific client, identified
	 * by the given session id. This method is used internal by the delete and
	 * add entry handlers, respectively their sub methods, to refresh the users
	 * view after modifying the data set.
	 * @param sessionId the clients session id.
	 */
	private void internalRefreshDisplay(String sessionId) {
		l.info("Entryset changed. Will refresh users view.");
		this.client.getConnection(conn -> {
			if(conn.failed()) {
				throw new IllegalStateException(conn.cause().getMessage());
			}
			SQLConnection connection = conn.result();
			selectAllEntries(connection, sessionId);
		});
	}
	
	/**
	 * Selects all entries from the entry table and sends the result
	 * set as JsonArray to the requesting client.
	 * @param conn SQLConnection for the transaction
	 */
	private void selectAllEntries(SQLConnection conn, String sessionId) {
		JsonArray reply = new JsonArray();
		conn.query("select * from entry", rs -> {
			if(rs.failed()) {
				throw new RuntimeException(rs.cause().getMessage());
			}
			for (JsonArray line: rs.result().getResults()) {
				reply.add(makeEntryJsonObjectFromSQLResult(line));
			}
			this.eb.send(display + ":" + sessionId, reply.toString());
			conn.close();
		});
	}
	
	/** 
	 * Makes an entry JsonObject for the SQL result array.
	 * @param sqlResult the result set JsonArray
	 * @return a JsonObject representing an entry
	 */
	private JsonObject makeEntryJsonObjectFromSQLResult(JsonArray sqlResult) {
		int id = sqlResult.getInteger(0);
		String name = sqlResult.getString(1);
		String number = sqlResult.getString(2);
		return new JsonObject()
		.put("id", id)
		.put("name", name)
		.put("number", number);
	}
}
