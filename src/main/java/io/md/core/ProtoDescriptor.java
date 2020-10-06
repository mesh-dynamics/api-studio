package io.md.core;

import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;

import java.io.IOException;
import java.util.*;

public class ProtoDescriptor {

    private DynamicSchema schema;

    private String descriptorFile;

    private Map<String, Map<String, MethodDescriptor>> serviceDescriptorMap;

    public static class MethodDescriptor {

        public MethodDescriptor(String inputTypeName, String outputTypeName) {
            this.inputTypeName = inputTypeName;
            this.outputTypeName = outputTypeName;
        }

        public String inputTypeName;
        public String outputTypeName;
    }

    private void initialize() throws IOException, Descriptors.DescriptorValidationException {
        serviceDescriptorMap = new HashMap<>();
        schema = DynamicSchema.parseFrom(Base64.getDecoder().decode(descriptorFile));
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
                                methodDescriptorProto.getOutputType().substring(1)));
            }
        }
    }

    public ProtoDescriptor(String descriptorFile) throws IOException, Descriptors.DescriptorValidationException {
        this.descriptorFile = descriptorFile;
        initialize();
    }

    private Optional<MethodDescriptor> findMethodDescriptor(String service, String method) {
        return Optional.ofNullable(serviceDescriptorMap.get(service)).
                map(methodDescriptorMap -> methodDescriptorMap.get(method));
    }

    private Optional<String> convertToJson(String encodedByteStream, String typeName)  {
        try {
            Descriptors.Descriptor featureMessageDescriptor = schema.newMessageBuilder(typeName).getDescriptorForType();
            byte[] decodedFeature = Base64.getDecoder().decode(encodedByteStream);
            byte[] copyOfRangeFeature = Arrays.copyOfRange(decodedFeature, 5, decodedFeature.length);
            DynamicMessage featureDynamicMessage = DynamicMessage.parseFrom(featureMessageDescriptor, copyOfRangeFeature);
            return Optional.of(JsonFormat.printer().print(featureDynamicMessage));
        } catch(Exception e) {
            return Optional.empty();
        }
    }

    private Optional<String> convertToByteString(String json, String typeName) {
        try {
            DynamicMessage.Builder featureMessageBuilder  = schema.newMessageBuilder(typeName);
            JsonFormat.parser().merge(json, featureMessageBuilder);
            return Optional.of(Base64.getEncoder().encodeToString(featureMessageBuilder.build().toByteArray()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }


    public Optional<String> convertByteStringToJson(String serviceName, String methodName
        , String encodedByteStream, boolean isRequest) {
        return findMethodDescriptor(serviceName, methodName)
                .flatMap(methodDescriptor ->
                   convertToJson(encodedByteStream, isRequest? methodDescriptor.inputTypeName
                       : methodDescriptor.outputTypeName));
    }

    public Optional<String> convertJsonToByteString(String serviceName, String methodName
        , String json, boolean isRequest) {
        return findMethodDescriptor(serviceName, methodName)
                .flatMap(methodDescriptor ->
                        convertToByteString(json, isRequest? methodDescriptor.inputTypeName :
                            methodDescriptor.outputTypeName));
    }




}
