package com.cube.golden;

import java.time.Instant;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GoldenSet {

    @JsonProperty("collec")
    public final String collectionId;  // the collection
    @JsonProperty("templateSet")
    public final String templateSetId;
    @JsonProperty("createTime")
    public final Instant creationTimestamp;
    @JsonProperty("rootGldnSetId")
    public final Optional<String> rootGoldenSet;
    @JsonProperty("prntGldnSetId")
    public final Optional<String> parentGoldentSet;

    public GoldenSet(String collection, String templateSetId, Optional<String> rootGoldenSet,
                     Optional<String> parentGoldentSet, Optional<Instant> creationTimestampOpt) {
        this.collectionId = collection;
        this.templateSetId = templateSetId;
        this.rootGoldenSet = rootGoldenSet;
        this.parentGoldentSet = parentGoldentSet;
        creationTimestamp = creationTimestampOpt.orElse(Instant.now());
    }

    public String getCollectionId() {
        return this.collectionId;
    }

    public String getTemplateSetId() {
        return this.templateSetId;
    }

}
