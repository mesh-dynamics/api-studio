package com.cube.golden;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.flipkart.zjsonpatch.JsonPatch;

public class JsonTransformer {

    private final ObjectMapper jsonMapper;
    private static final Logger LOGGER = LogManager.getLogger(JsonTransformer.class);

    public JsonTransformer(ObjectMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /*
    * takes in Responses from the recording (golden) and replay and applies the operations given
    * returns the modified Response with the given collection id and a generated reqId
    */
    public JsonNode transformResponse(JsonNode lhsRoot, JsonNode rhsRoot, List<ReqRespUpdateOperation> operationList) {
        JsonNode patch =
            preProcessUpdates(lhsRoot, rhsRoot, operationList);

        LOGGER.debug("PRE APPLYING PATCH :: " + lhsRoot);
        LOGGER.debug(patch);
        JsonNode transformedRoot = JsonPatch.apply(patch, lhsRoot);
        String transformedRespBody = transformedRoot.toString();
        LOGGER.debug("POST APPLYING PATCH :: " + transformedRespBody);
        return  transformedRoot;
    }


    // create a json patch to be applied using the patch library
    private ArrayNode preProcessUpdates(JsonNode lhsRoot, JsonNode rhsRoot,
                                        List<ReqRespUpdateOperation> updates) {
        // aim: to create patch json
        // how: populate the operations with the right value (and path?)
        // return: list of JsonNode objects to be used by the patch library

        LOGGER.debug("pre-processing operations");

        // create the patch as an array of JsonNodes
        ArrayNode patch = new ArrayNode(new JsonNodeFactory(false));
        updates.stream()
            .map(operation -> processOperation(lhsRoot, rhsRoot, operation))
            .forEach(patch::add);
        return patch;
    }


    private JsonNode processOperation(JsonNode recRoot, JsonNode repRoot,
                                      ReqRespUpdateOperation operation) {
        switch (operation.operationType) {
            // todo: check existence of value at path
            case ADD:
                // This case gets dealt in REPLACE
            case REPLACE:
                // there could be cases where the operation has been specified as 'replace', but all the response
                // bodies in the api path might not have both values present.
                // hence the following conditions may occur which would require changing the operation type/value
                // left: record; right: replay
                // if no value on left side, add
                // if no value on right side, delete
                // if both values present, replace
                JsonNode lval = (recRoot != null)? recRoot.at(operation.jsonpath) : MissingNode.getInstance();
                JsonNode rval = (repRoot != null)? repRoot.at(operation.jsonpath) : MissingNode.getInstance();
                JsonNode val = rval;
                // ?? todo: what if both not present, no-op? error?
                if (rval.isMissingNode()) { // (not the same as isNull)
                    // change operation type to remove
                    operation.operationType = OperationType.REMOVE;
                    val = null; // no need to specify value in remove
                } else if (lval.isMissingNode()) {
                    // change operation type to add
                    operation.operationType = OperationType.ADD;
                    // the value to apply is still right hand value, it's just the operation type would change
                    //val = rval;
                }
                operation.value = val;
                break;
            case REMOVE:

                // nothing to be done for 'remove'; perhaps validate the path?
                break;
        }
        return jsonMapper.valueToTree(operation);
    }


}
