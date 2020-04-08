package io.cube.agent;

import static io.md.utils.UtilException.rethrowFunction;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.md.dao.Event;
import io.md.dao.Event.RunType;
import io.md.dao.FnReqRespPayload;
import io.md.dao.FnReqRespPayload.RetStatus;
import io.md.dao.MDTraceInfo;
import io.md.utils.CommonUtils;
import io.md.utils.CubeObjectMapperProvider;
import io.md.utils.FnKey;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-06
 * @author Prasad M D
 */
public class SimpleMocker implements Mocker {

	private static final Logger LOGGER = LogManager.getLogger(SimpleMocker.class);
	private static Map<Integer, Instant> fnMap = new ConcurrentHashMap<>();
	private ObjectMapper jsonMapper;
	private Gson gson;
	CubeClient cubeClient;

	public SimpleMocker(Gson gson) throws Exception {
		jsonMapper = CubeObjectMapperProvider.getInstance();
		cubeClient = new CubeClient(jsonMapper);
		this.gson = gson;
	}

	@Override
	public FnResponseObj mock(FnKey fnKey, Optional<String> traceId, Optional<String> spanId,
		Optional<String> parentSpanId,
		Optional<Instant> prevRespTS, Optional<Type> retType, Object... args) {
		//This key is to identify cases where multiple Solr docs are matched
		Integer key = traceId.orElse("").concat(spanId.orElse(""))
			.concat(parentSpanId.orElse(""))
			.concat(fnKey.signature).hashCode();

		MDTraceInfo mdTraceInfo = new MDTraceInfo(traceId.orElse(null), spanId.orElse(null),
			parentSpanId.orElse(null));
		FnReqRespPayload fnReqRespPayload = new FnReqRespPayload(Optional.of(Instant.now()),
			args, null, null , null);
		Optional<Event> event = CommonUtils.createEvent(fnKey, mdTraceInfo, RunType.Replay,
			Optional.of(prevRespTS.orElse(fnMap.get(key))), fnReqRespPayload);

		return event.map(eve -> {
			Optional<FnResponse> fnResponse = cubeClient.getMockResponse(eve);

			return fnResponse.map(resp -> {
				//If multiple Solr docs were returned, we need to maintain the last timestamp
				//to be used in the next mock call.
				if (resp.retStatus == RetStatus.Success && resp.multipleResults) {
					fnMap.put(key, resp.timeStamp.get());
				} else {
					fnMap.remove(key);
				}

				Object retOrExceptionVal = null;
				try {
					retOrExceptionVal = gson.fromJson(resp.retVal,
						retType.isPresent() ? retType.get() : getRetOrExceptionClass(resp,
							fnKey.function.getGenericReturnType()));
				} catch (Exception e) {
					LOGGER.error(new ObjectMessage(Map.of("func_signature", eve.apiPath, "trace_id",
						eve.getTraceId())), e);
					return new FnResponseObj(null, Optional.empty(), RetStatus.Success,
						Optional.empty());
				}

				return new FnResponseObj(retOrExceptionVal, resp.timeStamp, resp.retStatus,
					resp.exceptionType);
			}).orElseGet(() -> {
				LOGGER.error(new ObjectMessage(Map.of("reason", "No Matching Response Received"
					, "trace_id", eve.getTraceId(), "func_signature", eve.apiPath)));
				return new FnResponseObj(null, Optional.empty(), RetStatus.Success,
					Optional.empty());
			});
		}).orElseGet(() -> {
			LOGGER.error(new ObjectMessage(Map.of("reason", "Not able to form a event"
				, "trace_id", traceId, "func_signature", fnKey.signature)));
			return new FnResponseObj(null, Optional.empty(), RetStatus.Success, Optional.empty());
		});
	}

	private Type getRetOrExceptionClass(FnResponse response, Type returnType) throws Exception {
		if (response.retStatus == RetStatus.Success) {
			return returnType;
		} else {
			return response.exceptionType.map(rethrowFunction(Class::forName)).map(TypeToken::get)
				.map(TypeToken::getType).orElseThrow(() -> new Exception(
					"Exception class not specified"));
		}
	}
}
