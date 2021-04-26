package io.md.drivers;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.cache.ProtoDescriptorCache;
import io.md.dao.Event;
import io.md.dao.GRPCRequestPayload;
import io.md.dao.GRPCResponsePayload;
import io.md.dao.Replay;
import io.md.dao.RequestDetails;
import io.md.dao.RequestPayload;
import io.md.dao.ResponsePayload;
import io.md.services.DataStore;
import io.md.utils.Utils;

public class GrpcReplayDriver extends AbstractReplayDriver {

	protected final Optional<ProtoDescriptorCache> protoDescriptorCacheOptional;

	private static Logger LOGGER = LogManager.getLogger(GrpcReplayDriver.class);


	GrpcReplayDriver(Replay replay, DataStore dataStore,
		ObjectMapper jsonMapper, Optional<ProtoDescriptorCache> protoDescriptorCacheOptional) {
		super(replay, dataStore);
		this.jsonMapper = jsonMapper;
		this.protoDescriptorCacheOptional = protoDescriptorCacheOptional;
	}

	@Override
	public IReplayClient initClient(Replay replay) throws Exception {
		return new GrpcReplayClient(protoDescriptorCacheOptional);
	}

	@Override
	protected void modifyResponse(Event respEvent) {
		if(respEvent.payload instanceof  GRPCResponsePayload){
			Utils.setProtoDescriptorGrpcEvent(respEvent, protoDescriptorCacheOptional.orElseThrow());
		}
	}

	public static  class GrpcReplayClient extends AbstractIReplayClient{

		protected final Optional<ProtoDescriptorCache> protoDescriptorCacheOptional;

		public GrpcReplayClient(Optional<ProtoDescriptorCache> protoDescriptorCacheOptional) throws Exception {
			this.protoDescriptorCacheOptional = protoDescriptorCacheOptional;
		}

		@Override
		public boolean verifyPayload(Event reqEvent) throws IOException {
			return reqEvent.payload instanceof GRPCRequestPayload;
		}

		@Override
		public RequestPayload modifyRequest(Event reqEvent) {
			try {

				protoDescriptorCacheOptional.map(
					protoDescriptorCache -> {
						io.md.utils.Utils.setProtoDescriptorGrpcEvent(reqEvent, protoDescriptorCache);
						return protoDescriptorCache;
					}
				).orElseThrow(() -> new Exception(
					"protoDescriptorCache is missing for GRPCPayload in GRPCReplayDriver"));
			} catch (Exception e) {
				LOGGER.error("protoDescriptorCache is missing for GRPCPayload in GRPCReplayDriver", e);
			}
			return (RequestPayload) reqEvent.payload;
		}

		@Override
		public ResponsePayload formResponsePayload(MDResponse response)
		{
			GRPCResponsePayload responsePayload = new GRPCResponsePayload(response.getHeaders(),
				response.getBody(), response.getPath(), response.statusCode(), response.getTrailers() );
			return responsePayload;
		}


		@Override
		public MDHttpClient getClient(RequestDetails details){
			return new MDHttp2Client(details);
		}
	}
}