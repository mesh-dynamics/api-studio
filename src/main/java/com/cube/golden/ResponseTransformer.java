package com.cube.golden;

import com.cube.utils.Constants;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.flipkart.zjsonpatch.JsonPatch;

import com.cube.dao.RRBase;

public class gitResponseTransformer {

    private final ObjectMapper jsonMapper;
    private static final Logger LOGGER = LogManager.getLogger(ResponseTransformer.class);

    public ResponseTransformer(ObjectMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /*
    * takes in Responses from the recording (golden) and replay and applies the operations given
    * returns the modified Response with the given collection id and a generated reqId
    */
    public Optional<String> transformResponse(String recordResponseBody,
                                              String replayResponseBody,
                                              List<ReqRespUpdateOperation> operationList) {
        JsonNode patch =
            preProcessUpdates(recordResponseBody, replayResponseBody, operationList);

        JsonNode recRoot = null;
        try {
            // if the recorded string is empty, creating an empty object so that the patch can be applied
            // the only case for which this doesn't work is when both the lhs and rhs are empty
            // in that case the lhs gets converted from an empty string to an empty object string "{}"
            recRoot = Optional.ofNullable(jsonMapper.readTree(recordResponseBody)).orElse(jsonMapper.createObjectNode());
        } catch (IOException e) {
            LOGGER.error("error reading JSON " + e.getMessage());
            return Optional.empty(); //
            // todo throw error
        }
        // todo separate out a method that takes a json and a patch to apply
        LOGGER.debug("PRE APPLYING PATCH :: " + recordResponseBody);
        LOGGER.debug(patch);
        // apply in place doesn't work when the recorded root is an empty node
        recRoot = JsonPatch.apply(patch, recRoot);
        String transformedRespBody = recRoot.toString();
        LOGGER.debug("POST APPLYING PATCH :: " + transformedRespBody);
        //JsonPatch.applyInPlace(patch, recRoot, EnumSet.of(CompatibilityFlags.MISSING_VALUES_AS_NULLS));
        return  Optional.of((transformedRespBody.equals("{}") && recordResponseBody.isEmpty()) ?
            recordResponseBody : transformedRespBody);

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
            LOGGER.error("Error While Parsing Rec Body as Json :: " + e.getMessage());// todo error handling
        }

        JsonNode repRoot = null;
        try {
            repRoot = jsonMapper.readTree(repBody);
        } catch (IOException e) {
            LOGGER.error("Error While Parsing Replay Body as Json :: " + e.getMessage());// todo error handling
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
        ReqRespUpdateOperation newop = new ReqRespUpdateOperation(operation.operationType,
            StringUtils.removeStart(operation.jsonpath, Constants.BODY_PATH));
        //String jsonpath = StringUtils.removeStart(operation.jsonpath, RRBase.BODYPATH);
        switch (newop.operationType) {
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
                JsonNode lval = (recRoot != null)? recRoot.at(newop.jsonpath) : MissingNode.getInstance();
                JsonNode rval = (repRoot != null)? repRoot.at(newop.jsonpath) : MissingNode.getInstance();
                JsonNode val = rval;
                // ?? todo: what if both not present, no-op? error?
                if (rval.isMissingNode()) { // (not the same as isNull)
                    // change operation type to remove
                    newop.operationType = OperationType.REMOVE;
                    val = null; // no need to specify value in remove
                } else if (lval.isMissingNode()) {
                    // change operation type to add
                    newop.operationType = OperationType.ADD;
                    // the value to apply is still right hand value, it's just the operation type would change
                    //val = rval;
                }
                newop.value = val;
                break;
            case REMOVE:

                // nothing to be done for 'remove'; perhaps validate the path?
                break;
        }
        return jsonMapper.valueToTree(newop);
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
