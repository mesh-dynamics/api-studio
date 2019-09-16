/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.dao;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonPointer;
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

    @Override
    public void collectKeyVals(Function<String, Boolean> filter, Collection<String> vals) {
        // Using json pointer to handle proper escaping in case keys have special characters
        JsonPointer path = JsonPointer.compile("");
        objRoot.ifPresent(root -> {
            processNode(root, filter, vals, path);
        });
    }

    private void processNode(JsonNode node, Function<String, Boolean> filter, Collection<String> vals, JsonPointer path) {
        if (node.isValueNode()) {
            if (filter.apply(path.toString())) {
                vals.add(node.asText());
            }
        } else if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> child = fields.next();
                processNode(child.getValue(), filter, vals, path.append(JsonPointer.compile("/" + child.getKey())));
            }
        } else if (node.isArray()) {
            int idx = 0;
            for (JsonNode child : node) {
                processNode(child, filter, vals, path.append(JsonPointer.compile("/" + idx)));
                idx++;
            }
        }
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
