package com.cube.learning;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.md.injection.DynamicInjectionConfig.InjectionMeta.HTTPMethodType;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@JsonPropertyOrder({"overallScore", "refValue(s)", "extractionConfig", "injectionConfig",
    "valueCountScore", "valueQualityScore", "extractionMethodScore", "extractionUniquenessScore"})
public class InjectionExtractionMeta implements Comparable{

    @JsonUnwrapped
    public ExtractionConfig extractionConfig;

    @JsonUnwrapped
    public InjectionConfig injectionConfig;

    @JsonProperty("refValue(s)")
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
    Integer extractionEquivalenceSetSize = 0;

    public InjectionExtractionMeta(){
        // Default constructor to handle no default constructor found JSON exception when deserializing
        super();
    }

    public InjectionExtractionMeta(ExtractionConfig extractionConfig, InjectionConfig injectionConfig) {
        this.extractionConfig = extractionConfig;
        this.injectionConfig = injectionConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InjectionExtractionMeta that = (InjectionExtractionMeta) o;
        return extractionConfig.equals(that.extractionConfig) &&
            injectionConfig.equals(that.injectionConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(extractionConfig, injectionConfig);
    }

    @JsonPropertyOrder({"ext_apiPath", "ext_jsonPath", "ext_method"})
    public static class ExtractionConfig {

        @JsonProperty("ext_apiPath")
        public String apiPath;

        @JsonProperty("ext_jsonPath")
        public  String jsonPath;

        @JsonProperty("ext_method")
        public HTTPMethodType method;

        @JsonIgnore
        Set<String> values = new HashSet<>();

        @JsonIgnore
        Integer instanceCount = 0; // Number of times this config has featured in response

        @JsonIgnore
        public String nameSuffix;

        public ExtractionConfig() {
            // Default constructor to handle no default constructor found JSON exception when deserializing
            super();
        }

        public ExtractionConfig(String apiPath, String jsonPath, HTTPMethodType method) {
            this.apiPath = apiPath;
            this.jsonPath = jsonPath;
            this.method = method;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExtractionConfig that = (ExtractionConfig) o;
            return apiPath.equals(that.apiPath) &&
                jsonPath.equals(that.jsonPath) &&
                method.equals(that.method);
        }

        @Override
        public int hashCode() {
            return Objects.hash(apiPath, jsonPath, method);
        }

    }

    @JsonPropertyOrder({"inj_apiPath", "inj_jsonPath", "inj_method"})
    public static class InjectionConfig {

        @JsonProperty("inj_apiPath")
        public String apiPath;

        @JsonProperty("inj_jsonPath")
        public String jsonPath;

        @JsonProperty("inj_method")
        public HTTPMethodType method;

        @JsonIgnore
        public Set<String> values = new HashSet<>();

        @JsonIgnore
        public Integer instanceCount = 0; // Number of times this config has featured in a request

        public InjectionConfig() {
            // Default constructor to handle no default constructor found JSON exception when deserializing
            super();
        }

        public InjectionConfig(String apiPath, String jsonPath, HTTPMethodType method) {
            this.apiPath = apiPath;
            this.jsonPath = jsonPath;
            this.method = method;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InjectionConfig that = (InjectionConfig) o;
            return apiPath.equals(that.apiPath) &&
                jsonPath.equals(that.jsonPath) &&
                method.equals(that.method);
        }

        @Override
        public int hashCode() {
            return Objects.hash(apiPath, jsonPath, method);
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
        this.extractionMethodScore = this.extractionConfig.method == HTTPMethodType.POST
            || this.extractionConfig.method == HTTPMethodType.PUT ? 1 : 0;
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

        InjectionExtractionMeta that = (InjectionExtractionMeta)meta;

        // Sort in reverse overall score order
        if (!that.overallScore.equals(this.overallScore)) {
            return that.overallScore.compareTo(this.overallScore);
        }

        ExtractionConfig thisExtConfig = this.extractionConfig;
        ExtractionConfig thatExtConfig = that.extractionConfig;

        if (!thisExtConfig.apiPath.equals(thatExtConfig.apiPath)) {
            return thisExtConfig.apiPath.compareTo(thatExtConfig.apiPath);
        }

        if (!thisExtConfig.jsonPath.equals(thatExtConfig.jsonPath)) {
            return thisExtConfig.jsonPath.compareTo(thatExtConfig.jsonPath);
        }

        if (!thisExtConfig.method.equals(thatExtConfig.method)) {
            return thisExtConfig.method.compareTo(thatExtConfig.method);
        }

        InjectionConfig thisInjConfig = this.injectionConfig;
        InjectionConfig thatInjConfig = that.injectionConfig;

        if (!thisInjConfig.apiPath.equals(thatInjConfig.apiPath)) {
            return thisInjConfig.apiPath.compareTo(thatInjConfig.apiPath);
        }

        if (!thisInjConfig.jsonPath.equals(thatInjConfig.jsonPath)) {
            return thisInjConfig.jsonPath.compareTo(thatInjConfig.jsonPath);
        }

        return thisInjConfig.method.compareTo(thatInjConfig.method);

    }
}


