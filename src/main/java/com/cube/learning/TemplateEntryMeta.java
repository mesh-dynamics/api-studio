package com.cube.learning;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.md.core.TemplateKey.Type;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

@JsonPropertyOrder({"Id", "RuleStatus", "InheritedRuleId", "Service", "ApiPath", "Method", "EventType", "JsonPath",
    "ValueMatchRequired", "PresenceRequired", "Count", "numViolations"})
public class TemplateEntryMeta implements Comparable{
    @JsonProperty("Id")
    String id = EMPTY_ID;

    @JsonProperty("InheritedRuleId")
    String inheritedRuleId = EMPTY_ID;

    @JsonProperty("RuleStatus")
    RuleStatus ruleStatus;

    @JsonProperty("Service")
    public String service;

    @JsonProperty("ApiPath")
    String apiPath;

    @JsonProperty("EventType")
    Type reqOrResp;

    @JsonProperty("Method")
    String method;

    @JsonProperty("JsonPath")
    String jsonPath;

    @JsonProperty("numViolations")
    Integer numViolations = 0;

    @JsonProperty("Count")
    Integer count = 0;

    @JsonProperty("ValueMatchRequired")
    YesOrNo valueMatchRequired;

    @JsonProperty("PresenceRequired")
    YesOrNo presenceRequired;

    Optional<TemplateEntryMeta> parentMeta = Optional.empty();

    public static final String METHODS_ALL = "ALL";
    public static final String EMPTY_ID = "";

    public TemplateEntryMeta(){
        // Default constructor for jsonMapper
    }


    public TemplateEntryMeta(RuleStatus ruleStatus,
        Type reqOrResp,
        String service,
        String apiPath, Optional<String> method, String jsonPath, YesOrNo valueMatchRequired,
        YesOrNo presenceRequired,
        Optional<TemplateEntryMeta> parentMeta) {
        this.ruleStatus = ruleStatus;
        this.reqOrResp = reqOrResp;
        this.service = service;
        this.apiPath = apiPath;
        this.method = method.orElse(METHODS_ALL);
        this.jsonPath = jsonPath;
        this.valueMatchRequired = valueMatchRequired;
        this.presenceRequired = presenceRequired;
        this.parentMeta = parentMeta;
    }

    public TemplateEntryMeta(String service, String apiPath, Type reqOrResp, Optional<String> method,
        String jsonPath) {
        this(RuleStatus.Undefined, Type.DontCare, service, apiPath,
            method, jsonPath, YesOrNo.undefined, YesOrNo.undefined, Optional.empty());
    }

    enum RuleStatus {
        // IMP: Order of fields is used for sorting.
        ViolatesExistingExact, // Instance violates an exact rule of expected match.
        ViolatesExistingInherited, // Instance violates an inherited rule of expected match.
        ViolatesDefault, // Violates a default rule
        UnusedExisting,  // Already configured rule that wasn't exercised.
        ConformsToExistingExact, // Instance complies with an exact rule already configured to ignore mismatch
        ConformsToExistingInherited, // Instance complies with an inherited rule already configured to ignore mismatch
        ConformsToDefault,  // Mismatch when no rule configured, or exact/inherited rule from template with behaviour also to ignore it.
        UsedExistingAsInherited,  // Already configured rule that was exercised as inherited rule.
        Undefined
    }

    enum YesOrNo{
        yes,
        no,
        undefined
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
        return Objects.equals(service, that.service) &&
            Objects.equals(apiPath, that.apiPath) &&
            reqOrResp == that.reqOrResp &&
            (Objects.equals(method, that.method) || method.equals(METHODS_ALL) || that.method
                .equals(METHODS_ALL)) &&
            Objects.equals(jsonPath, that.jsonPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, apiPath, reqOrResp, jsonPath);
    }

    @Override
    public int compareTo(@NotNull Object o) {
        TemplateEntryMeta that = (TemplateEntryMeta) o;
        if (this.ruleStatus != that.ruleStatus)
            return this.ruleStatus.compareTo(that.ruleStatus); // This sorts in declaration order in enum.
        if (!this.service.equals(that.service))
            return this.service.compareTo(that.service);
        if (!this.apiPath.equals(that.apiPath))
            return this.apiPath.compareTo(that.apiPath);
        if (this.reqOrResp != that.reqOrResp)
            return this.reqOrResp == Type.RequestCompare ? -1: 1;
        if (!this.count.equals(that.count))
            return Integer.compare(this.count, that.count);
        return this.jsonPath.compareTo(that.jsonPath);
    }
}
