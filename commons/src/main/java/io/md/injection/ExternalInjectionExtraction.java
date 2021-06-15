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

package io.md.injection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.md.injection.DynamicInjectionConfig.InjectionMeta.HTTPMethodType;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

@JsonPropertyOrder({"overallScore", "refValues", "extractionConfig", "injectionConfig",
    "valueCountScore", "valueQualityScore", "extractionMethodScore", "extractionUniquenessScore"})
public class ExternalInjectionExtraction implements Comparable{

    @JsonUnwrapped
    public ExternalExtraction externalExtraction;

    @JsonUnwrapped
    public ExternalInjection externalInjection;

    @JsonProperty("refValues")
    public Set<String> values = new HashSet<>();

    @JsonProperty("overallScore")
    public Float overallScore = 0f;

    @JsonProperty("extractionMethodScore")
    public Integer extractionMethodScore = 0;

    @JsonProperty("valueCountScore")
    public Integer valueCountScore = 0;

    @JsonProperty("valueQualityScore")
    public Float valueQualityScore = 0f;

    @JsonProperty("extractionUniquenessScore")
    public Float extractionUniquenessScore = 0f;

    public final static float valueCountScoreWeight = 0.5f;
    public final static float extractionMethodScoreWeight = 0.2f;
    public final static float valueQualityScoreWeight = 0.2f;
    public final static float extractionUniquenessScoreWeight = 0.1f;


    @JsonIgnore
    public Integer instanceCount = 0; //Number of times this relationship has featured

    // Think of extractions and injections as nodes in a graph, where edges flow from extractions to injections.
    // An extraction may point to multiple injections and vice-versa. Equivalence set size indicates #ext_configs
    // which point to the same injection that this config points to.
    // This is used to rank the extraction - larger equivalent set indicates insignificant value.
    @JsonIgnore
    public Integer extractionEquivalenceSetSize = 0;

    public ExternalInjectionExtraction(){
        // Default constructor to handle no default constructor found JSON exception when deserializing
        super();
    }

    public ExternalInjectionExtraction(ExternalExtraction externalExtraction, ExternalInjection externalInjection) {
        this.externalExtraction = externalExtraction;
        this.externalInjection = externalInjection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExternalInjectionExtraction that = (ExternalInjectionExtraction) o;
        return externalExtraction.equals(that.externalExtraction) &&
            externalInjection.equals(that.externalInjection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(externalExtraction, externalInjection);
    }

    @JsonPropertyOrder({"extApiPath", "extJsonPath", "extMethod"})
    public static class ExternalExtraction implements Comparable{

        @JsonProperty("extApiPath")
        public String apiPath;

        @JsonProperty("extJsonPath")
        public  String jsonPath;

        @JsonProperty("extMethod")
        public HTTPMethodType method;

        @JsonIgnore
        public Set<String> values = new HashSet<>();

        @JsonIgnore
        public Integer instanceCount = 0; // Number of times this config has featured in response

        @JsonIgnore
        public String nameSuffix;

        public ExternalExtraction() {
            // Default constructor to handle no default constructor found JSON exception when deserializing
            apiPath="";
            jsonPath="";
            method=HTTPMethodType.POST;
        }

        public ExternalExtraction(String apiPath, String jsonPath, HTTPMethodType method) {
            this.apiPath = apiPath;
            this.jsonPath = jsonPath;
            this.method = method;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExternalExtraction that = (ExternalExtraction) o;
            return apiPath.equals(that.apiPath) &&
                jsonPath.equals(that.jsonPath) &&
                method.equals(that.method);
        }

        @Override
        public int compareTo(@NotNull Object o) {


            ExternalExtraction that = (ExternalExtraction) o;

            int apiPathComp = this.apiPath.compareTo(that.apiPath);
            if (apiPathComp != 0) return apiPathComp;

            int jsonPathComp = this.jsonPath.compareTo(that.jsonPath);
            if (jsonPathComp != 0) return jsonPathComp;

            return this.method.compareTo(that.method);
        }

        @Override
        public int hashCode() {
            return Objects.hash(apiPath, jsonPath, method);
        }

    }

    @JsonPropertyOrder({"injApiPath", "injJsonPath", "injMethod", "keyTransform",
        "valueTransform", "injectAllPaths"})
    public static class ExternalInjection implements Comparable{

        @JsonProperty("injApiPath")
        public String apiPath;

        @JsonProperty("injJsonPath")
        public String jsonPath;

        @JsonProperty("injMethod")
        public HTTPMethodType method;

        @JsonProperty("keyTransform")
        public String keyTransform;

        @JsonProperty("valueTransform")
        public String valueTransform;

        @JsonProperty("injectAllPaths")
        public Boolean injectAllPaths = false;

        @JsonIgnore
        public Set<String> values = new HashSet<>();

        @JsonIgnore
        public Integer instanceCount = 0; // Number of times this config has featured in a request

        public ExternalInjection() {
            // Default constructor to handle no default constructor found JSON exception when deserializing
            apiPath = "";
            jsonPath = "";
            method = HTTPMethodType.POST;
            this.keyTransform = "";
            this.valueTransform = "";
            injectAllPaths = false;
        }

        public ExternalInjection(String apiPath, String jsonPath, HTTPMethodType method,
            String keyTransform, String valueTransform, Boolean injectAllPaths) {
            this.apiPath = apiPath;
            this.jsonPath = jsonPath;
            this.method = method;
            this.keyTransform = keyTransform;
            this.valueTransform = valueTransform;
            this.injectAllPaths = injectAllPaths;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExternalInjection that = (ExternalInjection) o;
            return apiPath.equals(that.apiPath) &&
                jsonPath.equals(that.jsonPath) &&
                method.equals(that.method);
        }

        @Override
        public int compareTo(@NotNull Object o) {

            ExternalInjection that = (ExternalInjection) o;

            int apiPathComp = this.apiPath.compareTo(that.apiPath);
            if (apiPathComp != 0) return apiPathComp;

            int jsonPathComp = this.jsonPath.compareTo(that.jsonPath);
            if (jsonPathComp != 0) return jsonPathComp;

            return this.method.compareTo(that.method);
        }

        @Override
        public int hashCode() {
            return Objects.hash(apiPath, jsonPath, method);
        }

        @JsonSetter("InjectAllPaths")
        public void setInjectAllPaths(String injectAllPaths) {
            // Required because Jackson doesn't support deserialization from caps (TRUE/FALSE)
            this.injectAllPaths = Boolean.valueOf(injectAllPaths);
        }
    }

    private static Float getStringScore(String str){
        if (str.matches("[-xX0-9a-fA-F]+")) {
            // Hex and <hyphen> can return scores higher than 0.5, but only if len > 5.
            return Float.min((float) str.length() / 10f, 1f);
        } else if (str.matches("[a-z A-Z,_]+") || str.contains(":")) {
            // Only <alphabets>, <space>, <comma> and <underscore> are least score
            // ':' caters to timestamps
            return 0f;
        } else {
            // Others are middle score
            return 0.5f;
        }

    }

    public void calculateScores(){
        this.valueCountScore = this.values.size() > 1 ? 1 : 0;
        this.valueQualityScore = getStringScore(this.values.iterator().next());
        this.extractionMethodScore = this.externalExtraction.method == HTTPMethodType.POST
            || this.externalExtraction.method == HTTPMethodType.PUT ? 1 : 0;
        this.extractionUniquenessScore =
            this.extractionEquivalenceSetSize > 0 ? (float) 1 / this.extractionEquivalenceSetSize
                : 0;

        this.overallScore = valueCountScoreWeight * this.valueCountScore
            + valueQualityScoreWeight * this.valueQualityScore
            + extractionMethodScoreWeight * this.extractionMethodScore
            + extractionUniquenessScoreWeight * this.extractionUniquenessScore;
    }

    public Float getOverallScore(){
        return overallScore;
    }

    @Override
    public int compareTo(@NotNull Object meta) {

        ExternalInjectionExtraction that = (ExternalInjectionExtraction)meta;

        // Sort in reverse overall score order
        if (!that.overallScore.equals(this.overallScore)) {
            return that.overallScore.compareTo(this.overallScore);
        }

        int extConfigComp = this.externalExtraction.compareTo(that.externalExtraction);
        if (extConfigComp != 0) return extConfigComp;

        return this.externalInjection.compareTo(that.externalInjection);

    }

    public static class ExternalNamedInjectionExtraction {
        @JsonProperty("varName")
        public String varName;

        @JsonUnwrapped
        public ExternalExtraction externalExtraction;

        @JsonUnwrapped
        public ExternalInjection externalInjection;

        public ExternalNamedInjectionExtraction() {
            // Default constructor for Jackson
            varName = "";
            externalExtraction = new ExternalExtraction();
            externalInjection = new ExternalInjection();
        }

        public ExternalNamedInjectionExtraction(String varName,
            ExternalExtraction externalExtraction,
            ExternalInjection externalInjection) {
            this.varName = varName;
            this.externalExtraction = externalExtraction;
            this.externalInjection = externalInjection;
        }
    }
}


