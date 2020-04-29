package io.md.utils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.flipkart.zjsonpatch.JsonPatch;

import io.md.dao.ReqRespUpdateOperation;
import io.md.dao.ReqRespUpdateOperation.OperationType;

public class JsonTransformer {


	private final ObjectMapper jsonMapper;
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonTransformer.class);

	public JsonTransformer(ObjectMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	/*
	 * takes in objects from the recording (golden) and replay and applies the operations given
	 * returns the modified object
	 */
	public JsonNode transform(JsonNode lhsRoot, JsonNode rhsRoot,
		List<ReqRespUpdateOperation> operationList) {
		JsonNode patch =
			preProcessUpdates(lhsRoot, rhsRoot, operationList);

		LOGGER.debug("PRE APPLYING PATCH : data : ".concat(lhsRoot.toString())
				.concat(" , patch : ").concat(patch.toString()));
		JsonNode transformedRoot = JsonPatch.apply(patch, lhsRoot);
		String transformedRespBody = transformedRoot.toString();
		LOGGER.debug("POST APPLYING PATCH : data : ".concat(transformedRespBody));
		return transformedRoot;
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
			.flatMap(node -> node.map(Stream::of).orElseGet(Stream::empty))
			.forEach(patch::add);
		return patch;
	}


	private Optional<JsonNode> processOperation(JsonNode recRoot, JsonNode repRoot,
		ReqRespUpdateOperation operation) {
		ReqRespUpdateOperation updateOperation = new ReqRespUpdateOperation(operation.operationType,
			operation.jsonpath);
		// there could be cases where the operation has been specified as 'replace', but all the response
		// bodies in the api path might not have both values present.
		// hence the following conditions may occur which would require changing the operation type/value
		// left: record; right: replay
		// if no value on left side, add
		// if no value on right side, delete
		// if both values present, replace
		JsonNode lval =
			(recRoot != null) ? recRoot.at(operation.jsonpath) : MissingNode.getInstance();
		JsonNode rval =
			(repRoot != null) ? repRoot.at(operation.jsonpath) : MissingNode.getInstance();
		JsonNode val = rval;
		if (rval.isMissingNode()) { // (not the same as isNull)
			if (lval.isMissingNode()) {
				// both rhs and lhs are absent -- do nothing
				return Optional.empty();
			}
			// rhs is absent and lhs is present -- remove
			updateOperation.operationType = OperationType.REMOVE;
			val = null; // no need to specify value in remove
		} else if (lval.isMissingNode()) {
			// rhs is present but lhs is absent -- add
			updateOperation.operationType = OperationType.ADD;
		} else {
			// both values are present -- replace
			updateOperation.operationType = OperationType.REPLACE;
		}

		updateOperation.value = val;
		return Optional.of(jsonMapper.valueToTree(updateOperation));
	}


}

