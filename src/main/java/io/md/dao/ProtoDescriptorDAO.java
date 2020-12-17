package io.md.dao;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.md.logger.LogMgr;
import io.md.utils.CubeObjectMapperProvider;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;

public class ProtoDescriptorDAO {
	public Integer version;
	public final String customerId;
	public final String app;
	public final String encodedFile;
	private DynamicSchema schema;
	private Map<String, Map<String, MethodDescriptor>> serviceDescriptorMap;
	private static final Logger LOGGER = LogMgr.getLogger(ProtoDescriptorDAO.class);


	public ProtoDescriptorDAO() {
		this.customerId = null;
		this.version = null;
		this.app = null;
		this.encodedFile = null;
	}

	public ProtoDescriptorDAO(String customerId, String app, String encodedFile)
		throws IOException, DescriptorValidationException {
		this.customerId = customerId;
		this.app = app;
		this.encodedFile = encodedFile;
		initialize();
	}

	public void setVersion(Integer version) {
		this.version = version;
	}


	public static class MethodDescriptor {

		public MethodDescriptor(String inputTypeName, String outputTypeName, Boolean clientStreaming, Boolean serverStreaming) {
			this.inputTypeName = inputTypeName;
			this.outputTypeName = outputTypeName;
			this.clientStreaming = clientStreaming;
			this.serverStreaming = serverStreaming;
		}

		public String inputTypeName;
		public String outputTypeName;
		public Boolean clientStreaming;
		public Boolean serverStreaming;
	}

	private void initialize() throws IOException, Descriptors.DescriptorValidationException {
		serviceDescriptorMap = new HashMap<>();
		schema = DynamicSchema.parseFrom(Base64.getDecoder().decode(encodedFile));
		DescriptorProtos.FileDescriptorProto fileDescriptorProto = schema.getFileDescriptorSet().getFile(0);
		for (int i=0 ; i < fileDescriptorProto.getServiceCount() ; i ++) {
			DescriptorProtos.ServiceDescriptorProto serviceDescriptorProto = fileDescriptorProto.getService(i);
			Map<String, MethodDescriptor> methodDescriptorMap = new HashMap<>();
			serviceDescriptorMap.put(serviceDescriptorProto.getName(), methodDescriptorMap);
			int methodCount = serviceDescriptorProto.getMethodCount();
			for (int j = 0 ; j < methodCount ; j++) {
				DescriptorProtos.MethodDescriptorProto methodDescriptorProto = serviceDescriptorProto.getMethod(j);
				methodDescriptorMap.put(methodDescriptorProto.getName(),
					new MethodDescriptor(methodDescriptorProto.getInputType().substring(1),
						methodDescriptorProto.getOutputType().substring(1),
						methodDescriptorProto.getClientStreaming(),
						methodDescriptorProto.getServerStreaming()));
			}
		}
	}

	public void initializeProtoDescriptor(String descriptorFile) throws IOException, Descriptors.DescriptorValidationException {
		initialize();
	}

	private Optional<MethodDescriptor> findMethodDescriptor(String service, String method) {
		return Optional.ofNullable(serviceDescriptorMap.get(service)).
			map(methodDescriptorMap -> methodDescriptorMap.get(method));
	}

	private Optional<String> convertToJson(String encodedByteStream, String typeName, Boolean isStreaming)  {
		try {
			StringBuilder finalJson = new StringBuilder();
			Descriptors.Descriptor featureMessageDescriptor = schema.newMessageBuilder(typeName).getDescriptorForType();

			byte[] decodedFeature = Base64.getDecoder().decode(encodedByteStream);

			if(isStreaming) {
				finalJson.append("[");
			}
			int i=0;
			while (i<decodedFeature.length) {
				if(decodedFeature[i]!=0) {
					break;
				}
				byte[] bodyLengthBytes = Arrays.copyOfRange(decodedFeature, i + 1, i + 5);
				ByteBuffer wrapped = ByteBuffer.wrap(bodyLengthBytes);
				int bodyLength = wrapped.getInt();
				int startInd = i+5;
				byte[] copyOfRangeFeatureList = Arrays.copyOfRange(decodedFeature , startInd , startInd+bodyLength);
				DynamicMessage featureDynamicMessage = DynamicMessage.parseFrom(featureMessageDescriptor, copyOfRangeFeatureList);
				if(i!=0) {
					finalJson.append(",");
				}
				JsonFormat.printer().appendTo(featureDynamicMessage, finalJson);
				i=startInd+bodyLength;
			}
			if(isStreaming && finalJson.length() > 1) {
				finalJson.append("]");
			}
			return finalJson.length() < 2 ? Optional.empty() : Optional.of(
				finalJson.toString());
		} catch(Exception e) {
			LOGGER.error("Cannot convert base64 encoded binary protobuf to json", e);
			return Optional.empty();
		}
	}

	private void convertToByteArraySingleObject(String json, String typeName, OutputStream outputStream)
		throws IOException {
		try {
			DynamicMessage.Builder featureMessageBuilder  = schema.newMessageBuilder(typeName);
			JsonFormat.parser().merge(json, featureMessageBuilder);
			byte[] originalBytes = featureMessageBuilder.build().toByteArray();
				// Need to add the 1st byte as 0 and 2nd to 5th byte as content length in case of grpc requests/responses
				int mbLength = originalBytes.length + 5;
				byte[] mb = new byte[mbLength];
				mb[0] = 0;
				byte[] contentLengthBytes = ByteBuffer.allocate(4).putInt(originalBytes.length)
					.array();
				System.arraycopy(contentLengthBytes, 0, mb, 1, 4); // copy length
				System.arraycopy(originalBytes, 0, mb, 5, originalBytes.length); // copy original bytes
				outputStream.write(mb);
			}
		 catch (Exception e) {
			LOGGER.error("Cannot convert json to byte array", e);
			throw e;
		}
	}


	public Optional<String> convertByteStringToJson(String serviceName, String methodName
		, String encodedByteStream, boolean isRequest) {
		return findMethodDescriptor(serviceName, methodName)
			.flatMap(methodDescriptor ->
				convertToJson(encodedByteStream, isRequest? methodDescriptor.inputTypeName
					: methodDescriptor.outputTypeName, isRequest? methodDescriptor.clientStreaming
					: methodDescriptor.serverStreaming));
	}


	public Optional<byte[]> convertJsonToByteArray(String serviceName, String methodName
		, String json, boolean isRequest) {

		Optional<byte[]> originalBytesOptional = findMethodDescriptor(serviceName, methodName)
			.flatMap(methodDescriptor -> {

				try {
					Boolean isStreaming = isRequest ? methodDescriptor.clientStreaming :
						methodDescriptor.serverStreaming;
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

					if (isStreaming) {
						ObjectMapper jsonMapper = CubeObjectMapperProvider.getInstance();
						JsonNode jsonArrayNode = jsonMapper.readTree(json);
						if (jsonArrayNode.isArray()) {
							for (JsonNode jsonNode : jsonArrayNode) {
								convertToByteArraySingleObject(
									jsonNode.toString(),
									isRequest ? methodDescriptor.inputTypeName :
										methodDescriptor.outputTypeName, outputStream);
							}
						}
					} else {
						convertToByteArraySingleObject(json,
							isRequest ? methodDescriptor.inputTypeName :
								methodDescriptor.outputTypeName, outputStream);
					}
					byte finalOutput[] = outputStream.toByteArray();
					return Optional.ofNullable(finalOutput);
				} catch (Exception e) {
					LOGGER.error("Cannot convert json to byte array for streaming case", e);
					return Optional.empty();
				}
			});

		return originalBytesOptional;
	}



}
