/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.cube.utils.Constants;
import com.cube.ws.Config;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-09-06
 */
public class DataObjFactory {

    private static final Logger LOGGER = LogManager.getLogger(DataObjFactory.class);

    // Http headers are case insensitive
    public static final List<String> HTTP_CONTENT_TYPE_PATHS = List.of("/hdrs/content-type/0", "/hdrs/Content-type/0", "/hdrs/Content-Type/0", "/hdrs/content-Type/0");

    public static DataObj build(Event.EventType type, byte[] payloadBin, String payloadStr,
        Config config, Map<String, Object> params) {

        switch (type) {
            case HTTPRequest:
            case HTTPResponse:
                JsonDataObj obj = new JsonDataObj(payloadStr, config.jsonMapper);
                String mimeType = MediaType.TEXT_PLAIN;
                boolean defaultMimeType = true;

                for (String HTTP_CONTENT_TYPE_PATH : HTTP_CONTENT_TYPE_PATHS) {
                    try {
                        mimeType = obj.getValAsString(HTTP_CONTENT_TYPE_PATH);
                        defaultMimeType = false;
                    } catch (DataObj.PathNotFoundException e) {
                        LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Content-type not found for field " + HTTP_CONTENT_TYPE_PATH)));
                    }
                }
                if(defaultMimeType) {
                    LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Content-type not found, using default of TEXT_PLAIN")));
                }
                obj.unwrapAsJson(Constants.BODY_PATH, mimeType);
                return obj;
            case JavaRequest:
            case JavaResponse:
                return new JsonDataObj(payloadStr, config.jsonMapper);
            case ThriftRequest:
            case ThriftResponse:
                return new ThriftDataObject.ThriftDataObjectBuilder().build(payloadBin, config, params);
            case ProtoBufRequest:
            case ProtoBufResponse:
            default:
                throw new NotImplementedException("Protobuf not implemented");
        }

        //return null;

    }

    // inverse of the build function
    public static void wrapIfNeeded(DataObj dataObj, Event.EventType eventType) {
        switch (eventType) {
            case HTTPRequest:
            case HTTPResponse:
                String mimeType = MediaType.TEXT_PLAIN;

                boolean defaultMimeType = true;

                for (String HTTP_CONTENT_TYPE_PATH : HTTP_CONTENT_TYPE_PATHS) {
                    try {
                        mimeType = dataObj.getValAsString(HTTP_CONTENT_TYPE_PATH);
                        defaultMimeType = false;
                    } catch (DataObj.PathNotFoundException e) {
                        LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Content-type not found for field " + HTTP_CONTENT_TYPE_PATH)));
                    }
                }
                if(defaultMimeType) {
                    LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Content-type not found, using default of TEXT_PLAIN")));
                }
                
                dataObj.wrapAsString(Constants.BODY_PATH, mimeType);
                return;
            case JavaRequest:
            case JavaResponse:
                return;
            case ThriftRequest:
            case ThriftResponse:
            case ProtoBufRequest:
            case ProtoBufResponse:
            default:
                throw new NotImplementedException("Thrift and Protobuf not implemented");
        }
    }
}
