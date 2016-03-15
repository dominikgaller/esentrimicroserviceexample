package com.esentri.microserviceExample;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class DatabaseTest {
	
	private static EventBus eb;
	private static Vertx vertx;
	private static final String VERTICLE = "com.esentri.microserviceexample.databaseService.DatabaseVerticle";
	
	@BeforeClass
	public static void start(TestContext context) {
		vertx = Vertx.vertx();
		vertx.deployVerticle(VERTICLE, new DeploymentOptions().setWorker(true));
		eb = vertx.eventBus();
	}
	
	@Test
	public void deleteHandlerTest(TestContext context) {
		Async async = context.async();
		
		JsonObject expectedReply = new JsonObject().put("succeeded", true);
		JsonObject generatedObject = generateTestObject();
		eb.consumer("esentri.testreply", onreply -> {
			context.assertEquals(expectedReply.toString(), onreply.body());
			async.complete();
		});
		eb.send("esentri.entries.delete", generatedObject.toString());
	}
	
	private JsonObject generateTestObject() {
		JsonObject entry = new JsonObject();
		entry.put("id", 111);
		entry.put("name", "Malermeister");
		entry.put("number", "1234-1234");
		
		JsonObject json = new JsonObject().put("entry", entry);
		eb.send("esentri.entries.add", json.toString());
		
		return json;
	}
	
	@AfterClass
	public static void stop(TestContext context) {
		vertx.undeploy(VERTICLE);
	}
}
