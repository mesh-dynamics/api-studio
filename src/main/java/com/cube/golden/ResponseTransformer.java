package com.cube.golden;

import com.cube.dao.Response;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.flipkart.zjsonpatch.JsonPatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class ResponseTransformer {

    private final ObjectMapper jsonMapper;
    private static final Logger LOGGER = LogManager.getLogger(ResponseTransformer.class);

    public ResponseTransformer(ObjectMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /*
    * takes in Responses from the recording (golden) and replay and applies the operations given
    * returns the modified Response with the given collection id and a generated reqid
    */
    public Response transformResponse(Optional<Response> recordResponse,
                                      Optional<Response> replayResponse,
                                      List<ReqRespUpdateOperation> operationList, String collectionName) {
        Optional<Response> transformResponse =
            recordResponse.flatMap(
                recResponse -> replayResponse.flatMap(
                    repResponse -> {

                        JsonNode patch =
                            preProcessUpdates(recResponse.body, repResponse.body, operationList);

                        JsonNode recRoot = null;
                        try {
                            recRoot = jsonMapper.readTree(recResponse.body);
                        } catch (IOException e) {
                            LOGGER.error("error reading JSON " + e.getMessage());
                            return Optional.empty(); //
                            // todo throw error
                        }
                        // todo separate out a method that takes a json and a patch to apply
                        LOGGER.debug("applying patch");
                        LOGGER.debug(recRoot.toString());
                        LOGGER.debug(repResponse.body);
                        LOGGER.debug(patch);
                        JsonPatch.applyInPlace(patch, recRoot);
                        LOGGER.debug(recRoot.toString());

                        Optional<String> reqId = generateReqId(recResponse.reqid, collectionName);
                        Instant timeStamp = Instant.now();

                        Response transformedResponse = new Response(reqId, repResponse.status,
                            repResponse.meta, repResponse.hdrs, recRoot.toString(), Optional.of(collectionName),
                            Optional.of(timeStamp), repResponse.rrtype, repResponse.customerid, repResponse.app);

                        return Optional.of(transformedResponse);
                    }
                )
            );
        return transformResponse.orElse(null); // todo
    }

    private Optional<String> generateReqId(Optional<String> recReqId, String collectionName) {
        return recReqId.map(
            reqid -> "gu-" + Objects.hash(reqid, collectionName));
    }

    // create a json patch to be applied using the patch library
    private ArrayNode preProcessUpdates(String recBody, String repBody, List<ReqRespUpdateOperation> updates) {
        // aim: to create patch json
        // how: populate the operations with the right value (and path?)
        // return: list of JsonNode objects to be used by the patch library

        LOGGER.debug("pre-processing operations");
        JsonNode recRoot = null;
        try {
            recRoot = jsonMapper.readTree(recBody);
        } catch (IOException e) {
            e.printStackTrace(); // todo error handling
        }

        JsonNode repRoot = null;
        try {
            repRoot = jsonMapper.readTree(repBody);
        } catch (IOException e) {
            e.printStackTrace(); // todo error handling
        }

        JsonNode finalRepRoot = repRoot;
        // create the patch as an array of JsonNodes
        ArrayNode patch = new ArrayNode(new JsonNodeFactory(false));
        JsonNode finalRecRoot = recRoot;
        updates.stream()
            .map(operation -> processOperation(finalRecRoot, finalRepRoot, operation))
            .forEach(patch::add);
        return patch;
    }

    private JsonNode processOperation(JsonNode recRoot, JsonNode repRoot,
                                      ReqRespUpdateOperation operation) {
        switch (operation.operationType) {
            // todo: check existence of value at path
            case ADD:
                // get the value to be added from the replay body
                operation.value = repRoot.at(operation.jsonpath);
                break;
            case REPLACE:
                // there could be cases where the operation has been specified as 'replace', but all the response
                // bodies in the api path might not have both values present.
                // hence the following conditions may occur which would require changing the operation type/value
                // left: record; right: replay
                // if no value on left side, delete
                // if no value on right side, add
                // if both values present, replace
                JsonNode lval = recRoot.at(operation.jsonpath);
                JsonNode rval = repRoot.at(operation.jsonpath);
                JsonNode val = rval;
                // ?? todo: what if both not present, no-op? error?
                if (rval.isMissingNode()) { // (not the same as isNull)
                    // change operation type to add
                    operation.operationType = OperationType.ADD;
                    val = lval;
                } else if (lval.isMissingNode()) {
                    // change operation type to remove
                    operation.operationType = OperationType.REMOVE;
                    val = null; // no need to specify value in remove
                }
                operation.value = val;
                break;
            case REMOVE:
                // nothing to be done for 'remove'; perhaps validate the path?
                break;
        }
        return jsonMapper.valueToTree(operation);
    }

//    private Object transform(String recBody, String repBody, ReqRespUpdateOperation operation) {
//        // convert to tree
//        // go to the path
//        // add/delete/replace the value
//
//        Configuration.setDefaults(new Configuration.Defaults() {
//            private final JsonProvider jsonProvider = new JacksonJsonProvider();
//            private final MappingProvider mappingProvider = new JacksonMappingProvider();
//
//            @Override
//            public JsonProvider jsonProvider() {
//                return jsonProvider;
//            }
//
//            @Override
//            public MappingProvider mappingProvider() {
//                return mappingProvider;
//            }
//
//            @Override
//            public Set<Option> options() {
//                return EnumSet.noneOf(Option.class);
//            }
//        });
//
//        ParseContext parseContext = JsonPath.using(Configuration.defaultConfiguration());
//
//        DocumentContext recCtx = null;
//        try {
//            recCtx = parseContext.parse(recBody);
//        } catch (Exception e) {
//            e.printStackTrace(); // todo
//        }
//
//        ReadContext repCtx = null;
//        try {
//            repCtx = parseContext.parse(repBody);
//        } catch (Exception e) {
//            e.printStackTrace(); // todo
//        }
//
//        String jsonPathStr = operation.jsonpath;
//
//
//        switch (operation.operationType){
//
//            case ADD:
//                Object val = repCtx.read(jsonPathStr);
//
//                int i = jsonPathStr.lastIndexOf('.');
//                String parentPath = jsonPathStr.substring(0, i);
//                String key = jsonPathStr.substring(i);
//
//                Object parentContainer = recCtx.read(parentPath);
//                //recCtx.add(parentPath, val);
//                Class<?> aClass = parentContainer.getClass();
//                if (aClass == ArrayNode.class){
//
//                }
//                break;
//            case REPLACE:
//                Object val = repCtx.read(jsonPathStr);
//                recCtx.set(jsonPathStr, val);
//                break;
//            case DELETE:
//                recCtx.delete(jsonPathStr);
//                break;
//        }
//        return null;
//    }

}
