/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.dao;

import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.NotImplementedException;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.cube.ws.Config;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-09-06
 */
public class DataObjFactory {


    public static DataObj build(Event.EventType type, byte[] payloadBin, String payloadStr, Config config) {

        switch (type) {
            case HTTPRequest:
            case HTTPResponse:
            case JavaRequest:
            case JavaResponse:
                return new JsonObj(payloadStr, config.jsonmapper);
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
