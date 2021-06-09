package com.cube.serialize;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class GsonSolrDocumentSerializer implements JsonSerializer<SolrDocument> , JsonDeserializer<SolrDocument> {

    private static final Logger LOGGER = LogManager.getLogger(GsonSolrDocumentSerializer.class);

    @Override
    public SolrDocument deserialize(JsonElement jsonElement, Type type
        , JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {

        JsonObject solrDocJson = jsonElement.getAsJsonObject();
        SolrDocument doc = new SolrDocument();
        JsonArray childDocumentJson = solrDocJson.getAsJsonArray("childDocs");
        JsonObject fieldValueMapJson = solrDocJson.getAsJsonObject("fieldMap");

        childDocumentJson.forEach(childDocJson ->
            doc.addChildDocument(jsonDeserializationContext.deserialize(childDocJson , type)));
        fieldValueMapJson.entrySet().forEach(entry -> {
                try {
                    String fieldName = entry.getKey();
                    JsonObject fieldValueJson = entry.getValue().getAsJsonObject();
                    JsonElement fieldSerialized = fieldValueJson.get("value");
                    JsonElement fieldValueClassName = fieldValueJson.get("className");
                    String fieldTypeAsString = jsonDeserializationContext.deserialize(fieldValueClassName , String.class);
                    Object fieldValue = jsonDeserializationContext.deserialize(fieldSerialized , Class.forName(fieldTypeAsString));
                    doc.addField(fieldName , fieldValue);
                } catch (Exception e) {
                    LOGGER.error("Exception occured while deserializing :: "
                        + entry.getKey() + " " + entry.getValue() + " " + e.getMessage());
                }
            }
        );

        return doc;
    }

    @Override
    public JsonElement serialize(SolrDocument document, Type type, JsonSerializationContext jsonSerializationContext) {
        Map<String, Object> fieldMap =  document.getFieldValueMap();
        List<SolrDocument> childDocuments =  document.getChildDocuments();
        JsonObject solrDocJson = new JsonObject();
        JsonObject fieldValueMapJson = new JsonObject();
        fieldMap.keySet().forEach(fieldName -> {
            Object value = document.getFieldValue(fieldName);
            JsonObject valueJson = new JsonObject();
            Class<?> valueClass = value.getClass();
            valueJson.add("className" , jsonSerializationContext.serialize(valueClass.getTypeName()));
            valueJson.add("value" , jsonSerializationContext.serialize(value , valueClass));
            fieldValueMapJson.add(fieldName , valueJson);
        });
        JsonArray childDocumentJson = new JsonArray();
        if (childDocuments != null) {
            childDocuments.forEach(doc -> childDocumentJson.add(jsonSerializationContext.serialize(doc, type)));
        }
        solrDocJson.add("fieldMap" , fieldValueMapJson);
        solrDocJson.add("childDocs" , childDocumentJson);
        return solrDocJson;
    }
}
