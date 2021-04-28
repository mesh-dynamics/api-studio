package io.md.injection;

import io.md.dao.DataObj;
import io.md.dao.DataObj.PathNotFoundException;
import io.md.dao.Event;
import io.md.dao.Payload;
import io.md.logger.LogMgr;
import io.md.services.DataStore;
import io.md.constants.Constants;
import org.apache.commons.text.lookup.StringLookup;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InjectionVarResolver implements StringLookup {

	public static final String VAL_PLACEHOLDER = "_VAL";
	public static final String MATCHEDVAL_PLACEHOLDER = "_MATCHEDVAL";
	public static final String PATH_PLACEHOLDER = "_PATH";

	private static Logger LOGGER = LogMgr.getLogger(InjectionVarResolver.class);

	Event goldenRequestEvent;
	Payload testResponsePayload;
	Payload testRequestPayload;
	DataStore dataStore;

	private String _valResolved = null;
	private String _matchedValResolved = null;
	private String _pathResolved = null;

	public String get_valResolved() {
		return _valResolved;
	}

	public void set_valResolved(String _valResolved) {
		this._valResolved = _valResolved;
	}

	public String get_matchedValResolved() {
		return _matchedValResolved;
	}

	public void set_matchedValResolved(String _matchedValResolved) {
		this._matchedValResolved = _matchedValResolved;
	}

	public String get_pathResolved() {
		return _pathResolved;
	}

	public void set_pathResolved(String _pathResolved) {
		this._pathResolved = _pathResolved;
	}

	public InjectionVarResolver(Event goldenRequestEvent, Payload testResponsePayload,
                                Payload testRequestPayload, DataStore dataStore) {
		this.goldenRequestEvent = goldenRequestEvent;
		this.testResponsePayload = testResponsePayload;
		this.testRequestPayload = testRequestPayload;
		this.dataStore = dataStore;
	}

	public Payload getPayload(String sourceString) {
		Payload payload = null;
		switch (sourceString) {
			case Constants.GOLDEN_REQUEST:
				payload = goldenRequestEvent.payload;
				break;
			case Constants.GOLDEN_RESPONSE:
				Optional<Event> goldenResponseOptional = dataStore
					.getResponseEvent(goldenRequestEvent.reqId);
				if (!goldenResponseOptional.isPresent()) {
					LOGGER.error("Cannot fetch golden response for golden request");
					return null; // Null resorts to default variable in substitutor
				}
				payload = goldenResponseOptional.get().payload;
				break;
			case Constants.TESTSET_RESPONSE:
				payload = testResponsePayload;
				break;
			case Constants.TESTSET_REQUEST:
				payload = testRequestPayload;
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + sourceString);
		}
		return payload;
	}

	public ExtractionInfo getSourcePayloadAndJsonPath(String lookupString) {
		Payload sourcePayload = null;
		String jsonPath = null;
		Optional<String> resolvedValue = Optional.empty();
		String regularExpression = null;
		// TODO : Add support for iterating over objects and not only values.
		// _VAL/_MATCHEDVAL could itself be a source to be resolved. In this case collectKeyVals in event would return Map<string, DataObj>
		// "foreach": {source: Golden.Response, match: TestSet.Response, path: /body/.+/vals/.+, keys: [key1, key2]}
		//"name": "${_VAL}: /id}_record_ids",
		//"value": "${_MATCHEDVAL: /id}",
		switch (lookupString) {
			case VAL_PLACEHOLDER:
				resolvedValue = Optional.ofNullable(get_valResolved());
				break;
			case MATCHEDVAL_PLACEHOLDER:
				resolvedValue = Optional.ofNullable(get_matchedValResolved());
				break;
			case PATH_PLACEHOLDER:
				resolvedValue = Optional.ofNullable(get_pathResolved());
				break;
			default:
				String[] splitStrings = lookupString.split(":");
				if (splitStrings.length < 2 || splitStrings.length > 3) {
					LOGGER.error("Lookup String format mismatch");
					return null; // Null resorts to default variable in substitutor
				}
				String source = splitStrings[0].trim();
				jsonPath = splitStrings[1].trim();

				if (splitStrings.length == 3) {
					regularExpression = splitStrings[2].trim();
				}

				sourcePayload = getPayload(source);
		}

		if(sourcePayload==null && jsonPath==null && !resolvedValue.isPresent()) return null;
		return new ExtractionInfo(sourcePayload, jsonPath, regularExpression, resolvedValue);
	}

	public DataObj lookupObject(String lookupString) {
		ExtractionInfo extractionInfo = getSourcePayloadAndJsonPath(lookupString);
		if (extractionInfo == null) return null;
		DataObj value;
		value = extractionInfo.source.getVal(extractionInfo.jsonPath);
		return value;
	}

	@Override
	/** Lookup String will always be in format of
	 "VariableSources: <JSONPath> <regular expression>
	 **/
	public String lookup(String lookupString) {
		String value = null;
		ExtractionInfo extractionInfo =  getSourcePayloadAndJsonPath(lookupString);
		if (extractionInfo == null) return null; //Nothing got resolved
		else if(extractionInfo.resolvedValue.isPresent()) // Case of _val, _path, _mathchedval
		{
			return extractionInfo.resolvedValue.get();
		}

		String regex = extractionInfo.regex;
		try {
			if (extractionInfo.source != null) {
				// For cases such as devtool reqs, source = golden.response is missing leading to
				// NULL ptr exception and default value being skipped.
				value = extractionInfo.source.getValAsString(extractionInfo.jsonPath);
			} else {
				return null;
			}
			if (regex != null && !regex.isEmpty()) {
				Matcher match = Pattern.compile(regex).matcher(value);
				if (match.find()) {
					if (match.groupCount() == 0) value = match.group();
					else {
						value = "";
						for (int i=1 ; i<= match.groupCount() ; i++) {
							value = value.concat(match.group(i));
						}
					}
				}
			}
		} catch (PathNotFoundException e) {
			LOGGER.error("Cannot find JSONPath" + extractionInfo.jsonPath + " in source");
			return null;
		}
		return value;
	}

	class ExtractionInfo {

		public ExtractionInfo(Payload source, String jsonPath, String regex,
			Optional<String> resolvedValue) {
			this.source = source;
			this.jsonPath = jsonPath;
			this.regex = regex;
			this.resolvedValue = resolvedValue;
		}

		public final Payload source;
		public final String jsonPath;
		public final String regex;
		public final Optional<String> resolvedValue;
	}
}
