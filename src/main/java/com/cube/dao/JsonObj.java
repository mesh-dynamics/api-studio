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
import java.util.function.Function;

import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.cube.agent.UtilException;

import com.cube.core.Comparator;
import com.cube.core.CompareTemplate;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-09-03
 */
public class JsonObj implements DataObj {

    private static final Logger LOGGER = LogManager.getLogger(JsonObj.class);

    public JsonObj(String json, ObjectMapper jsonMapper) {
        this(jsonStrToObj(json, jsonMapper), jsonMapper);
    }

    private JsonObj(JsonNode root, ObjectMapper jsonMapper) {
        this.objRoot = root;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public boolean isLeaf() {
        return objRoot.isValueNode() || objRoot.isMissingNode();
    }

    @Override
    public boolean isEmpty() {
        return objRoot.isMissingNode();
    }

    @Override
    public DataObj getVal(String path) {
        return new JsonObj(getNode(path), jsonMapper);
    }

    @Override
    public String getValAsString(String path) throws PathNotFoundException {
        JsonNode node = getNode(path);
        if (!node.isMissingNode()) {
            return nodeToString(getNode(path));
        } else {
            throw new PathNotFoundException();
        }
    }

    @Override
    public String serialize() {
        // TODO: Not yet implemented
        return null;
    }

    @Override
    public String toString() {
        try {
            return jsonMapper.writeValueAsString(objRoot);
        } catch (JsonProcessingException e) {
            LOGGER.error(new ObjectMessage(Map.of("message", "Not able to serialize json",
                "value", objRoot.toString())));
            return objRoot.toString();
        }
    }

    @Override
    public void collectKeyVals(Function<String, Boolean> filter, Collection<String> vals) {
        // Using json pointer to handle proper escaping in case keys have special characters
        JsonPointer path = JsonPointer.compile("");
        processNode(objRoot, filter, vals, path);
    }

    @Override
    public Comparator.MatchType compare(DataObj rhs, CompareTemplate template) {

        return null;
    }

    /**
     * Unwrap the string at path into a json object. The type for interpreting the string is given by mimetype
     * @param path
     * @param mimetype
     */
    public boolean unwrapAsJson(String path, String mimetype) {
        return unwrapAsJson(objRoot, path, mimetype);
    }

    private boolean unwrapAsJson(JsonNode root, String path, String mimetype) {
        JsonPointer pathPtr = JsonPointer.compile(path);
        JsonNode valParent = root.at(pathPtr.head());
        if (valParent != null &&  valParent.isObject()) {
            ObjectNode valParentObj = (ObjectNode) valParent;
            String fieldName = pathPtr.last().getMatchingProperty();
            JsonNode val = valParentObj.get(fieldName);
            if (val != null && val.isTextual()) {
                // parse it as per mime type
                // currently handling only json type
                if (mimetype.equals(MediaType.APPLICATION_JSON)) {
                    try {
                        JsonNode parsedVal = jsonMapper.readTree(val.asText());
                        valParentObj.set(fieldName, parsedVal);
                        return true;
                    } catch (IOException e) {
                        LOGGER.error(String.format("Exception in parsing json string at path: %s, val: %s",
                            path, val.asText()), e.getMessage(), UtilException.extractFirstStackTraceLocation(e.getStackTrace()));
                    }
                }
            }
        }
        return false;
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

    private static JsonNode jsonStrToObj(String json, ObjectMapper jsonMapper) {
        try {
            return jsonMapper.readTree(json);
        } catch (IOException e) {
            LOGGER.error("Not able to parse json: " + json);
            return MissingNode.getInstance();
        }
    }

    private JsonNode getNode(String path) {
        return objRoot.at(path);
    }

    private String nodeToString(JsonNode node) {
        try {
            if (node.isTextual()) {
                return node.asText();
            }
            return jsonMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error in converting json node to string: " + node.toString());
            return node.toString();
        }
    }


    private final JsonNode objRoot;
    private final ObjectMapper jsonMapper;

    public JsonNode getRoot() {
        return objRoot;
    }
}
