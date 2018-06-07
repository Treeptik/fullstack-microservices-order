package fr.treeptik.fsms.order;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class OrderVerticle extends AbstractVerticle {
	private MongoClient mongo;
	private OrderService service;
	private JsonObject config;
	
	@Override
	public void start() throws Exception {
		ConfigRetriever retriever = ConfigRetriever.create(vertx);
		
		retriever.getConfig(res -> {
			if (res.failed()) {
				throw new RuntimeException(res.cause());
			}
			
			config = res.result();
			
			startMongoClient();
			createOrderService();
			startHttpServer(router());
		});
	}
	
	private void startMongoClient() {
		mongo = MongoClient.createShared(vertx, config);
	}

	private void createOrderService() {
		service = new OrderService(mongo);
	}

	private Router router() {
		Router router = Router.router(vertx);
		
		service.register(router);
		router.route("/*").handler(StaticHandler.create());
		
		return router;
	}
	
	private void startHttpServer(Router router) {
		vertx.createHttpServer()
		.requestHandler(router::accept)
		.listen(config.getInteger("server.port", 8080),
				res -> {
					if (res.failed()) {
						throw new RuntimeException(res.cause());
					}
				});
	}
}
