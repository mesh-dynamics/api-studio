package com.cube.learning;

import com.cube.dao.ReqRespStore;
import com.cube.golden.TemplateSet;
import com.cube.learning.TemplateEntryMeta.RuleStatus;
import com.cube.learning.TemplateEntryMeta.YesOrNo;
import io.md.core.Comparator.Resolution;
import io.md.core.CompareTemplate;
import io.md.core.CompareTemplate.ComparisonType;
import io.md.core.CompareTemplate.PresenceType;
import io.md.core.TemplateEntry;
import io.md.core.TemplateEntryAsRule;
import io.md.core.TemplateKey;
import io.md.core.TemplateKey.Type;
import io.md.dao.ReqRespMatchResult;
import io.md.services.DataStore.TemplateNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompareTemplatesLearner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompareTemplatesLearner.class);

    HashMap<RulesKey, TemplateEntryMeta> learnedMetasMap = new HashMap<>();
    // Creating our own map to restrict search to user-defined rules. ComparatorCache returns
    // default rules as well when no user-defined rules are found for a particular service/api.
    HashMap<RulesKey, CompareTemplate> existingCompareTemplatesMap = new HashMap<>();
    // Used to get the default rule
    String customer;
    String app;
    String templateVersion;
    ReqRespStore rrstore;

    RulesKey createRulesKey(String service, String apiPath,  Type reqOrResp, Optional<String> method, Optional<String> jsonPath){
        return new RulesKey(templateVersion, customer, app, service, apiPath, reqOrResp, method, jsonPath);
    }

    public CompareTemplatesLearner(String customer, String app, String templateVersion, ReqRespStore rrstore) {
        this.customer = customer;
        this.app = app;
        this.templateVersion = templateVersion;
        this.rrstore = rrstore;
    }

    Optional<TemplateEntry> getDecisiveExistingRule(RulesKey key) {
        return Optional
            .ofNullable(existingCompareTemplatesMap.get(
                createRulesKey(key.getServiceId(), key.getPath(), key.getReqOrResp(),
                    key.getMethod(), Optional.empty())))
            .flatMap(template -> key.getJsonPath().map(path -> template.getRule(path)));
    }

    Optional<TemplateEntryMeta> getExistingParentMeta(RulesKey key) {


        return getDecisiveExistingRule(key)
            .flatMap(existingTemplate -> {
            if (!isDefaultRule(existingTemplate) && existingTemplate.getClass().equals(TemplateEntryAsRule.class)) {
                // This is an existing compare template with a non-default rule,
                // hence an existing parent meta must be existing (since a user exact meta
                // wasn't found for it and we had indexed all the user-templates to metas)
                // Not finding it indicates an error
                Optional<String> parentPath = ((TemplateEntryAsRule) existingTemplate).parentPath;
                return parentPath.flatMap(path -> getLearnedMeta(
                    createRulesKey(key.getServiceId(), key.getPath(), key.getReqOrResp(),
                        key.getMethod(), Optional.of(path))));
            }else
                return Optional.empty();
        });
    }

    Optional<TemplateEntry> getDefaultRule(RulesKey key) {
        // In case there is a compare template for the service, apiPath, method, it will return
        // an existing rule.
        return key.getJsonPath().flatMap(jsonPath -> {
            try {
                return Optional
                    .of(rrstore.getComparator(
                        new TemplateKey(templateVersion, customer, app, key.getServiceId(),
                            key.getPath(), key.getReqOrResp(), key.getMethod(),
                            TemplateKey.DEFAULT_RECORDING)).getCompareTemplate().getRule(jsonPath));
            } catch (TemplateNotFoundException e) {
                return Optional.empty();
            }
        });
    }

    Optional<TemplateEntryMeta> getLearnedMeta(RulesKey key) {
        return Optional.ofNullable(learnedMetasMap.get(key));
    }


    boolean isDefaultRule(TemplateEntry templateEntry){
        return (templateEntry.ct == ComparisonType.Default || templateEntry.pt == PresenceType.Default);
    }

    YesOrNo getValueMatchRequired(TemplateEntry templateEntry){
        //TODO: Comparison type has a third level called equalOptional. Cater to that as well.
        return templateEntry.ct == ComparisonType.Equal ? YesOrNo.yes : YesOrNo.no;
    }

    YesOrNo getPresenceRequired(TemplateEntry templateEntry){
        return templateEntry.pt == PresenceType.Required ? YesOrNo.yes: YesOrNo.no;
    }

    void convertExistingTemplatesToMetas(TemplateSet existingTemplateSet, Map<String, String> reqIdToMethodMap){

            existingTemplateSet.templates.forEach(template -> {
                // Put compare template in existing compare templates map

                RulesKey key = createRulesKey(template.service, template.requestPath,
                    template.type, template.method, Optional.empty());
                existingCompareTemplatesMap.putIfAbsent(key, template);

                if (template.type == Type.RequestCompare || template.type == Type.ResponseCompare) {
                    // Convert existing template rules to learnt template metas
                    template.getRules().forEach(templateEntry ->
                        {
                           learnedMetasMap
                                .putIfAbsent(createRulesKey(template.service, template.requestPath,
                                    template.type, template.method,
                                    Optional.of(templateEntry.path)), new TemplateEntryMeta(
                                    RuleStatus.UnusedExisting,
                                    template.type, template.service, template.requestPath,
                                    template.method,
                                    templateEntry.path,
                                    getValueMatchRequired(templateEntry),
                                    getPresenceRequired(templateEntry),
                                    Optional.empty()));
                        }
                    );
                }
            });
    }

    RuleStatus negateRule(RuleStatus rule){
        RuleStatus negatedRule;
        switch (rule) {
            case ConformsToDefault:
            case ViolatesDefault:
                negatedRule = RuleStatus.ViolatesDefault;
                break;
            case ConformsToExistingExact:
            case ViolatesExistingExact:
                negatedRule = RuleStatus.ViolatesExistingExact;
                break;
            case ConformsToExistingInherited:
            case ViolatesExistingInherited:
                negatedRule = RuleStatus.ViolatesExistingInherited;
                break;
            default:
                negatedRule = rule;
        }
        return negatedRule;
    }


    void addLearnedTemplateMetas(Type reqOrResp, String service, Resolution resolution,
        String apiPath,
        Optional<String> method, String jsonPath) {

        RulesKey key = createRulesKey(service, apiPath, reqOrResp, method, Optional.of(jsonPath));

        Optional<TemplateEntryMeta> existingLearnedMeta = getLearnedMeta(key);

        Optional<TemplateEntryMeta> existingParentMeta =
            existingLearnedMeta.isPresent() ? Optional.empty()
                : getExistingParentMeta(key);

        YesOrNo valueMatchRequired;
        YesOrNo presenceRequired;
        RuleStatus oldRuleStatus;
        Integer count = 0, numViolations = 0;


        if (existingLearnedMeta.isPresent()) {
            TemplateEntryMeta meta = existingLearnedMeta.get();
            valueMatchRequired = meta.valueMatchRequired;
            presenceRequired = meta.presenceRequired;
            oldRuleStatus =
                meta.ruleStatus == RuleStatus.UnusedExisting ? RuleStatus.ConformsToExistingExact
                    : meta.ruleStatus;
            count = meta.count + 1;
            numViolations = meta.numViolations;
        } else {
            if (existingParentMeta.isPresent()){
                TemplateEntryMeta meta = existingParentMeta.get();
                valueMatchRequired = meta.valueMatchRequired;
                presenceRequired = meta.presenceRequired;
                oldRuleStatus = RuleStatus.ConformsToExistingInherited;
                count = 1;

                // Update parent's status.
                // Note: a rule cannot simultaneously be used as both exact and inherited rule,
                // as the moment it is inherited, that means there are sub-fields,
                // so exact path match is not possible.

                meta.ruleStatus = RuleStatus.UsedExistingAsInherited;
                meta.count++;

            } else {
                // A Compare Template doesn't exist for provided service/apiPath combination,
                // or we have reached here because the rules were of default type even in user-
                // defined template as the rules for the particular JSON path didn't exist.
                // In both cases, the rule will be categorized as a default rule, the template
                // taken to reach to it may be non-default.
                Optional<TemplateEntry> defaultTemplateRule = getDefaultRule(key);
                valueMatchRequired = defaultTemplateRule.map(rule -> getValueMatchRequired(rule))
                    .orElse(YesOrNo.no);
                presenceRequired = defaultTemplateRule.map(rule -> getPresenceRequired(rule))
                    .orElse(YesOrNo.no);
                oldRuleStatus = RuleStatus.ConformsToDefault;
                count = 1;

            }
        }

        RuleStatus newRuleStatus = oldRuleStatus;


        switch (resolution) {

            case OK_Ignore:
            case OK_DefaultCT:
            case OK_DefaultPT:
            case OK_Optional:
            case OK_OtherValInvalid:
                // Diff conforms to existing rules, so no action required
                break;

            case ERR_ValMismatch:
                valueMatchRequired = YesOrNo.no;
                newRuleStatus = negateRule(oldRuleStatus);
                numViolations++;
                break;

            case ERR_NewField:
            case ERR_Required:
            case ERR_RequiredGolden:
                presenceRequired = YesOrNo.no;
                newRuleStatus = negateRule(oldRuleStatus);
                numViolations++;
                break;

            // Not handling below types as they are not related to either value or presence
            case OK:
            case OK_CustomMatch:
            case OK_OptionalMismatch:
            case ERR:
            case ERR_ValFormatMismatch:
            case ERR_ValTypeMismatch:
            case ERR_InvalidExtractionMethod:
            default:
                return;
        }

        if (existingLearnedMeta.isPresent()) {
            // Not using Optional lambdas as they accept only final variables inside lambda.
            TemplateEntryMeta meta = existingLearnedMeta.get();

                meta.ruleStatus = newRuleStatus;
                meta.presenceRequired = presenceRequired;
                meta.valueMatchRequired = valueMatchRequired;

        } else {
            TemplateEntryMeta meta = new TemplateEntryMeta(newRuleStatus,
                reqOrResp, service, apiPath, method,
                jsonPath, valueMatchRequired,
                presenceRequired, existingParentMeta);
            meta.count = count;
            meta.numViolations = numViolations;
            learnedMetasMap.put(key, meta);

        }
    }

    public List<TemplateEntryMeta> learnCompareTemplates(Map<String, String> reqIdToMethodMap,
        List<ReqRespMatchResult> reqRespMatchResultList,
        Optional<TemplateSet> existingTemplateSet) {

        existingTemplateSet.ifPresent(templateSet -> convertExistingTemplatesToMetas(templateSet, reqIdToMethodMap));

        reqRespMatchResultList.forEach(res -> {
                Optional<String>  method = res.recordReqId.map(reqIdToMethodMap::get);

                res.reqCompareRes.diffs.forEach(
                    diff -> {
                        addLearnedTemplateMetas(Type.RequestCompare, res.service, diff.resolution, res.path,
                            method, diff.path);
                        ;
                    });

                res.respCompareRes.diffs.forEach(
                    diff -> {
                        addLearnedTemplateMetas(Type.ResponseCompare, res.service, diff.resolution, res.path,
                            method, diff.path);
                    });
            }
        );

       return generateCompareTemplates();
    }

    public List<TemplateEntryMeta> generateCompareTemplates() {
        List<TemplateEntryMeta> templateEntryMetaList = new ArrayList<>(
            learnedMetasMap.values());
        Collections.sort(templateEntryMetaList);
        int[] id = {0};
        templateEntryMetaList.forEach(meta -> meta.id = String.valueOf(id[0]++));
        templateEntryMetaList.forEach(
            meta -> meta.parentMeta.ifPresent(parentMeta -> meta.inheritedRuleId = parentMeta.id));
        return templateEntryMetaList;
    }

    private static class RulesKey extends TemplateKey{

        private static final String EMPTY_RECORDING = "";
        private final Optional<String> jsonPath;

        public Optional<String> getJsonPath() {
            return jsonPath;
        }

        public RulesKey(String templateVersion, String customerId, String appId, String service, String apiPath, Type eventType, Optional<String> method, Optional<String> jsonPath) {
            super(templateVersion, customerId, appId, service, apiPath, eventType, method, EMPTY_RECORDING);
            this.jsonPath = jsonPath;
        }

        private boolean checkRegexEquals(Optional<String> string1, Optional<String> string2){
            return string1.map(
                str1 -> string2.map(str2 -> {
                    if (str1.contains(*))
                    str1.equals(str2);
                })
                    .orElse(true)).orElse(true);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RulesKey that = (RulesKey) o;
            return getPath().equals(that.getPath()) &&
                getServiceId().equals(that.getServiceId()) &&
                getReqOrResp() == that.getReqOrResp() &&
                getMethod().map(
                    method -> that.getMethod().map(thatMethod -> method.equals(thatMethod))
                        .orElse(true)).orElse(true) &&
                getJsonPath().map(
                    jsonPath -> that.getJsonPath().map(thatJsonPath -> jsonPath.equals(thatJsonPath))
                        .orElse(true)).orElse(true);
        }



        @Override
        public int hashCode() {
            return Objects.hash(getPath(), getServiceId(), getReqOrResp());
        }
    }
}
