package com.cube.learning;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.md.core.CompareTemplate.ComparisonType;
import io.md.core.CompareTemplate.PresenceType;
import io.md.core.TemplateKey.Type;
import io.md.core.Utils;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

@JsonPropertyOrder({"Id", "Action", "Service", "ApiPath", "Method", "EventType", "JsonPath",
    "SourceRulePath", "CurrentComparisonType", "CurrentPresenceType",
    "NewComparisonType", "NewPresenceType", "InheritedRuleId", "Count",
    "NumViolationsComparison", "NumViolationsPresence"})
public class TemplateEntryMeta implements Comparable{
    @JsonProperty("Id")
    String id = EMPTY;

    @JsonProperty("InheritedRuleId")
    String inheritedRuleId = EMPTY;

    @JsonProperty("SourceRulePath")
    String sourceRulePath;

    @JsonIgnore
    RuleStatus ruleStatus;

    @JsonProperty("Action")
    Action action;

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

    @JsonProperty("NumViolationsComparison")
    Integer numViolationsComparison = 0;

    @JsonProperty("NumViolationsPresence")
    Integer numViolationsPresence = 0;

    @JsonProperty("Count")
    Integer count = 0;

    @JsonProperty("NewComparisonType")
    private Optional<ComparisonType> newCt;

    @JsonProperty("NewPresenceType")
    private Optional<PresenceType> newPt;

    @JsonProperty("CurrentComparisonType")
    ComparisonType currentCt;

    @JsonProperty("CurrentPresenceType")
    PresenceType currentPt;

    Optional<TemplateEntryMeta> parentMeta = Optional.empty();

    public static final String METHODS_ALL = "ALL";
    public static final String EMPTY = "";


    public TemplateEntryMeta(Action action, Type reqOrResp, String service, String apiPath,
        Optional<String> method, String jsonPath, ComparisonType currentCt, PresenceType currentPt,
        Optional<ComparisonType> newCt, Optional<PresenceType> newPt,
        Optional<TemplateEntryMeta> parentMeta, RuleStatus ruleStatus) {
        this.ruleStatus = ruleStatus;
        this.reqOrResp = reqOrResp;
        this.service = service;
        this.apiPath = apiPath;
        this.method = method.orElse(METHODS_ALL);
        this.jsonPath = jsonPath;
        this.sourceRulePath = jsonPath;
        this.currentCt = currentCt;
        this.currentPt = currentPt;
        setNewCt(newCt);
        setNewPt(newPt);
        this.parentMeta = parentMeta;
        this.action = action;
    }



    @JsonGetter("NewComparisonType")
    public String getNewCt() { return newCt.map(ct -> ct.toString()).orElse(EMPTY); }

    @JsonGetter("NewPresenceType")
    public String getNewPt() {return newPt.map(pt -> pt.toString()).orElse(EMPTY); }

    @JsonSetter("NewComparisonType")
    public void setNewCt(String newCt) {
        this.newCt = Utils.valueOf(ComparisonType.class, newCt);
    }

    @JsonSetter("NewPresenceType")
    public void setNewPt(String newPt) {this.newPt = Utils.valueOf(PresenceType.class, newPt);}

    public void setNewCt(Optional<ComparisonType> newCt) { this.newCt = newCt; }

    public void setNewPt(Optional<PresenceType> newPt) { this.newPt = newPt;}


    //    public TemplateEntryMeta(String service, String apiPath, Type reqOrResp, Optional<String> method,
//        String jsonPath) {
//        this(RuleType.Undefined, Type.DontCare, service, apiPath,
//            method, jsonPath, ComparisonType.Default, PresenceType.Default, Optional.empty(),
//            Optional.empty(), Optional.empty(), Action.Remove);
//    }

    enum RuleStatus {
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

    enum Action{
        Create,
        Remove,
        None
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
