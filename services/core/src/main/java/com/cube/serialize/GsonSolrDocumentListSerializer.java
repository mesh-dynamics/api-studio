/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cube.serialize;

import java.lang.reflect.Type;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class GsonSolrDocumentListSerializer implements JsonSerializer<SolrDocumentList> , JsonDeserializer<SolrDocumentList> {

    private static final String NUM_FOUND = "numFound";
    private static final String START = "start";
    private static final String MAX_SCORE = "maxScore";
    private static final String DOCUMENTS = "documents";

    @Override
    public JsonElement serialize(SolrDocumentList solrDocuments,
                                 Type type, JsonSerializationContext jsonSerializationContext) {
        JsonElement result = new JsonObject();
        ((JsonObject) result).add(NUM_FOUND , new JsonPrimitive(solrDocuments.getNumFound()));
        ((JsonObject) result).add(START, new JsonPrimitive(solrDocuments.getStart()));
        if (solrDocuments.getMaxScore() != null) {
            ((JsonObject) result).add(MAX_SCORE, new JsonPrimitive(solrDocuments.getMaxScore()));
        }
        JsonArray resultArray = new JsonArray();
        solrDocuments.forEach(document ->  resultArray.add(jsonSerializationContext.serialize(document)));
        ((JsonObject) result).add(DOCUMENTS , resultArray);
        return result;
    }

    @Override
    public SolrDocumentList deserialize(JsonElement jsonElement,
                                        Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        SolrDocumentList documentList = new SolrDocumentList();
        JsonObject asObject = (JsonObject) jsonElement;
        JsonPrimitive numFound = asObject.getAsJsonPrimitive(NUM_FOUND);
        if (numFound != null) {
            documentList.setNumFound(numFound.getAsLong());
        }
        JsonPrimitive start = asObject.getAsJsonPrimitive(START);
        if (start != null) {
            documentList.setStart(start.getAsLong());
        }
        JsonPrimitive maxScore = asObject.getAsJsonPrimitive(MAX_SCORE);
        if (maxScore != null) {
            documentList.setMaxScore(maxScore.getAsFloat());
        }
        JsonArray documentArray = asObject.getAsJsonArray(DOCUMENTS);
        documentArray.forEach(element -> {
            documentList.add(jsonDeserializationContext.deserialize(element , SolrDocument.class));
        });

        return documentList;
    }
}
