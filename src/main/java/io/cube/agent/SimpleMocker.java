package io.cube.agent;

import static io.md.utils.UtilException.rethrowFunction;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleMocker.class);
	private static Map<Integer, Instant> fnMap = new ConcurrentHashMap<>();
	private ObjectMapper jsonMapper;
	private Gson gson;
	CubeClient cubeClient;

	public SimpleMocker(Gson gson)  {
		jsonMapper = CubeObjectMapperProvider.getInstance();
		cubeClient = new CubeClient(jsonMapper);
		this.gson = gson;
	}

	@Override
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
		Optional<Event> event = CommonUtils.createEvent(fnKey, mdTraceInfo, RunType.Replay,
			Optional.ofNullable(prevRespTS.orElse(fnMap.get(key))), fnReqRespPayload);

		try {
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
						LOGGER.error("func_signature :".concat(eve.apiPath)
							.concat(" , trace_id : ").concat(eve.getTraceId()), e);
						return new FnResponseObj(null, Optional.empty(), RetStatus.Success,
							Optional.empty());
					}

					return new FnResponseObj(retOrExceptionVal, resp.timeStamp, resp.retStatus,
						resp.exceptionType);
				}).orElseGet(() -> {
					LOGGER.error(
						"No Matching Response Received : trace_id : ".concat(eve.getTraceId())
							.concat(" , func_signature : ".concat(eve.apiPath)));
					return new FnResponseObj(null, Optional.empty(), RetStatus.Success,
						Optional.empty());
				});
			}).orElseGet(() -> {
				LOGGER.error("Not able to form an event : trace_id :".concat(traceId.orElse(" NA "))
					.concat(" , func_signature : ".concat(fnKey.signature)));
				return new FnResponseObj(null, Optional.empty(), RetStatus.Success,
					Optional.empty());
			});
		} catch (Exception ex) {
			LOGGER.error("Exception occurred while mocking function! Function Key : " + fnKey);
		}

		return new FnResponseObj(null, Optional.empty(), RetStatus.Success,
			Optional.empty());
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
