package fr.treeptik.fsms.order;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class OrderService {
	private static final String COLLECTION = "order";
	private final MongoClient mongo;
	
	public OrderService(MongoClient mongo) {
		this.mongo = mongo;
	}

	public void findByUser(RoutingContext rc) {
		if (rc.queryParam("user").size() != 1) {
			rc.fail(new IllegalArgumentException("a single user must be specified"));
			return;
		}
		
		String user = rc.queryParam("user").get(0);
		
		JsonObject query = new JsonObject()
				.put("user", user);
		
		mongo.find(COLLECTION, query, res -> {
			if (res.failed()) {
				rc.fail(res.cause());
			}
			
			JsonArray orders = new JsonArray(res.result());
			
			for (Object o : orders) {
				JsonObject order = (JsonObject) o;
				
				double total = 0d;
				for (Object i : order.getJsonArray("items")) {
					JsonObject item = (JsonObject) i;
					
					double price = item.getJsonObject("item").getDouble("price");
					double quantity = item.getDouble("quantity");
					total += price * quantity;
				}
				
				order.put("total", total);
			}
			
			rc.response().end(orders.encodePrettily());
		});
	}

	public void find(RoutingContext rc) {
		String id = rc.pathParam("id");
		JsonObject query = new JsonObject()
				.put("_id", id);
		
		mongo.findOne(COLLECTION, query, null, res -> {
			if (res.failed()) {
				rc.fail(res.cause());
			}
			
			rc.response().end(res.result().encodePrettily());
		});
	}
	
	public void addOrder(RoutingContext rc) {
		JsonObject order = rc.getBodyAsJson();
		
		mongo.save(COLLECTION, order, res -> {
			if (res.failed()) {
				rc.fail(res.cause());
			}
			
			res.result();
			
			rc.response().setStatusCode(201);
			rc.response().end();
		});
	}

	public void register(Router router) {
		router.route().handler(BodyHandler.create());
		router.get("/orders").handler(this::findByUser);
		router.post("/orders").handler(this::addOrder);
		router.get("/orders/:id").handler(this::find);
	}
}
