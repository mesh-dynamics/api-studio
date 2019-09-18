package org.apache.logging.log4j.core.layout;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder;

//https://github.com/savantly-net/log4j2-extended-jsonlayout/blob/master/src/main/java/org/apache/logging/log4j/core/layout/ExtendedJsonSerializer.java

public class CustomJsonSerializer extends BeanSerializer {
    public CustomJsonSerializer(JavaType type, BeanSerializerBuilder builder, BeanPropertyWriter[] properties, BeanPropertyWriter[] filteredProperties) {
        super(type, builder, properties, filteredProperties);
    }

    @Override
    protected void serializeFields(Object bean, final JsonGenerator gen, SerializerProvider provider) throws IOException {

    }
}
