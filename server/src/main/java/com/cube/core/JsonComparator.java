/**
 * Copyright Cube I O
 */
package com.cube.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cube.core.JsonCompareTemplate.ComparisonType;
import com.cube.core.JsonCompareTemplate.PresenceType;
import com.cube.core.JsonCompareTemplate.TemplateEntry;
import com.cube.ws.Config;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.DiffFlags;
import com.flipkart.zjsonpatch.JsonDiff;

/**
 * @author prasad
 *
 */
public class JsonComparator implements Comparator {

    private static final Logger LOGGER = LogManager.getLogger(JsonComparator.class);

    public enum Resolution {
    	OK,
    	OK_Optional,
    	OK_Ignore,
    	OK_CustomMatch,
    	OK_OtherValInvalid, // the val to compare against does not exist or is not of right type
    	ERR_NotExpected, // This indicates that presence type is required and either the new or the old object does not have the value
    	ERR_Required,
    	ERR_ValMismatch,
    	ERR_ValTypeMismatch,
    	ERR_ValFormatMismatch,
    	ERR; 

		/**
		 * @return
		 */
		public boolean isErr() {
			return this == Resolution.ERR_NotExpected || this == ERR_Required || 
					this == ERR_ValMismatch || this == ERR_ValTypeMismatch || 
					this == ERR_ValFormatMismatch || this == ERR;
		}

    }
    
    /**
     * This class captures the information that the json diff library produces
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Diff {    	    	
    	
        static final String ADD = "add";
        static final String REMOVE = "remove";
        static final String REPLACE = "replace";
        static final String MOVE = "move";
        static final String COPY = "copy";
        static final String TEST = "test";
        static final String NOOP = "noop"; // used for validation errors
        
		/**
		 * Needed for jackson
		 */
		@SuppressWarnings("unused")
		private Diff() {
			super();
			value = null;
			path = null;
			op = null;
			fromValue = Optional.empty();
			from = Optional.empty();
			resolution = Resolution.ERR;
		}



		/**
		 * @param op
		 * @param path
		 * @param value
		 * @param resolution
		 */
		public Diff(String op, String path, JsonNode value, Resolution resolution) {
			super();
			this.op = op;
			this.path = path;
			this.value = value;
			this.resolution = resolution;
			from = Optional.empty();
			fromValue = Optional.empty();
		}



		public final String op;
	    public final String path;
	    public final JsonNode value;
	    public final Optional<String> from; //only to be used in move operation
	    public final Optional<JsonNode> fromValue; // only used in replace operation
	    public Resolution resolution;
    }
    
    
    
	/**
	 * @param template
	 * @param jsonmapper
	 */
	public JsonComparator(JsonCompareTemplate template, ObjectMapper jsonmapper) {
		super();
		this.template = template;
		this.jsonmapper = jsonmapper;
	}
	
	/* (non-Javadoc)
	 * @see com.cube.core.Comparator#compare(java.lang.String, java.lang.String)
	 */
	@Override
	public Match compare(String lhs, String rhs) {
		
		JsonNode lhsroot;
		JsonNode rhsroot;
		
		try {
			lhsroot = jsonmapper.readTree(lhs);
			rhsroot = jsonmapper.readTree(rhs);
		} catch (IOException e) {
			LOGGER.error("Error in parsing json: " + lhs, e);
			return new Match(MatchType.Exception, e.getMessage());
		}
		
		List<Diff> result = new ArrayList<Diff>();
		// first validate the rhs (new json)
		validate(rhsroot, result);
		
		
		// Now diff new (rhs) with the old (lhs)
		EnumSet<DiffFlags> flags = EnumSet.of(DiffFlags.OMIT_COPY_OPERATION, 
				DiffFlags.OMIT_MOVE_OPERATION,
				DiffFlags.ADD_ORIGINAL_VALUE_ON_REPLACE);
		JsonNode patch = JsonDiff.asJson(lhsroot, rhsroot, flags);
		Diff[] diffs = null;
		
		try {
			diffs = jsonmapper.treeToValue(patch, Diff[].class);
		} catch (JsonProcessingException e) {
			LOGGER.error("Error in parsing diffs: " + patch.toString(), e);
			return new Match(MatchType.Exception, e.getMessage());
		}
		
		int numerrs = result.size();
		for (Diff diff : diffs) {
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
		
		String matchmeta;
		try {
			matchmeta = jsonmapper.writeValueAsString(result);
		} catch (JsonProcessingException e) {
			LOGGER.error("Error in writing diffs: " + patch.toString(), e);
			return new Match(MatchType.Exception, e.getMessage());
		}
		MatchType mt = (numerrs > 0) ? MatchType.NoMatch : 
			(diffs.length > 0) ? MatchType.FuzzyMatch : MatchType.ExactMatch;
		return new Match(mt, matchmeta);
	}

	/**
	 * @param root
	 * @param resdiffs
	 * Validate if any required element are missing, validate data types and formats
	 */
	private void validate(JsonNode root, List<Diff> resdiffs) {
		template.getRules().forEach(rule -> {
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
						String val = node.asText();
						valformatmismatch = rule.regex.map(r -> r.matcher(val).matches()).orElse(valformatmismatch);
					}
					break;
				case Float:
					if (!node.isFloat() && !node.isDouble()) valtypemismatch = true;
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
					Diff diff = new Diff(Diff.NOOP, rule.path, node, Resolution.ERR_ValTypeMismatch);
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
	 * @param r.ct
	 * @param fromValue
	 * @param value
	 * @return
	 */
	static private Resolution matchVals(TemplateEntry rule, 
			Optional<JsonNode> fromValue, 
			JsonNode value) {
		
		switch (rule.ct) {
		case CustomRegex:
			if (!value.isTextual())
				return Resolution.ERR_ValTypeMismatch;
			String rhs = value.asText();
			
			Pattern pattern = rule.regex.orElseGet(() -> {
				LOGGER.error("Internal logical error - compiled pattern missing for regex");
				return Pattern.compile(rule.customization.orElse(".*"));	
			});
			
			Matcher matcher = pattern.matcher(rhs);
			if (!matcher.matches()) {
				return Resolution.ERR_ValFormatMismatch;
			}
			return fromValue.map(fv -> {
				if (!fv.isTextual()) {
					return Resolution.OK_OtherValInvalid;
				}
				String lhs = fv.asText();
				Matcher lhsmatcher = pattern.matcher(lhs);
				if (!lhsmatcher.matches()) {
					return Resolution.OK_OtherValInvalid;
				}
				if (matcher.groupCount() != lhsmatcher.groupCount()) {
					return Resolution.ERR_ValMismatch;
				}
				for (int i=0; i<matcher.groupCount(); ++i) {
					if (!matcher.group(i).equals(lhsmatcher.group(i))) {
						return Resolution.ERR_ValMismatch;
					}
				}
				return Resolution.OK_CustomMatch;
			}).orElse(rule.pt == PresenceType.Default ? Resolution.ERR_NotExpected : Resolution.OK_OtherValInvalid);
		case CustomFloor:
		case CustomCeil:
		case CustomRound:
			if (!value.isDouble() & !value.isFloat()) {
				return Resolution.ERR_ValMismatch;
			}
			return fromValue.map(lhs -> {
				if (!lhs.isDouble() && !lhs.isFloat()) {
					return Resolution.ERR_ValMismatch;
				}
				double lval = lhs.asDouble();
				double rval = value.asDouble();
				int numdecimal = rule.customization.flatMap(Utils::strToInt).orElse(0);
				double lval1 = adjustDblVal(rule.ct, lval, numdecimal);
				double rval1 = adjustDblVal(rule.ct, rval, numdecimal);
				return (lval1 == rval1) ? Resolution.OK_CustomMatch : Resolution.ERR_ValMismatch;
			}).orElse(rule.pt == PresenceType.Default ? Resolution.ERR_NotExpected : Resolution.OK_OtherValInvalid);
		case Equal:
			return fromValue.map(lhs -> {
				// here values are not actually compared. 
				// If prev value is not empty, then just the fact that it appeared in a diff 
				// indicates that the value changed. 	
				return Resolution.ERR_ValMismatch;
			}).orElse(rule.pt == PresenceType.Default ? Resolution.ERR_NotExpected : Resolution.OK_OtherValInvalid);
		case Ignore:
			return Resolution.OK_Ignore;
		default:
			break;
		}
		return Resolution.OK; // this can happen if compare type is not specified
	}

	private static double adjustDblVal(ComparisonType ct, double val, int numdecimal) {
		double multiplier = Math.pow(10, numdecimal);
		switch(ct) {
		case CustomCeil:
			return Math.ceil(val * multiplier)/multiplier;
		case CustomFloor:
			return Math.floor(val * multiplier)/multiplier;
		case CustomRound:
			return Math.round(val * multiplier)/multiplier;
		default:
			return val;
		}
	}
	
	final JsonCompareTemplate template;
	final ObjectMapper jsonmapper;
	
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
		System.out.println(diffarr);
		System.out.println(jsonmapper.writeValueAsString(diff));
		System.out.println(jsonmapper.writeValueAsString(diffarr));
	}
}
