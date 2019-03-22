/**
 * Copyright Cube I O
 */
package com.cube.core;

import static com.cube.core.Comparator.Resolution.*;

import java.io.IOException;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.DiffFlags;
import com.flipkart.zjsonpatch.JsonDiff;

import com.cube.core.CompareTemplate.ComparisonType;
import com.cube.core.CompareTemplate.PresenceType;
import com.cube.ws.Config;

/**
 * @author prasad
 *
 */
public class JsonComparator implements Comparator {

    private static final Logger LOGGER = LogManager.getLogger(JsonComparator.class);


	/**
	 * @param template
	 * @param jsonMapper
	 */
	JsonComparator(CompareTemplate template, ObjectMapper jsonMapper) {
		super();
		this.template = template;
		this.jsonMapper = jsonMapper;
	}
	
	/* (non-Javadoc)
	 * @see com.cube.core.Comparator#compare(java.lang.String, java.lang.String)
	 */
	@Override
	public Match compare(String lhs, String rhs) {
		
		JsonNode lhsroot;
		JsonNode rhsroot;

		List<Diff> result = new ArrayList<>();
		try {
			lhsroot = jsonMapper.readTree(lhs);
			rhsroot = jsonMapper.readTree(rhs);
		} catch (IOException e) {
			LOGGER.error("Error in parsing json: " + lhs, e);
			return new Match(MatchType.Exception, e.getMessage(), result);
		}
		
		// first validate the rhs (new json)
		validate(rhsroot, result);
		
		
		// Now diff new (rhs) with the old (lhs)
		EnumSet<DiffFlags> flags = EnumSet.of(DiffFlags.OMIT_COPY_OPERATION, 
				DiffFlags.OMIT_MOVE_OPERATION,
				DiffFlags.ADD_ORIGINAL_VALUE_ON_REPLACE);
		JsonNode patch = JsonDiff.asJson(lhsroot, rhsroot, flags);
		Diff[] diffs;
		
		try {
			diffs = jsonMapper.treeToValue(patch, Diff[].class);
		} catch (JsonProcessingException e) {
			LOGGER.error("Error in parsing diffs: " + patch.toString(), e);
			return new Match(MatchType.Exception, e.getMessage(), result);
		}
		
		int numerrs = result.size();
		for (var diff : diffs) {
			TemplateEntry rule = template.getRule(diff.path);
			
			switch (diff.op) {
			case Diff.ADD:
			case Diff.REPLACE:
				diff.resolution = matchVals(rule, diff.fromValue, diff.value);
				break;
			case Diff.REMOVE:
				if (rule.pt == PresenceType.Optional) {
					diff.resolution = Resolution.OK_Optional;
				} else if (rule.pt == PresenceType.Default) {
					diff.resolution = Resolution.OK;
				} else {
					diff.resolution = Resolution.ERR_Required;
				}
				break;
			default: 
				LOGGER.error("Unexpected op in diff, ignoring: " + diff.op);
				
			}
			if (diff.resolution.isErr()) {
				numerrs++;
			}
			result.add(diff);
		}
		
		String matchmeta = "JsonDiff";
		MatchType mt = (numerrs > 0) ? MatchType.NoMatch :
			(diffs.length > 0) ? MatchType.FuzzyMatch : MatchType.ExactMatch;
		return new Match(mt, matchmeta, result);
	}

	/**
	 * @param root
	 * @param resdiffs
	 * Validate if any required element are missing, validate data types and formats
	 */
	private void validate(JsonNode root, List<Diff> resdiffs) {
		template.getRules().forEach(rule -> {

			int index = rule.path.lastIndexOf('/');
			if (index != -1 && rule.path.substring( index + 1 ).equalsIgnoreCase("*")){
				return;
			}
			JsonNode node = root.at(rule.pathptr);
			if (node.isMissingNode()) {
				if (rule.pt == PresenceType.Required) {
					Diff diff = new Diff(Diff.NOOP, rule.path, node, Resolution.ERR_Required);
					resdiffs.add(diff);
				}				
			} else {
				// validate data type
				boolean valtypemismatch = false;
				boolean valformatmismatch = false;
				switch (rule.dt) {
				case Str:
					if (!node.isTextual()) {
						valtypemismatch = true;
					} else {
						// check for regex pattern match
						if (rule.ct == ComparisonType.CustomRegex) {
							String val = node.asText();
							valformatmismatch = rule.regex.map(r -> !r.matcher(val).matches()).orElse(valformatmismatch);
						}
					}
					break;
				case Float:
					if (!node.isFloat() && !node.isDouble() && !node.isInt()) valtypemismatch = true;
					break;
				case Int:
					if (!node.isInt()) valtypemismatch = true;
					break;
				case NrptArray:
				case RptArray:
					if (!node.isArray()) valtypemismatch = true;
					break;
				case Obj:
					if (!node.isObject()) valtypemismatch = true;
					break;
				default:
					break;
				}
				if (valtypemismatch) {
					Diff diff = new Diff(Diff.NOOP, rule.path, node, ERR_ValTypeMismatch);
					resdiffs.add(diff);
				}								
				if (valformatmismatch) {
					Diff diff = new Diff(Diff.NOOP, rule.path, node, Resolution.ERR_ValFormatMismatch);
					resdiffs.add(diff);
				}								
			}
		});
	}
	
	/**
	 * @param rule
	 * @param fromValue
	 * @param value
	 * @return
	 */
	static private Resolution matchVals(TemplateEntry rule,
										Optional<JsonNode> fromValue,
										Optional<JsonNode> value) {

		return value.map(toVal -> {
			if (toVal.isTextual()) {
				if (!fromValue.map(JsonNode::isTextual).orElse(true)) {
					return ERR_ValTypeMismatch;
				}
				Optional<String> lhs = fromValue.map(JsonNode::asText);
				Optional<String> rhs = value.map(JsonNode::asText);
				return rule.checkMatchStr(lhs, rhs);
			}
			if (toVal.isInt()) {
				if (!fromValue.map(JsonNode::isInt).orElse(true)) {
					return ERR_ValTypeMismatch;
				}
				Optional<Integer> lhs = fromValue.map(JsonNode::asInt);
				Optional<Integer> rhs = value.map(JsonNode::asInt);
				return rule.checkMatchInt(lhs, rhs);
			}
			if (toVal.isDouble() || toVal.isFloat()) {
				if (!fromValue.map(fv -> fv.isDouble() || fv.isFloat()).orElse(true)) {
					return ERR_ValTypeMismatch;
				}
				Optional<Double> lhs = fromValue.map(JsonNode::asDouble);
				Optional<Double> rhs = value.map(JsonNode::asDouble);
				return rule.checkMatchDbl(lhs, rhs);
			}
			if (toVal.isValueNode()) {
				// Treat everything else as String
				// TODO: revisit this later
				Optional<String> lhs = fromValue.map(JsonNode::asText);
				Optional<String> rhs = value.map(JsonNode::asText);
				return rule.checkMatchStr(lhs, rhs);
			} else {
				// object or array. This can come in diff only if there is no corresponding object in the from
				if (fromValue.isPresent()) {
					LOGGER.error("Internal error - this should never happen");
					return ERR;
				} else {
					return rule.lhsmissing();
				}
			}
		}).orElseGet(() -> {
			LOGGER.error("Internal error - this should never happen");
			return ERR;
		});
	}

	private final CompareTemplate template;
	private final ObjectMapper jsonMapper;
	
	public static void main(String[] args) throws Exception {
		Config config = new Config();
		ObjectMapper jsonmapper = config.jsonmapper;
		ArrayNode root = jsonmapper.createArrayNode();
		ObjectNode elem = jsonmapper.createObjectNode();
		root.add(elem);
		elem.put("op", "ADD");
		elem.put("path", "/a");
		JsonNode val = jsonmapper.getNodeFactory().numberNode(1);
		elem.set("value", val);
		
		Diff diff = jsonmapper.treeToValue(elem, Diff.class);
		System.out.println(diff.toString());
		Diff[] diffarr = jsonmapper.treeToValue(root, Diff[].class);
		System.out.println(Arrays.toString(diffarr));
		System.out.println(jsonmapper.writeValueAsString(diff));
		System.out.println(jsonmapper.writeValueAsString(diffarr));

	}
}
