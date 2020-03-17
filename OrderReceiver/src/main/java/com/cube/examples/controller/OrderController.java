package com.cube.examples.controller;

import java.net.URI;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.constants.Constants;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.cube.examples.dao.OrdersDAO;
import com.cube.examples.model.Order;
import com.cube.examples.model.Orders;

@RestController
@RequestMapping(path = "/orders")
public class OrderController {

	//public static final String URL = "http://order-transformer:9080/enhanceAndSendForProcessing/";
	public static final String URL = "http://localhost:8081/enhanceAndSendForProcessing/";

	@Autowired
	private OrdersDAO ordersDao;

//	@Autowired
//	private OkHttpClient httpClient;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ObjectMapper jacksonObjectMapper;

	private static final Logger LOGGER = LogManager.getLogger(OrderController.class);

	@GetMapping(path = "/getOrders", produces = "application/json")
	public Orders getOrders() {
		LOGGER.info("getOrders call Received");
		return ordersDao.getAllOrders();
	}

	@PostMapping(path = "/postOrder", consumes = "application/json", produces = "application/json")
	public ResponseEntity<Object> placeOrders(@RequestBody Order order)
		throws Exception {
		//Generate resource id
		Integer id = ordersDao.getAllOrders().getOrderList().size() + 1;
		order.setId(id);

		//add resource
		ordersDao.placeOrder(order);

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(org.springframework.http.MediaType.APPLICATION_JSON));
		HttpEntity<Order> entity = new HttpEntity<Order>(order, headers);

		// send it to order transformer
		// send for processing

		ResponseEntity response = restTemplate.exchange(URL, HttpMethod.POST, entity, String.class);

			int code = response.getStatusCodeValue();
			if (code >= 200 && code <= 299) {
				LOGGER.info("Response code Received :" + code);
				//Create resource location
//				URI location = UriComponentsBuilder.fromHttpRequest(serverHttpRequest)
//					.path("/{id}")
//					.buildAndExpand(order.getId())
//					.toUri();
				//Send location in response
				return ResponseEntity.ok().build();
			} else {
				LOGGER.info("Response Received :" + response.toString());
				throw new IllegalArgumentException(
					"HTTP error response returned by Transformer service " + code);
			}

	}
}
