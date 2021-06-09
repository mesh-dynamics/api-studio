/*
 *
 *    Copyright Cube I O
 *
 */

/*
 * Created by IntelliJ IDEA.
 * Date: 31/07/20
 */
package com.cube.ws;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author
 * This was needed for json support for java 8 objects in jackson
 * set default for jersey using @Provider. Don't remove this class
 */
@Provider
public class CubeObjectMapperProvider implements ContextResolver<ObjectMapper> {

    final ObjectMapper defaultObjectMapper;

    public CubeObjectMapperProvider() {
        defaultObjectMapper = io.md.utils.CubeObjectMapperProvider.getInstance();
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return defaultObjectMapper;
    }
}

