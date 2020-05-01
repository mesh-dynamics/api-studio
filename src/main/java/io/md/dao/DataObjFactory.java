package io.md.dao;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.constants.Constants;

// TODO this class will eventually go away
public class DataObjFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataObjFactory.class);

	// Http headers are case insensitive
	private static final List<String> HTTP_CONTENT_TYPE_PATHS = Arrays.asList("/hdrs/content-type/0"
		, "/hdrs/Content-type/0", "/hdrs/Content-Type/0", "/hdrs/content-Type/0");

	private static  Optional<String> getMimeType(DataObj obj) {
		Optional<String> mimeType = Optional.empty();
		for (String HTTP_CONTENT_TYPE_PATH : HTTP_CONTENT_TYPE_PATHS) {
			try {
				mimeType = Optional.of(obj.getValAsString(HTTP_CONTENT_TYPE_PATH));
				break;
			} catch (DataObj.PathNotFoundException e) {
				LOGGER.info(
					"Content-type not found for field " + HTTP_CONTENT_TYPE_PATH);
			}
		}
		if (!mimeType.isPresent()) {
			LOGGER.info("Content-type not found, using default of TEXT_PLAIN");

		}
		return mimeType;
	}


	public static DataObj build(Event.EventType type, Payload payload,
		 Map<String, Object> params) {
		ObjectMapper jsonMapper;
		switch (type) {
			case HTTPRequest:
			case HTTPResponse:
				jsonMapper = (ObjectMapper) params.get(Constants.OBJECT_MAPPER);
				if (jsonMapper == null) throw new RuntimeException("Json Mapper Not Provided");
				try {
					JsonDataObj obj = new JsonDataObj(payload.rawPayloadAsString(), jsonMapper);
					String mimeType = getMimeType(obj).orElse(MediaType.TEXT_PLAIN);
					obj.unwrapAsJson(Constants.BODY_PATH, mimeType);
					return obj;
				} catch (Exception e) {
					LOGGER.error("Error Occurred while creating data object"
						,e);
				}

			case JavaRequest:
			case JavaResponse:
				try {
					jsonMapper = (ObjectMapper) params.get(Constants.OBJECT_MAPPER);
					if (jsonMapper == null)
						throw new RuntimeException("Json Mapper Not Provided");
					return new JsonDataObj(payload.rawPayloadAsString(), jsonMapper);
				} catch (Exception e) {
					LOGGER.error("Error Occurred while creating data object"
						,e);
				}
			case ThriftRequest:
			case ThriftResponse:
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
				String mimeType = getMimeType(dataObj).orElse(MediaType.TEXT_PLAIN);
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

