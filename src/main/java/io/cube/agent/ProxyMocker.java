package io.cube.agent;

import static io.md.utils.UtilException.rethrowFunction;

import java.lang.reflect.Type;
import java.sql.Blob;
import java.sql.Clob;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import io.md.dao.Event;
import io.md.dao.Event.RunType;
import io.md.dao.FnReqRespPayload;
import io.md.dao.FnReqRespPayload.RetStatus;
import io.md.dao.MDTraceInfo;
import io.md.dao.Recording.RecordingType;
import io.md.services.FnResponse;
import io.md.services.MockResponse;
import io.md.utils.CommonUtils;
import io.md.utils.CubeObjectMapperProvider;
import io.md.utils.FnKey;
import io.md.services.Mocker;
import io.md.utils.Utils;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-06
 * @author Prasad M D
 */
public class ProxyMocker implements Mocker {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProxyMocker.class);
	private static Map<Integer, Instant> fnMap = new ConcurrentHashMap<>();
	private ObjectMapper jsonMapper;
	private Gson gson;
	CubeClient cubeClient;

	public ProxyMocker(Gson gson)  {
		jsonMapper = CubeObjectMapperProvider.getInstance();
		cubeClient = new CubeClient(jsonMapper);
		this.gson = gson;
	}

	@Override
	public MockResponse mock(Event event, Optional<Instant> lowerBoundForMatching) throws MockerException {
		return cubeClient.getMockResponseEvent(event, lowerBoundForMatching).orElse(emptyResponse);
	}

	public FnResponseObj mock(FnKey fnKey,
		Optional<Instant> prevRespTS, Optional<Type> retType, Object... args) {
		MDTraceInfo mdTraceInfo;
		if (CommonUtils.getCurrentTraceId().isPresent()) {
			//load the created context
			mdTraceInfo = CommonUtils.mdTraceInfoFromContext();

		} else {
			//No span context. Initialization scenario.
			mdTraceInfo = CommonUtils.getDefaultTraceInfo();
		}

		Optional<String> traceId = Optional.ofNullable(mdTraceInfo.traceId);
		Optional<String> spanId = Optional.ofNullable(mdTraceInfo.spanId);
		Optional<String> parentSpanId = Optional.ofNullable(mdTraceInfo.parentSpanId);

		Integer key = traceId.orElse("").concat(spanId.orElse(""))
			.concat(parentSpanId.orElse(""))
			.concat(fnKey.signature).hashCode();

		FnReqRespPayload fnReqRespPayload = new FnReqRespPayload(Optional.of(Instant.now()),
			args, null, null , null);
		Optional<Instant> lowerBound = prevRespTS.isPresent() ? prevRespTS : Optional.ofNullable(fnMap.get(key));
		Optional<Event> event = CommonUtils.createEvent(fnKey, mdTraceInfo, RunType.Replay,
			Optional.empty(), fnReqRespPayload, RecordingType.Golden);

		try {
			return event.map(UtilException.rethrowFunction(eve -> {
				Optional<FnResponse> fnResponse = Utils.mockResponseToFnResponse(mock(eve, lowerBound));

				return fnResponse.map(resp -> {
					//If multiple Solr docs are returned, we need to maintain the last timestamp
					//to be used in the next mock call.
					if (resp.retStatus == RetStatus.Success && resp.multipleResults) {
						fnMap.put(key, resp.timeStamp.get());
					} else {
						fnMap.remove(key);
					}

					Object retOrExceptionVal = null;
					try {
						Type returnType = retType.orElse(fnKey.function.getGenericReturnType());
						if (returnType.getTypeName().equals(Clob.class.getTypeName())) {
							retOrExceptionVal = new SerialClob(resp.retVal.toCharArray());
						} else if (returnType.getTypeName().equals(Blob.class.getTypeName())) {
							retOrExceptionVal = new SerialBlob(Base64.decodeBase64(resp.retVal));
						} else {
							retOrExceptionVal = gson.fromJson(resp.retVal,
								retType.isPresent() ? retType.get() : getRetOrExceptionClass(resp,
									returnType));
						}
					} catch (JsonSyntaxException ex) {
						//If the returned value is a String with spaces, this exception
						//is thrown, In that case we will return the same value
						LOGGER.error(
							"Json Syntax exception, could be a simple string, returning the original value");
						return new FnResponseObj(resp.retVal, resp.timeStamp, resp.retStatus,
							resp.exceptionType);
					}
					catch (Exception e) {
						LOGGER.error("func_signature :".concat(eve.apiPath)
							.concat(" , trace_id : ").concat(eve.getTraceId()), e);
						return emptyFnResponseObj;
					}

					return new FnResponseObj(retOrExceptionVal, resp.timeStamp, resp.retStatus,
						resp.exceptionType);
				}).orElseGet(() -> {
					LOGGER.error(
						"No Matching Response Received : trace_id : ".concat(eve.getTraceId())
							.concat(" , func_signature : ".concat(eve.apiPath)));
					return emptyFnResponseObj;
				});
			})).orElseGet(() -> {
				LOGGER.error("Not able to form an event : trace_id :".concat(traceId.orElse(" NA "))
					.concat(" , func_signature : ".concat(fnKey.signature)));
				return emptyFnResponseObj;
			});
		} catch (Exception ex) {
			LOGGER.error("Exception occurred while mocking function! Function Key : " + fnKey);
		}
		return emptyFnResponseObj;
	}

	static private Type getRetOrExceptionClass(FnResponse response, Type returnType) throws Exception {
		if (response.retStatus == RetStatus.Success) {
			return returnType;
		} else {
			return response.exceptionType.map(rethrowFunction(Class::forName)).map(TypeToken::get)
				.map(TypeToken::getType).orElseThrow(() -> new Exception(
					"Exception class not specified"));
		}
	}

	static final MockResponse emptyResponse = new MockResponse(Optional.empty(), 0);
	static final FnResponseObj emptyFnResponseObj = new FnResponseObj(null, Optional.empty(), RetStatus.Success,
			Optional.empty());

}
