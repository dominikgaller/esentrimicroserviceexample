package com.esentri.microservice.example.sessionservice;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.sstore.ClusteredSessionStore;

/**
 * The class @code SessionVerticle implements a rudimentary 
 * user session management. The only use for this is,
 * that by design we decided, that every user entering the page
 * gets a unique session id. That might be a problem in productive use
 * but for a demo - yay.
 * 
 * @author dominikgaller
 *
 */
public class SessionVerticle extends AbstractVerticle {
	
	/** Address for sessionrequest. */
	private static final String SESSIONREQUEST = "esentri.session.request";
	
	/** Eventbus instance. */
	private EventBus eb;
	
	/** Session store. */
	private ClusteredSessionStore css;
	
	private Logger l;
	
	/**
	 * Startmethod for this verticle.
	 */
	public void start() {
		l = LoggerFactory.getLogger(SessionVerticle.class);
		l.info("Starting session service.");
		this.eb = vertx.eventBus();
		this.css = ClusteredSessionStore.create(this.vertx);
		this.eb.consumer(SESSIONREQUEST, this::sessionHandler);
	}
	
	/**
	 * Handler for a new session id.
	 * 
	 * @param msg messages payload.
	 */
	private void sessionHandler(Message<String> msg) {
		l.info("Request for new session id received.");
		JsonObject reply = new JsonObject().put("sessionId", this.css.createSession(1000000).id().toString());
		l.info("Answering imideatly with new session id. Payload is: " + reply.toString());
		msg.reply(reply);
	}

}
