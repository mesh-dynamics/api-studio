/**
 * Copyright Cube I O
 */
package com.cube.core;

import static io.md.core.Comparator.Resolution.ERR;
import static io.md.core.Comparator.Resolution.ERR_ValTypeMismatch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.DiffFlags;
import com.flipkart.zjsonpatch.JsonDiff;

import io.cube.agent.UtilException;
import io.md.core.Comparator;
import io.md.core.CompareTemplate;
import io.md.core.CompareTemplate.DataType;
import io.md.core.CompareTemplate.PresenceType;
import io.md.core.TemplateEntry;
import io.md.dao.DataObj;
import io.md.dao.JsonDataObj;
import io.md.dao.LazyParseAbstractPayload;

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
	public JsonComparator(CompareTemplate template, ObjectMapper jsonMapper) {
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
		} catch (IOException e) {
		    LOGGER.error("Error in parsing json: " + lhs, e.getMessage()
                + " " + UtilException.extractFirstStackTraceLocation(e.getStackTrace()));
			return new Match(MatchType.Exception, e.getMessage(), result);
		}

		// Need to log properly which json parsing caused error
		try {
		    rhsroot = jsonMapper.readTree(rhs);
		} catch (IOException e) {
		    LOGGER.error("Error in parsing json: " + rhs, e.getMessage()
                + " " + UtilException.extractFirstStackTraceLocation(e.getStackTrace()));
		    return new Match(MatchType.Exception, e.getMessage(), result);
        }

		return compare(lhsroot, rhsroot);

	}

	private Match compare(JsonNode lhsRoot, JsonNode rhsRoot) {
        List<Diff> result = new ArrayList<>();

        // first validate the rhs (new json)
        validate(rhsRoot, result);

        Set<String> arrayPathsToReconstructLHS = new HashSet<>();
		Set<String> arrayPathsToReconstructRHS = new HashSet<>();
		Set<String> setPathsLHS = new HashSet<>();
		Set<String> setPathsRHS = new HashSet<>();

        //convert arrays to objects
		JsonNode lhsConverted, rhsConverted;
		try {
			 lhsConverted = ServerUtils.convertArrayToObject(lhsRoot, template, "", setPathsLHS
                 , arrayPathsToReconstructLHS, jsonMapper);
			 rhsConverted = ServerUtils.convertArrayToObject(rhsRoot, template, "", setPathsRHS
                 , arrayPathsToReconstructRHS, jsonMapper);
		 } catch (JsonProcessingException e) {
			LOGGER.error("Error in converting arrays to objects ", e);
			return new Match(MatchType.Exception, e.getMessage(), result);
		}

        // Now diff new (rhs) with the old (lhs)
        EnumSet<DiffFlags> flags = EnumSet.of(DiffFlags.OMIT_COPY_OPERATION,
            DiffFlags.OMIT_MOVE_OPERATION,
            DiffFlags.ADD_ORIGINAL_VALUE_ON_REPLACE);
        JsonNode patch = JsonDiff.asJson(lhsConverted, rhsConverted, flags);
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
                    // In case of remove, the library populates the removed value in diff.value
                    // whereas the diff.from value is empty. Hence handle remove case as a special one
                    diff.resolution = rule.rhsmissing();
                    break;
                default:
                    LOGGER.error("Unexpected op in diff, ignoring: " + diff.op);
            }
            if (diff.resolution.isErr()) {
                numerrs++;
            }

            // remove duplicates
            result.removeIf(d -> d.path.equalsIgnoreCase(diff.path)
	            && d.resolution == diff.resolution && Diff.NOOP.equals(d.op));
            result.add(diff);
        }

        String matchmeta = "JsonDiff";

        Set<String> pathsToReconstructUnion = new HashSet<>(arrayPathsToReconstructLHS);
        pathsToReconstructUnion.addAll(arrayPathsToReconstructRHS);

		TreeMap<String, Diff> diffTreeMap = new TreeMap<>();
		result.forEach(diffEntry -> diffTreeMap.put(diffEntry.path , diffEntry));

		List<Pair<String, Integer>> pathVsPathLength = new ArrayList<>();

		pathsToReconstructUnion.forEach(path -> pathVsPathLength.add(Pair.of(path , path.split("/").length)));

		pathVsPathLength.sort((o1, o2) -> Integer.compare(o2.getRight(), o1.getRight()));

        pathVsPathLength.forEach(path -> {
        	ServerUtils.reconstructArray(lhsConverted , rhsConverted, path.getLeft(),  diffTreeMap, jsonMapper);
        });

        MatchType mt = (numerrs > 0) ? MatchType.NoMatch :
            (diffs.length > 0) ? MatchType.FuzzyMatch : MatchType.ExactMatch;

        Set<String> setPathsUnion = new HashSet<>(setPathsLHS);
        setPathsUnion.addAll(setPathsRHS);


        return new Match(mt, matchmeta, result, new ArrayList<>(setPathsUnion),
            // Avoid replication by storing lhs and rhs objects only if modified
            // which would be the case only if there are some sets
            setPathsLHS.size() > 0 ? Optional.of(lhsConverted) : Optional.empty(),
            setPathsRHS.size() > 0 ? Optional.of(rhsConverted) : Optional.empty());
    }

    @Override
    public Match compare(DataObj lhs, DataObj rhs) {
        if ((lhs instanceof JsonDataObj) && (rhs instanceof JsonDataObj)) {
            JsonDataObj lhsObj = (JsonDataObj) lhs;
            JsonDataObj rhsObj = (JsonDataObj) rhs;
            return compare(lhsObj.getRoot(), rhsObj.getRoot());
        } if ((lhs instanceof LazyParseAbstractPayload) && (rhs instanceof LazyParseAbstractPayload)) {
		    LazyParseAbstractPayload lhsObj = (LazyParseAbstractPayload) lhs;
		    LazyParseAbstractPayload rhsObj = (LazyParseAbstractPayload) rhs;
		    lhsObj.parseIfRequired();
		    rhsObj.parseIfRequired();
		    //JsonDataObj rhsObj = (JsonDataObj) rhs;
		    return compare(lhsObj.dataObj.getRoot(), rhsObj.dataObj.getRoot());
	    } else {
            ObjectMessage objectMessage = new ObjectMessage(Map.of("message", "Payload not of json type in JsonComparator", "lhs",
                lhs.toString(), "rhs", rhs.toString()));
            List<Diff> result = new ArrayList<>();
            LOGGER.error(objectMessage);
            return new Match(MatchType.Exception, objectMessage.getFormattedMessage(), result);
        }
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
				String parentPath = rule.path.substring( 0, index );
				Optional<TemplateEntry> parentRule = template.get(JsonPointer.valueOf(parentPath));
				if (parentRule.isEmpty()) {
					JsonNode node = root.at(rule.pathptr.head());
					checkRptArrayTypes(node, resdiffs, rule.dt, parentPath);

				}
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
				boolean valTypeMismatch = false;
				boolean valFormatMismatch = false;
				switch (rule.dt) {
				case Str:
					if (!node.isTextual()) {
						valTypeMismatch = true;
					} else {
						// check for regex pattern match
						if (rule.em == CompareTemplate.ExtractionMethod.Regex) {
							String val = node.asText();
							valFormatMismatch = rule.regex.map(r -> !r.matcher(val).matches()).orElse(valFormatMismatch);
						}
					}
					break;
				case Float:
					if (!node.isFloat() && !node.isDouble() && !node.isInt() && !node.isLong()) valTypeMismatch = true;
					break;
				case Int:
					if (!node.isInt()) valTypeMismatch = true;
					break;
				case NrptArray:
					if (!node.isArray()) valTypeMismatch = true;
					break;
				case RptArray:
					if (!node.isArray()) valTypeMismatch = true;
					Optional<TemplateEntry> starRule = template.get(JsonPointer.valueOf(rule.path + "/*"));
					Optional<CompareTemplate.DataType> itemDataType = starRule.map(r -> r.dt)
							.or(() -> Optional.ofNullable(node.get(0)).map(this::getDataType));
					itemDataType.ifPresent(idt -> {
						checkRptArrayTypes(node, resdiffs, idt, rule.path);
					});
					break;
				case Obj:
					if (!node.isObject()) valTypeMismatch = true;
					break;
				default:
					break;
				}
				if (valTypeMismatch) {
					Diff diff = new Diff(Diff.NOOP, rule.path, node, ERR_ValTypeMismatch);
					resdiffs.add(diff);
				}
				if (valFormatMismatch) {
					Diff diff = new Diff(Diff.NOOP, rule.path, node, Resolution.ERR_ValFormatMismatch);
					resdiffs.add(diff);
				}
			}
		});
	}

	private void checkRptArrayTypes(JsonNode node, List<Diff> resdiffs, CompareTemplate.DataType dt, String parentPath) {
		for (int i = 0; i < node.size(); i ++) {
			if ((dt != DataType.Default) && (getDataType(node.get(i)) != dt)) {
				Diff diff = new Diff(Diff.NOOP, parentPath + "/" + i, node.get(i), ERR_ValTypeMismatch);
				resdiffs.add(diff);
			}
		}
	}

	private DataType getDataType(JsonNode node) {
		// TODO take care of null pointer exception here
		if (node.isTextual()) return DataType.Str;
		if (node.isInt()) return DataType.Int;
		if (node.isDouble() || node.isFloat() || node.isLong()) return DataType.Float;
		if (node.isObject()) return DataType.Obj;
		return DataType.Default;
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

        return fromValue.map(fromVal ->
            value.map(toVal -> {
                if (toVal.isTextual()) {
			    /* Don't do type check if ct is Ignore. So pass on to checkMatch
				if (!fromValue.map(JsonNode::isTextual).orElse(true)) {
					return ERR_ValTypeMismatch;
				}
				*/
				Optional<String> lhs = fromValue.flatMap(v -> {
                    return v.isTextual() ? Optional.of(v.asText()) : Optional.empty();
                });
				Optional<String> rhs = value.map(JsonNode::asText);
				return rule.checkMatch(lhs, rhs, DataType.Str);
			}
			if (toVal.isInt()) {
			    /* Don't do type check if ct is Ignore. So pass on to checkMatch
				if (!fromValue.map(JsonNode::isInt).orElse(true)) {
					return ERR_ValTypeMismatch;
				}
				*/
                    Optional<Integer> lhs = fromValue.flatMap(v -> {
                        return v.isInt() ? Optional.of(v.asInt()) : Optional.empty();
                    });
                    Optional<Integer> rhs = value.map(JsonNode::asInt);
                    return rule.checkMatch(lhs, rhs, DataType.Int);
                }
                if (toVal.isDouble() || toVal.isFloat() || toVal.isLong()) {
			    /* Don't do type check if ct is Ignore. So pass on to checkMatch
				if (!fromValue.map(fv -> fv.isDouble() || fv.isFloat() || fv.isLong()).orElse(true)) {
					return ERR_ValTypeMismatch;
				}
				*/
                    Optional<Double> lhs = fromValue.flatMap(v -> {
                        return (v.isDouble() || v.isFloat() || v.isLong() || v.isInt()) ?
                            Optional.of(v.asDouble()) : Optional.empty();
                    });
                    Optional<Double> rhs = value.map(JsonNode::asDouble);
                    return rule.checkMatch(lhs, rhs, DataType.Float);
                }
                if (toVal.isValueNode()) {
                    // Treat everything else as String
                    // TODO: revisit this later
                    Optional<String> lhs = fromValue.map(JsonNode::asText);
                    Optional<String> rhs = value.map(JsonNode::asText);
                    return rule.checkMatch(lhs, rhs, DataType.Str);
                } else {
                    // object or array. This can come in diff only if there is no corresponding object in the from
                    LOGGER.error("Internal error - this should never happen");
                    return ERR;

                }
                // this is the case when the rhs path is present, but the value is null
            }).orElseGet(rule::rhsmissing)).orElseGet(rule::lhsmissing);
	}


	/*
	  Check if any rules exist at level deeper than root
	 */
	public boolean shouldConsiderAsObj() {
	    // first check if root is an object
        boolean dtObj = template.get(JsonPointer.valueOf("")).map(rule -> rule.dt.isObj()).orElse(false);
        return dtObj || template.getRules().stream().filter(r -> !(r.path.isBlank() || r.path.equals("/"))).findAny().isPresent();

		//return !template.getRules().isEmpty();
	}

    @Override
	public CompareTemplate getCompareTemplate() {
	    return template;
    }

	private final CompareTemplate template;
	private final ObjectMapper jsonMapper;

	public static JsonComparator EMPTY_COMPARATOR = new JsonComparator(new CompareTemplate(), null);

	public static void main(String[] args) throws Exception {
		Config config = new Config();
		ObjectMapper jsonMapper = config.jsonMapper;
		ArrayNode root = jsonMapper.createArrayNode();
		ObjectNode elem = jsonMapper.createObjectNode();
		root.add(elem);
		elem.put("op", "ADD");
		elem.put("path", "/a");
		JsonNode val = jsonMapper.getNodeFactory().numberNode(1);
		elem.set("value", val);

		Diff diff = jsonMapper.treeToValue(elem, Diff.class);
		System.out.println(diff.toString());
		Diff[] diffarr = jsonMapper.treeToValue(root, Diff[].class);
		System.out.println(Arrays.toString(diffarr));
		System.out.println(jsonMapper.writeValueAsString(diff));
		System.out.println(jsonMapper.writeValueAsString(diffarr));

	}
}
