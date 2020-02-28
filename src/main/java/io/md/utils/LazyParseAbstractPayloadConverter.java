package io.md.utils;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;

import io.md.dao.DataObj.DataObjProcessingException;
import io.md.dao.DataObj.PathNotFoundException;
import io.md.dao.LazyParseAbstractPayload;

public class LazyParseAbstractPayloadConverter implements Converter<LazyParseAbstractPayload, LazyParseAbstractPayload> {

	@Override
	public LazyParseAbstractPayload convert(LazyParseAbstractPayload lazyParseAbstractPayload) {
		try {
			lazyParseAbstractPayload.syncFromDataObj();
		} catch (PathNotFoundException | DataObjProcessingException e) {
			e.printStackTrace();
		}
		return lazyParseAbstractPayload;
	}

	@Override
	public JavaType getInputType(TypeFactory typeFactory) {
		return typeFactory.constructType(LazyParseAbstractPayload.class);
	}

	@Override
	public JavaType getOutputType(TypeFactory typeFactory) {
		return typeFactory.constructType(LazyParseAbstractPayload.class);
	}
}
