package io.md.dao;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.md.logger.LogMgr;
import io.md.utils.CubeObjectMapperProvider;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;

public class ProtoDescriptorDAO {
	public Integer version;
	public final String customerId;
	public final String app;
	public final String encodedFile;
	public final Map<String, String> protoFileMap;
	private DynamicSchema schema;
	private Map<String, Map<String, MethodDescriptor>> serviceDescriptorMap;
	private static final Logger LOGGER = LogMgr.getLogger(ProtoDescriptorDAO.class);


	private ProtoDescriptorDAO() {
		this.customerId = null;
		this.version = null;
		this.app = null;
		this.encodedFile = null;
		this.protoFileMap = null;
	}

	public ProtoDescriptorDAO(String customerId, String app, String encodedFile,
		Map<String, String> protoFileMap)
		throws IOException, DescriptorValidationException {
		this.customerId = customerId;
		this.app = app;
		this.encodedFile = encodedFile;
		this.protoFileMap = protoFileMap;
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
		List<FileDescriptorProto> fileDescriptorProtos = schema
			.getFileDescriptorSet().getFileList();
//		DescriptorProtos.FileDescriptorProto fileDescriptorProto = schema.getFileDescriptorSet().getFile(0);
		for (int fileInd=0; fileInd<fileDescriptorProtos.size(); fileInd++) {
			DescriptorProtos.FileDescriptorProto fileDescriptorProto = schema.getFileDescriptorSet().getFile(fileInd);
			for (int i = 0; i < fileDescriptorProto.getServiceCount(); i++) {
				DescriptorProtos.ServiceDescriptorProto serviceDescriptorProto = fileDescriptorProto
					.getService(i);
				Map<String, MethodDescriptor> methodDescriptorMap = new HashMap<>();
				serviceDescriptorMap.put(serviceDescriptorProto.getName(), methodDescriptorMap);
				int methodCount = serviceDescriptorProto.getMethodCount();
				for (int j = 0; j < methodCount; j++) {
					DescriptorProtos.MethodDescriptorProto methodDescriptorProto = serviceDescriptorProto
						.getMethod(j);
					methodDescriptorMap.put(methodDescriptorProto.getName(),
						new MethodDescriptor(methodDescriptorProto.getInputType().substring(1),
							methodDescriptorProto.getOutputType().substring(1),
							methodDescriptorProto.getClientStreaming(),
							methodDescriptorProto.getServerStreaming()));
				}
			}
		}
	}

	public void initializeProtoDescriptor() throws IOException, Descriptors.DescriptorValidationException {
		initialize();
	}

	public String convertToJsonDescriptor() {
		ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
		schema.getFileDescriptorSet().getFileList().forEach(fileDescriptorProto -> {
			ObjectNode fileNode = JsonNodeFactory.instance.objectNode();
			rootNode.set(fileDescriptorProto.getName() , fileNode);
			List<DescriptorProto> descriptorProtoList = fileDescriptorProto.getMessageTypeList();
			Map<String, JsonNode> typeMap = new HashMap<>();

			descriptorProtoList.forEach(descriptorProto -> {
				ObjectNode typeNode = JsonNodeFactory.instance.objectNode();
				typeMap.put(descriptorProto.getName(), typeNode);
				for (int i = 0; i < descriptorProto.getFieldCount(); i++) {
					FieldDescriptorProto fieldDescriptor = descriptorProto.getField(i);
					String fieldName = fieldDescriptor.getName();
					String type = fieldDescriptor.getType().name();
					String typeName = fieldDescriptor.getTypeName();
					if (type.equals("TYPE_INT32") || type.equals("TYPE_INT64") ||
						type.equals("TYPE_UINT32") || type.equals("TYPE_UINT64") ||
						type.equals("TYPE_SINT32") || type.equals("TYPE_FIXED32") ||
						type.equals("TYPE_FIXED64")) {
						typeNode.set(fieldName, JsonNodeFactory.instance.numberNode(10));
					} else if (type.equals("TYPE_FLOAT") || type.equals("TYPE_DOUBLE")) {
						typeNode.set(fieldName, JsonNodeFactory.instance.numberNode(1.1));
					} else if (type.equals("TYPE_BOOL")) {
						typeNode.set(fieldName, JsonNodeFactory.instance.booleanNode(true));
					} else if (type.equals("TYPE_STRING")) {
						typeNode.set(fieldName, JsonNodeFactory.instance.textNode("Hello"));
					} else if (type.equals("TYPE_MESSAGE")) {
						typeNode.set(fieldName, typeMap
							.get(typeName.substring(typeName.lastIndexOf(".") + 1)));
					}
				}
			});

			fileNode.set("package" , JsonNodeFactory.instance.textNode(fileDescriptorProto.getPackage()));

			for (int i = 0; i < fileDescriptorProto.getServiceCount(); i++) {
				DescriptorProtos.ServiceDescriptorProto serviceDescriptorProto = fileDescriptorProto
					.getService(i);
				ObjectNode serviceNode = JsonNodeFactory.instance.objectNode();
				fileNode.set(serviceDescriptorProto.getName(), serviceNode);
				int methodCount = serviceDescriptorProto.getMethodCount();
				for (int j = 0; j < methodCount; j++) {
					DescriptorProtos.MethodDescriptorProto methodDescriptorProto = serviceDescriptorProto
						.getMethod(j);
					ObjectNode methodNode = JsonNodeFactory.instance.objectNode();
					serviceNode.set(methodDescriptorProto.getName(), methodNode);
					String inputTypeName = methodDescriptorProto.getInputType();
					String outputTypeName = methodDescriptorProto.getOutputType();
					methodNode.set("inputTypeName", new TextNode(inputTypeName));
					methodNode.set("inputSchema",
						typeMap.get(inputTypeName.substring(inputTypeName.lastIndexOf(".") + 1)));
					methodNode.set("outputTypeName", new TextNode(outputTypeName));
					methodNode.set("outputSchema",
						typeMap.get(outputTypeName.substring(outputTypeName.lastIndexOf(".") + 1)));

				}
			}
		});

		return rootNode.toString();
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
