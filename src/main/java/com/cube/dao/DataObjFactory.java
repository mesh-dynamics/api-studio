/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.dao;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;

import com.cube.exception.DataObjException;
import com.cube.utils.Constants;
import com.cube.ws.Config;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-09-06
 */
public class DataObjFactory {

    private static final Logger LOGGER = LogManager.getLogger(DataObjFactory.class);

    public static final String HTTP_CONTENT_TYPE_PATH = "/hdrs/content-type/0";

    public static DataObj build(Event.EventType type, byte[] payloadBin, String payloadStr,
        Config config, Map<String, Object> params) throws DataObjException {

        switch (type) {
            case HTTPRequest:
            case HTTPResponse:
                JsonObj obj = new JsonObj(payloadStr, config.jsonMapper);
                String mimeType = MediaType.TEXT_PLAIN;
                try {
                    mimeType = obj.getValAsString(HTTP_CONTENT_TYPE_PATH);
                } catch (DataObj.PathNotFoundException e) {
                    LOGGER.info("Content-type not found, using default of TEXT_PLAIN for payload: "
                        + payloadStr);
                }
                obj.unwrapAsJson("/body", mimeType);
                return obj;
            case JavaRequest:
            case JavaResponse:
                return new JsonObj(payloadStr, config.jsonMapper);
            case ThriftRequest:
            case ThriftResponse:

                TDeserializer deserializer = new TDeserializer();
                try {
                    ClassLoader loader = (URLClassLoader) params.get(Constants.CLASS_LOADER);
                    Class<?> clazz = loader.loadClass((String)params.get(Constants.THRIFT_CLASS_NAME));
                    Constructor<?> constructor = clazz.getConstructor();
                    Object obj1 = constructor.newInstance();
                    deserializer.deserialize((TBase) obj1, payloadBin);
                    String jsonSerialized = config.gson.toJson(obj1);
                    JsonObj jsonObj = new JsonObj(jsonSerialized, config.jsonMapper);
                    jsonObj.unwrapAsJson("/", MediaType.APPLICATION_JSON);
                    return jsonObj;
                } catch (InstantiationException | IllegalAccessException
                    | InvocationTargetException | ClassNotFoundException | NoSuchMethodException e) {
                    LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE
                        , "Unable to instantiate class", Constants.CLASS_NAME,
                        params.get(Constants.THRIFT_CLASS_NAME))), e);
                    throw new DataObjException(
                        "Unable to instantiate class" + payloadStr, e);
                } catch (TException e) {
                    LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE
                        , "Thrift Deserialization Error Occured While forming payload")), e);
                    throw new DataObjException(
                        "Unable to create DataObj for Thrift Event Type " + payloadStr, e);
                }

            case ProtoBufRequest:
            case ProtoBufResponse:
            default:
                throw new NotImplementedException("Thrift and Protobuf not implemented");
        }

        //return null;

    }

}
