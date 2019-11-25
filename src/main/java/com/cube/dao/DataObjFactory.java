/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.dao;

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

    public static final String HTTP_CONTENT_TYPE_PATH = "/hdrs/content-type/0";

    public static DataObj build(Event.EventType type, byte[] payloadBin, String payloadStr, Config config) {

        switch (type) {
            case HTTPRequest:
            case HTTPResponse:
                JsonDataObj obj = new JsonDataObj(payloadStr, config.jsonMapper);
                String mimeType = MediaType.TEXT_PLAIN;
                try {
                    mimeType = obj.getValAsString(HTTP_CONTENT_TYPE_PATH);
                } catch (DataObj.PathNotFoundException e) {
                    LOGGER.info("Content-type not found, using default of TEXT_PLAIN for payload: " + payloadStr);
                }
                obj.unwrapAsJson(Constants.BODY_PATH, mimeType);
                return obj;
            case JavaRequest:
            case JavaResponse:
                return new JsonDataObj(payloadStr, config.jsonMapper);
            case ThriftRequest:
            case ThriftResponse:
            case ProtoBufRequest:
            case ProtoBufResponse:
            default:
                throw new NotImplementedException("Thrift and Protobuf not implemented");
        }

        //return null;

    }

    // inverse of the build function
    public static void wrapIfNeeded(DataObj dataObj, Event.EventType eventType) {
        switch (eventType) {
            case HTTPRequest:
            case HTTPResponse:
                String mimeType = MediaType.TEXT_PLAIN;
                try {
                    mimeType = dataObj.getValAsString(HTTP_CONTENT_TYPE_PATH);
                } catch (DataObj.PathNotFoundException e) {
                    LOGGER.info(new ObjectMessage(Map.of(
                        Constants.MESSAGE, "Content-type not found, using default of TEXT_PLAIN"
                    )));
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
