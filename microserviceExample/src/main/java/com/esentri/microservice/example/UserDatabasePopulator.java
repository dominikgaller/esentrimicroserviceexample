package com.esentri.microservice.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;

/**
 * The class UserDatabasePopulator provides simple
 * mechanisms and implementations, to populate an user database
 * and insert some default tuples, defined in an json file.
 * 
 * @author dominikgaller
 *
 */
public class UserDatabasePopulator {
	
	/** Vert.x Instance to create the shared database. */
	private Vertx vertxInstance;
	
	/** The name for the data base. */
	private String dbName;

	/** Path to users.json file, where initial users for the user db table are stored. */
	private static final String USERSPATH = "src/main/resources/users.json";
	
	/**
	 * Constructor for a new EntryDatabasePopulator instance.
	 * @param vertx the vertx context
	 * @param dbname the data base name
	 */
	public UserDatabasePopulator(Vertx vertx, String dbname) {
		this.setVertxInstance(vertx);
		this.setDBName(dbname);
	}
	
	/**
	 * Populates the database.
	 */
	public void populateDatabase() {
		initDBClient();
	}

	/**
	 * Initializes the JDBCClient, and establishes a connection to the data
	 * base.
	 * 
	 * @param vertx
	 *            this verticles vertx instance.
	 */
	private void initDBClient() {
		final JDBCClient client = JDBCClient.createShared(this.vertxInstance, createConfig(), this.dbName);
		client.getConnection(conn -> {
			if (conn.failed()) {
				throw new IllegalStateException(conn.cause().getMessage());
			}
			setUpDB(conn);
		});
	}

	/**
	 * Fills the database with entry table and content.
	 * 
	 * @param conn
	 */
	private void setUpDB(AsyncResult<SQLConnection> conn) {
		createUserDB(conn);
		insertUsers(conn);
	}

	/**
	 * Creates the user table.
	 * @param conn AsyncReslut<SQLConnection> SqlConnection handler
	 */
	private void createUserDB(AsyncResult<SQLConnection> conn) {
		final SQLConnection connection = conn.result();
		connection.execute("create table user(id integer identity primary key, name varchar(255), password varchar(255))", res -> {
			if(res.failed()) {
				throw new IllegalStateException(conn.cause().getMessage());
			}
			connection.close();
		});
	}

	/**
	 * Inserts users into the data base. The users are defined in the users.json file.
	 * @param conn AsyncReslut<SQLConnection> SqlConnection handler
	 */
	private void insertUsers(AsyncResult<SQLConnection> conn) {
		JsonArray arr = readJsonArrayFile(USERSPATH);
		arr.forEach(elem -> { 
			JsonObject val = new JsonObject(elem.toString());
			insertUser(conn, val.getString("name"), val.getString("password"));
		});
	}
	
	/**
	 * Inserts a single user into the user table.
	 * @param conn the SQLConnection for the transaction
	 * @param name the users name
	 * @param password the users password
	 */
	private void insertUser(AsyncResult<SQLConnection> conn, String name, String password) {
		SQLConnection connection = conn.result();
		connection.execute("insert into user (name, password) values ('" + name + "','" + password + "')", res -> {
			if(res.failed()) {
				throw new IllegalStateException(res.cause().getMessage());
			}
			connection.close();
		});
	}


	// TODO Insert error handling :D
	/**
	 * Reads a file into a json array by using non blocking io. FYI: Does no
	 * error handling. Put in clean json array and get one out. Do something
	 * else and no guarantees what will happen :-)
	 * 
	 * @param path
	 *            the path to the file to read.
	 * @return the json array representing the file content.
	 */
	private JsonArray readJsonArrayFile(String path) {
		try {
			String content = new String(Files.readAllBytes(Paths.get(path)));
			return new JsonArray(content);
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}

	/**
	 * Creates the configuration for the data base.
	 * 
	 * @return a json object defining the configuration.
	 */
	private JsonObject createConfig() {
		JsonObject config = new JsonObject().put("url", "jdbc:hsqldb:mem:" + this.dbName + "?shutdown=false")
				.put("driver_class", "org.hsqldb.jdbcDriver").put("max_pool_size", 30);
		return config;
	}

	/**
	 * @return the vertxInstance
	 */
	public Vertx getVertxInstance() {
		return vertxInstance;
	}

	/**
	 * @param vertxInstance
	 *            the vertxInstance to set
	 */
	public void setVertxInstance(Vertx vertxInstance) {
		this.vertxInstance = vertxInstance;
	}

	/**
	 * @return the dBNAME
	 */
	public String getDBName() {
		return this.dbName;
	}

	/**
	 * @param dBNAME
	 *            the dBNAME to set
	 */
	public void setDBName(String dbName) {
		this.dbName = dbName;
	}

}
