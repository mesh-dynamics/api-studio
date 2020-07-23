package com.cube.utils;

import java.util.Optional;

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

	public InjectionVarResolver(Event goldenRequestEvent, Payload testResponsePayload,
                                Payload testRequestPayload, DataStore dataStore) {
		this.goldenRequestEvent = goldenRequestEvent;
		this.testResponsePayload = testResponsePayload;
		this.testRequestPayload = testRequestPayload;
		this.dataStore = dataStore;
	}

	public Pair<Payload, String> getSourcePayloadAndJsonPath(String lookupString) {
		String[] splitStrings = lookupString.split(":");
		if (splitStrings.length != 2) {
			LOGGER.error("Lookup String format mismatch");
			return null; // Null resorts to default variable in substitutor
		}
		String source = splitStrings[0].trim();
		String jsonPath = splitStrings[1].trim();
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
		return new ImmutablePair<>(sourcePayload, jsonPath);
	}

	public DataObj lookupObject(String lookupString) {
		Pair<Payload, String> pair = getSourcePayloadAndJsonPath(lookupString);
		Payload sourcePayload = pair.getLeft();
		String jsonPath = pair.getRight();
		DataObj value;
		value = sourcePayload.getVal(jsonPath);
		return value;
	}

	@Override
	/** Lookup String will always be in format of
	 "VariableSources: <JSONPath>
	 **/
	public String lookup(String lookupString) {
		String value = null;
		Pair<Payload, String> pair = getSourcePayloadAndJsonPath(lookupString);
		Payload sourcePayload = pair.getLeft();
		String jsonPath = pair.getRight();
		try {
			value = sourcePayload.getValAsString(jsonPath);
		} catch (PathNotFoundException e) {
			LOGGER.error("Cannot find JSONPath" + jsonPath + " in source", e);
			return null;
		}
		return value;
	}
}
