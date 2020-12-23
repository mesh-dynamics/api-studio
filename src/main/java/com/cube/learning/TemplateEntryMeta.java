package com.cube.learning;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

@JsonPropertyOrder({"RequiresReview", "Service", "ApiPath", "EventType", "Count", "JsonPath",
    "IgnoreValue", "IgnorePresence"})
public class TemplateEntryMeta implements Comparable{

    @JsonProperty("RequiresReview")
    YesOrNo requiresReview;

    @JsonProperty("EventType")
    RequestOrResponse eventType;

    @JsonProperty("Service")
    public String service;

    @JsonProperty("ApiPath")
    String apiPath;

    @JsonProperty("JsonPath")
    String jsonPath;

    @JsonProperty("Count")
    Integer count;

    @JsonProperty("IgnoreValue")
    YesOrNo ignoreValue;

    @JsonProperty("IgnorePresence")
    YesOrNo ignorePresence;


    public TemplateEntryMeta(YesOrNo requiresReview, RequestOrResponse requestOrResponse, String service,
        String apiPath, String jsonPath, Integer count, YesOrNo ignoreValue,
        YesOrNo ignorePresence) {
        this.requiresReview = requiresReview;
        this.eventType = requestOrResponse;
        this.service = service;
        this.apiPath = apiPath;
        this.jsonPath = jsonPath;
        this.count = count;
        this.ignoreValue = ignoreValue;
        this.ignorePresence = ignorePresence;
    }

    enum RequestOrResponse {
        Request, Response
    }

    enum YesOrNo{
        yes, no
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TemplateEntryMeta that = (TemplateEntryMeta) o;
        return eventType.equals(that.eventType) &&
            service.equals(that.service) &&
            apiPath.equals(that.apiPath) &&
            jsonPath.equals(that.jsonPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventType, service, apiPath, jsonPath);
    }

    @Override
    public int compareTo(@NotNull Object o) {
        TemplateEntryMeta that = (TemplateEntryMeta) o;
        if (this.requiresReview != that.requiresReview)
            return this.requiresReview == YesOrNo.yes ? -1: 1;
        if (!this.service.equals(that.service))
            return this.service.compareTo(that.service);
        if (!this.apiPath.equals(that.apiPath))
            return this.apiPath.compareTo(that.apiPath);
        if (this.eventType != that.eventType)
            return this.eventType == RequestOrResponse.Request ? -1: 1;
        if (!this.count.equals(that.count))
            return Integer.compare(this.count, that.count);
        return this.jsonPath.compareTo(that.jsonPath);
    }
}
