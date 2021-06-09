package com.cube.examples.controller;

import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.cube.examples.model.EnhancedOrder;

@RestController
@RequestMapping(path = "/processEnhancedOrders")
public class OrderProcessorController
{

    @Autowired
    private ObjectMapper jacksonObjectMapper;

    private static final Logger LOGGER = LogManager.getLogger(OrderProcessorController.class);

    @PostMapping(path= "/", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Object> enhanceAndProcessOrder(
                        @RequestBody EnhancedOrder order)
                 throws Exception  {
        String orderJsonStr = jacksonObjectMapper.writeValueAsString(order);
        LOGGER.info("Order came for processing " + orderJsonStr);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(order.getId())
            .toUri();
        //Send location in response
        return ResponseEntity.created(location).body(order); //send input as body for demo
    }
}
