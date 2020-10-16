package io.md.drivers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.UriBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.uri.UriComponent;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.cache.ProtoDescriptorCache;
import io.md.cache.ProtoDescriptorCache.ProtoDescriptorKey;
import io.md.core.RRTransformerOperations;
import io.md.core.Utils;
import io.md.dao.Event;
import io.md.dao.GRPCPayload;
import io.md.dao.GRPCRequestPayload;
import io.md.dao.ProtoDescriptorDAO;
import io.md.dao.Replay;
import io.md.services.DataStore;
import io.md.utils.Constants;
import io.md.utils.UtilException;

public class GrpcReplayDriver extends HttpReplayDriver {

	protected final Optional<ProtoDescriptorCache> protoDescriptorCacheOptional;

	private static Logger LOGGER = LogManager.getLogger(AbstractReplayDriver.class);


	GrpcReplayDriver(Replay replay, DataStore dataStore,
		ObjectMapper jsonMapper, Optional<ProtoDescriptorCache> protoDescriptorCacheOptional) {
		super(replay, dataStore, jsonMapper);
		this.protoDescriptorCacheOptional = protoDescriptorCacheOptional;
	}

	@Override
	public IReplayClient initClient(Replay replay) throws Exception {
		return new GrpcReplayClient(replay, jsonMapper, protoDescriptorCacheOptional);
	}

	static class GrpcReplayClient extends HttpReplayClient {

		protected final Optional<ProtoDescriptorCache> protoDescriptorCacheOptional;


		GrpcReplayClient(Replay replay, ObjectMapper jsonMapper, Optional<ProtoDescriptorCache> protoDescriptorCacheOptional) throws Exception {
			super(replay, jsonMapper);
			this.protoDescriptorCacheOptional = protoDescriptorCacheOptional;
		}

		@Override
		protected boolean verifyPayload(Event reqEvent) throws IOException {
			if (!(reqEvent.payload instanceof GRPCRequestPayload)) {
				return false;
			}
			else {
				return true;
			}
		}

		@Override
		public HttpRequest build(Replay replay, Event reqEvent)
			throws IOException {
			if (!verifyPayload(reqEvent)) {
				throw new IOException("Invalid Payload type");
			}

			GRPCRequestPayload grpcRequestPayload = (GRPCRequestPayload) reqEvent.payload;
			try {

				protoDescriptorCacheOptional.map(
					protoDescriptorCache -> {
						Optional<ProtoDescriptorDAO> protoDescriptorDAOOptional = protoDescriptorCache
							.get(
								new ProtoDescriptorKey(reqEvent.customerId, reqEvent.app,
									reqEvent.getCollection()));
						grpcRequestPayload.setProtoDescriptor(protoDescriptorDAOOptional);
						return protoDescriptorCache;
					}
				).orElseThrow(() -> new Exception(
					"protoDescriptorCache is missing for GRPCPayload in GRPCReplayDriver"));
			} catch (Exception e) {
				LOGGER.error("protoDescriptorCache is missing for GRPCPayload in GRPCReplayDriver", e);
			}


			return buildRequest(replay, reqEvent, grpcRequestPayload);
		}
	}
}