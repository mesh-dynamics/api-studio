/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.dao;

import java.nio.charset.StandardCharsets;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.cube.ws.Config;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-09-06
 */
public class DataObjFactory {

    private static final Logger LOGGER = LogManager.getLogger(DataObjFactory.class);

    public static final String HTTP_CONTENT_TYPE_PATH = "/hdrs/content-type/0";

    public static DataObj build(Event.EventType type, byte[] payloadBin, String payloadStr, Config config) {

        switch (type) {
            case HTTPRequest:
            case HTTPResponse:
                JsonObj obj = new JsonObj(payloadStr, config.jsonMapper);
                String mimeType = MediaType.TEXT_PLAIN;
                try {
                    mimeType = obj.getValAsString(HTTP_CONTENT_TYPE_PATH);
                } catch (DataObj.PathNotFoundException e) {
                    LOGGER.info("Content-type not found, using default of TEXT_PLAIN for payload: " + payloadStr);
                }
                obj.unwrapAsJson("/body", mimeType);
                return obj;
            case JavaRequest:
            case JavaResponse:
                return new JsonObj(payloadStr, config.jsonMapper);
            case ThriftRequest:
            case ThriftResponse:
            case ProtoBufRequest:
            case ProtoBufResponse:
            default:
                throw new NotImplementedException("Thrift and Protobuf not implemented");
        }

        //return null;

    }

}
