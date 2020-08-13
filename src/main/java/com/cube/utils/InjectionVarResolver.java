package com.cube.utils;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
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
	String source;
	String jsonPath;
	String regularExpression;
	Payload sourcePayload;

	public InjectionVarResolver(Event goldenRequestEvent, Payload testResponsePayload,
                                Payload testRequestPayload, DataStore dataStore) {
		this.goldenRequestEvent = goldenRequestEvent;
		this.testResponsePayload = testResponsePayload;
		this.testRequestPayload = testRequestPayload;
		this.dataStore = dataStore;
	}

	public void getSourcePayloadAndJsonPath(String lookupString) {
		String[] splitStrings = lookupString.split(":");
		if (splitStrings.length < 2 || splitStrings.length > 3) {
			LOGGER.error("Lookup String format mismatch");
			//return null; // Null resorts to default variable in substitutor
		}
		source = splitStrings[0].trim();
		jsonPath = splitStrings[1].trim();

		if (splitStrings.length == 3) {
			regularExpression = splitStrings[2].trim();
		}

		//Payload sourcePayload;
		switch (source) {
			case Constants.GOLDEN_REQUEST:
				sourcePayload = goldenRequestEvent.payload;
				break;
			case Constants.GOLDEN_RESPONSE:
				Optional<Event> goldenResponseOptional = dataStore
					.getResponseEvent(goldenRequestEvent.reqId);
				if (goldenResponseOptional.isEmpty()) {
					LOGGER.error("Cannot fetch golden response for golden request");
					//return null; // Null resorts to default variable in substitutor
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
		// new ImmutablePair<>(sourcePayload, jsonPath);
	}

	public DataObj lookupObject(String lookupString) {
		getSourcePayloadAndJsonPath(lookupString);
		DataObj value;
		value = sourcePayload.getVal(jsonPath);
		return value;
	}

	@Override
	/** Lookup String will always be in format of
	 "VariableSources: <JSONPath> <regular expression>
	 **/
	public String lookup(String lookupString) {
		String value = null;
		getSourcePayloadAndJsonPath(lookupString);
		try {
			value = sourcePayload.getValAsString(jsonPath);
			if (regularExpression != null && !regularExpression.isEmpty()) {
				Matcher match = Pattern.compile(regularExpression).matcher(value);
				if (match.find()) {
					value = match.group("mdgroup");
				}
			}
		} catch (PathNotFoundException e) {
			LOGGER.error("Cannot find JSONPath" + jsonPath + " in source", e);
			return null;
		}
		return value;
	}
}
