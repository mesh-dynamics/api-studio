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

package com.cube.learning;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.md.core.CompareTemplate.ComparisonType;
import io.md.core.CompareTemplate.DataType;
import io.md.core.CompareTemplate.ExtractionMethod;
import io.md.core.CompareTemplate.PresenceType;
import io.md.core.TemplateKey.Type;
import io.md.core.Utils;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

@JsonPropertyOrder({"Id", "Action", "Service", "ApiPath", "Method", "EventType", "JsonPath",
    "SourceRulePath", "CurrentComparisonType", "CurrentPresenceType",
    "NewComparisonType", "NewPresenceType", "InheritedRuleId", "Count",
    "NumViolationsComparison", "NumViolationsPresence", "DataType", "ExtractionMethod",
    "Customization", "ArrayComparisonKeyPath"})

public class TemplateEntryMeta implements Comparable{
    @JsonProperty("Id")
    String id;

    @JsonProperty("InheritedRuleId")
    private Optional<String> inheritedRuleId = Optional.empty();

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
    private Optional<String> method;

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

    @JsonIgnore
    DataType dt;

    @JsonIgnore
    ExtractionMethod em;

    Optional<String> arrayComparisionKeyPath;

    Optional<String> customization;

    Optional<TemplateEntryMeta> parentMeta = Optional.empty();

    public static final String METHODS_ALL = "ALL";
    public static final String EMPTY = "";

    public TemplateEntryMeta(){
        // Default constructor for jsonMapper
    }


    public TemplateEntryMeta(Action action, Type reqOrResp, String service, String apiPath,
        Optional<String> method, String jsonPath, ComparisonType currentCt, PresenceType currentPt,
        Optional<ComparisonType> newCt, Optional<PresenceType> newPt,
        DataType dt, ExtractionMethod em,
        Optional<String> customization, Optional<String> arrayCompKeyPath,
        Optional<TemplateEntryMeta> parentMeta,
        RuleStatus ruleStatus) {
        this.ruleStatus = ruleStatus;
        this.reqOrResp = reqOrResp;
        this.service = service;
        this.apiPath = apiPath;
        setMethod(method);
        this.jsonPath = jsonPath;
        this.sourceRulePath = jsonPath;
        this.currentCt = currentCt;
        this.currentPt = currentPt;
        this.dt = dt;
        this.em = em;
        this.customization = customization;
        this.arrayComparisionKeyPath = arrayCompKeyPath;
        setNewCt(newCt);
        setNewPt(newPt);
        this.parentMeta = parentMeta;
        this.action = action;
    }

    @JsonGetter("InheritedRuleId")
    public String getInheritedRuleIdAsString() {
        return inheritedRuleId.orElse(EMPTY);
    }

    @JsonSetter("InheritedRuleId")
    public void setInheritedRuleId(String inheritedRuleId) {
        this.inheritedRuleId =
            inheritedRuleId.equals(EMPTY) ? Optional.empty() : Optional.of(inheritedRuleId);
    }

    public void setInheritedRuleId(Optional<String> inheritedRuleId) {
        this.inheritedRuleId = inheritedRuleId;
    }

    public void setMethod(Optional<String> method) {
        this.method = method;
    }

    @JsonSetter("Method")
    public void setMethod(String method) {
        this.method = method.equals(METHODS_ALL) ? Optional.empty() : Optional.of(method);
    }

    @JsonGetter("Method")
    public String getMethodAsString() {
        return method.orElse(METHODS_ALL);
    }

    public Optional<String> getMethod() {
        return method;
    }


    @JsonGetter("NewComparisonType")
    public String getNewCtAsString() {
        return newCt.map(Enum::toString).orElse(EMPTY);
    }

    public Optional<ComparisonType> getNewCt() {
        return newCt;
    }

    @JsonSetter("NewComparisonType")
    public void setNewCt(String newCt) {
        this.newCt = Utils.valueOf(ComparisonType.class, newCt);
    }

    public void setNewCt(Optional<ComparisonType> newCt) {
        this.newCt = newCt;
    }


    @JsonGetter("NewPresenceType")
    public String getNewPtAsString() {
        return newPt.map(Enum::toString).orElse(EMPTY);
    }

    public Optional<PresenceType> getNewPt() {
        return newPt;
    }

    @JsonSetter("NewPresenceType")
    public void setNewPt(String newPt) {
        this.newPt = Utils.valueOf(PresenceType.class, newPt);
    }

    public void setNewPt(Optional<PresenceType> newPt) {
        this.newPt = newPt;
    }

    @JsonGetter("JsonPath")
    public String getJsonPath() {
        return jsonPath;
    }

    @JsonSetter("JsonPath")
    public void setJsonPath(String jsonPath) {
        this.jsonPath = jsonPath;
    }

    public ComparisonType getCurrentCt() {
        return currentCt;
    }

    @JsonGetter("CurrentComparisonType")
    public String getCurrentCtAsString() {
        return currentCt.toString();
    }

    @JsonSetter("CurrentComparisonType")
    public void setCurrentCt(String currentCt) {
        this.currentCt = Utils.valueOf(ComparisonType.class, currentCt)
            .orElse(ComparisonType.Ignore);
    }

    public PresenceType getCurrentPt() {
        return currentPt;
    }

    @JsonGetter("CurrentPresenceType")
    public String getCurrentPtAsString() {
        return currentPt.toString();
    }

    @JsonSetter("CurrentPresenceType")
    public void setCurrentPt(String currentPt) {
        this.currentPt = Utils.valueOf(PresenceType.class, currentPt)
            .orElse(PresenceType.Optional);
    }

    public DataType getDt() {
        return dt;
    }

    @JsonGetter("DataType")
    public String getDtAsString() {
        return dt.toString();
    }

    @JsonSetter("DataType")
    public void setDt(String dt) {
        this.dt = Utils.valueOf(DataType.class, dt)
            .orElse(DataType.Default);
    }

    public ExtractionMethod getEm() {
        return em;
    }

    @JsonGetter("ExtractionMethod")
    public String getEmAsString() {
        return em.toString();
    }

    @JsonSetter("ExtractionMethod")
    public void setEm(String em) {
        this.em = Utils.valueOf(ExtractionMethod.class, em)
            .orElse(ExtractionMethod.Default);
    }

    @JsonGetter("Customization")
    public String getCustomization() {
        return customization.orElse(EMPTY);
    }

    @JsonSetter("Customization")
    public void setCustomization(String customization) {
        this.customization =
            customization.equals(EMPTY) ? Optional.empty() : Optional.of(customization);
    }

    @JsonGetter("ArrayComparisonKeyPath")
    public String getArrayCompPath() {
        return arrayComparisionKeyPath.orElse(EMPTY);
    }

    @JsonSetter("ArrayComparisonKeyPath")
    public void setArrayCompPath(String arrayCompKeyPath) {
        arrayComparisionKeyPath =
            arrayCompKeyPath.equals(EMPTY) ? Optional.empty() : Optional.of(arrayCompKeyPath);
    }



    enum RuleStatus {
        // IMP: Order of fields is used for sorting.
        ViolatesExact, // Instance violates an exact rule of expected match.
        ViolatesInherited, // Instance violates an inherited rule of expected match.
        ViolatesDefault, // Violates a default rule
        UnusedExisting,  // Already configured rule that wasn't exercised.
        ConformsToExact, // Instance complies with an exact rule already configured to ignore mismatch
        ConformsToInherited, // Instance complies with an inherited rule already configured to ignore mismatch
        UsedAsInherited,  // Already configured rule that was exercised as inherited rule.
        ConformsToDefault,  // Mismatch when no rule configured, or exact/inherited rule from template with behaviour also to ignore it.
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
        if (this.action != that.action)
            return this.action.compareTo(that.action); // This sorts in declaration order in enum.
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

    @Override
    public String toString(){
        return String
            .format("service=%s api=%s method=%s reqOrResp=%s jsonPath=%s", service, apiPath,
                getMethodAsString(), reqOrResp, jsonPath);
    }
}
