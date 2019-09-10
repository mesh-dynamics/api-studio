/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.dao;

import java.io.IOException;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-09-03
 */
public class JsonObj implements DataObj {

    private static final Logger LOGGER = LogManager.getLogger(JsonObj.class);


    JsonObj(String json, ObjectMapper jsonMapper) {
        this(jsonStrToObj(json, jsonMapper), jsonMapper);
    }

    private JsonObj(Optional<JsonNode> root, ObjectMapper jsonMapper) {
        this.objRoot = root;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public boolean isLeaf() {
        return objRoot.map(JsonNode::isValueNode).orElse(true);
    }

    @Override
    public boolean isEmpty() {
        return objRoot.isEmpty();
    }

    @Override
    public DataObj getVal(String path) {
        return new JsonObj(getNode(path), jsonMapper);
    }

    @Override
    public String getValAsString(String path) {
        return getNode(path).flatMap(this::nodeToString).orElse("");
    }

    @Override
    public String serialize() {
        return null;
    }

    private static Optional<JsonNode> jsonStrToObj(String json, ObjectMapper jsonMapper) {
        try {
            return Optional.ofNullable(jsonMapper.readTree(json));
        } catch (IOException e) {
            LOGGER.error("Not able to parse json: " + json);
            return Optional.empty();
        }
    }

    private Optional<JsonNode> getNode(String path) {
        return objRoot.map(obj -> obj.at(path))
            .filter(val -> !val.isMissingNode());
    }

    private Optional<String> nodeToString(JsonNode node) {
        try {
            return Optional.of(jsonMapper.writeValueAsString(node));
        } catch (JsonProcessingException e) {
            LOGGER.error("Error in converting json node to string: " + node.toString());
            return Optional.empty();
        }
    }


    private final Optional<JsonNode> objRoot;
    private final ObjectMapper jsonMapper;
}
