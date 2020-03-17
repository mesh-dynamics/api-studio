package com.cube.examples.controller;

import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import reactor.core.publisher.Mono;

import com.cube.examples.dao.OrdersDAO;
import com.cube.examples.model.EnhancedOrder;
import com.cube.examples.model.Order;

@RestController
@RequestMapping(path = "/enhanceAndSendForProcessing")
public class OrderTransformerController {

	private static final Logger LOGGER = LogManager.getLogger(OrderTransformerController.class);

	@Autowired
	private OrdersDAO ordersDao;

	@Autowired
	private WebClient webClient;

	@PostMapping(path = "/", consumes = "application/json", produces = "application/json")
	public Mono<ResponseEntity<String>> enhanceAndProcessOrder(@RequestBody Order order)
		throws Exception {
		//add resource
		EnhancedOrder enhancedOrder = ordersDao.enhanceOrder(order);

		//send for processing
		Mono<ResponseEntity<String>> result = webClient.post()
			.uri("/processEnhancedOrders/")
			.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
			.accept(org.springframework.http.MediaType.APPLICATION_JSON)
			.body(Mono.just(enhancedOrder), EnhancedOrder.class)
			.exchange()
			.flatMap(response -> response.toEntity(String.class))
			.flatMap(entity -> {
				int code = entity.getStatusCodeValue();
				if (code >= 200 && code <= 299) {
					LOGGER.info("Response code Received :" + code);
					//Create resource location
//					URI location = UriComponentsBuilder.fromHttpRequest(serverHttpRequest)
//						.path("/{id}")
//						.buildAndExpand(order.getId())
//						.toUri();
					//Send location in response
					return Mono.just(ResponseEntity.ok().build());
				} else {
					LOGGER.info("Response Received :" + entity.toString());
					throw new IllegalArgumentException(
						"HTTP error response returned by Processor service " + code);
				}
			});

		return result;

	}
}