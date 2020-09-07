package io.md.utils;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.lookup.StringLookup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.md.dao.DataObj;
import io.md.dao.DataObj.PathNotFoundException;
import io.md.dao.Event;
import io.md.dao.Payload;
import io.md.services.DataStore;

public class InjectionVarResolver implements StringLookup {

	private static Logger LOGGER = LogManager.getLogger(InjectionVarResolver.class);

	Event goldenRequestEvent;
	Payload testResponsePayload;
	Payload testRequestPayload;
	DataStore dataStore;

	public InjectionVarResolver(Event goldenRequestEvent, Payload testResponsePayload,
                                Payload testRequestPayload, DataStore dataStore) {
		this.goldenRequestEvent = goldenRequestEvent;
		this.testResponsePayload = testResponsePayload;
		this.testRequestPayload = testRequestPayload;
		this.dataStore = dataStore;
	}

	public ExtractionInfo getSourcePayloadAndJsonPath(String lookupString) {
		String[] splitStrings = lookupString.split(":");
		if (splitStrings.length < 2 || splitStrings.length > 3) {
			LOGGER.error("Lookup String format mismatch");
			return null; // Null resorts to default variable in substitutor
		}
		String source = splitStrings[0].trim();
		String jsonPath = splitStrings[1].trim();

		String regularExpression = null;
		if (splitStrings.length == 3) {
			regularExpression = splitStrings[2].trim();
		}

		Payload sourcePayload;
		switch (source) {
			case Constants.GOLDEN_REQUEST:
				sourcePayload = goldenRequestEvent.payload;
				break;
			case Constants.GOLDEN_RESPONSE:
				Optional<Event> goldenResponseOptional = dataStore
					.getResponseEvent(goldenRequestEvent.reqId);
				if (goldenResponseOptional.isEmpty()) {
					LOGGER.error("Cannot fetch golden response for golden request");
					return null; // Null resorts to default variable in substitutor
				}
				sourcePayload = goldenResponseOptional.get().payload;
				break;
			case Constants.TESTSET_RESPONSE:
				sourcePayload = testResponsePayload;
				break;
			case Constants.TESTSET_REQUEST:
				sourcePayload = testRequestPayload;
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + source);
		}

		return new ExtractionInfo(sourcePayload, jsonPath, regularExpression);
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
		if (extractionInfo == null) return null;
		String regex = extractionInfo.regex;
		try {
			value = extractionInfo.source.getValAsString(extractionInfo.jsonPath);
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
			LOGGER.error("Cannot find JSONPath" + extractionInfo.jsonPath + " in source", e);
			return null;
		}
		return value;
	}

	class ExtractionInfo {

		public ExtractionInfo(Payload source, String jsonPath, String regex) {
			this.source = source;
			this.jsonPath = jsonPath;
			this.regex = regex;
		}

		public final Payload source;
		public final String jsonPath;
		public final String regex;
	}
}
