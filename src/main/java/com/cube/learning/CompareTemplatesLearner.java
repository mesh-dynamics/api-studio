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
import javax.swing.text.html.Option;

public class CompareTemplatesLearner {

    HashMap<TemplateEntryMeta, TemplateEntryMeta> learnedMetasMap = new HashMap<>();
    // Creating our own map to restrict search to user-defined rules. ComparatorCache returns
    // default rules as well when no user-defined rules are found for a particular service/api.
    HashMap<CompareTemplateMapKey, CompareTemplate> existingCompareTemplatesMap = new HashMap<>();
    // Used to get the default rule
    String customer;
    String app;
    String templateVersion;
    ReqRespStore rrstore;

    public CompareTemplatesLearner(String customer, String app, String templateVersion, ReqRespStore rrstore) {
        this.customer = customer;
        this.app = app;
        this.templateVersion = templateVersion;
        this.rrstore = rrstore;
    }

    Optional<TemplateEntry> getDecisiveExistingRule(String service, String apiPath, Type reqOrResp, Optional<String> method,
        String jsonPath) {
        return Optional
            .ofNullable(existingCompareTemplatesMap.get(new CompareTemplateMapKey(service, apiPath,
                reqOrResp, method)))
            .map(template -> template.getRule(jsonPath));
    }

    Optional<TemplateEntryMeta> getExistingParentMeta(String service, String apiPath, Type reqOrResp, Optional<String> method,
        String jsonPath) {

        // The rule can come from a user-defined template or default template. Both in user-defined
        // template and default template, if a jsonPath is not found, a default rule (DEFAULT_CT/DEFAULT_PT) would be
        // used to resolve the diff.

        // This is an existing compare template with a non-default rule,
        // hence an existing parent meta must be existing (since a user exact meta
        // wasn't found for it and we had indexed all the user-templates to metas)
        return getDecisiveExistingRule(service, apiPath,
            reqOrResp, method, jsonPath).flatMap(existingTemplate -> {
            if (!isDefaultRule(existingTemplate) && existingTemplate.getClass().equals(TemplateEntryAsRule.class)) {
                Optional<String> parentPath = ((TemplateEntryAsRule) existingTemplate).parentPath;
                return parentPath
                    .flatMap(path -> getLearnedMeta(service, apiPath, reqOrResp, method,
                        path));
            }else
                return Optional.empty();
        });
    }

    Optional<TemplateEntry> getDefaultRule(String service, String apiPath, Type reqOrResp, Optional<String> method,
        String jsonPath) {
        // In case there is a compare template for the service, apiPath, method, it will return
        // an existing rule.
        try {
            return Optional.of(rrstore.getComparator(new TemplateKey(templateVersion, customer, app, service, apiPath,
                    reqOrResp, method, TemplateKey.DEFAULT_RECORDING)).getCompareTemplate().getRule(jsonPath));
        } catch (TemplateNotFoundException e) {
            return Optional.empty();
        }
    }

    Optional<TemplateEntryMeta> getLearnedMeta(String service, String apiPath, Type reqOrResp, Optional<String> method,
        String jsonPath) {

        return Optional.ofNullable(learnedMetasMap
            .get(new TemplateEntryMeta(service, apiPath, reqOrResp, method.orElse(TemplateEntryMeta.METHODS_ALL), jsonPath)));

    }

    boolean isDefaultRule(TemplateEntry templateEntry){
        return (templateEntry.ct == ComparisonType.Default || templateEntry.pt == PresenceType.Default);
    }

    YesOrNo getValueMatchRequired(TemplateEntry templateEntry){
        return templateEntry.ct == ComparisonType.Equal ? YesOrNo.yes
            : YesOrNo.no;
    }

    YesOrNo getPresenceRequired(TemplateEntry templateEntry){
        //TODO: Comparison type has a third level called equalOptional. Cater to that as well.

        // Handles default_pt and default_ct case as well
        return templateEntry.pt == PresenceType.Required ? YesOrNo.yes
            : YesOrNo.no;
    }

    void convertExistingTemplatesToMetas(TemplateSet existingTemplateSet, Map<String, String> reqIdToMethodMap){

            existingTemplateSet.templates.forEach(template -> {
                // Put compare template in existing compare templates map
                String apiPath = template.requestPath;
                String service = template.service;
                Type reqOrResp = template.type;
                Optional<String> method = template.method;

                CompareTemplateMapKey key = new CompareTemplateMapKey(
                    service, apiPath, reqOrResp, method);
                existingCompareTemplatesMap.putIfAbsent(key, template);

                if (reqOrResp == Type.RequestCompare || reqOrResp == Type.ResponseCompare) {
                    // Convert existing template rules to learnt template metas
                    template.getRules().forEach(templateEntry ->
                        {
                            TemplateEntryMeta templateEntryMeta = new TemplateEntryMeta(Optional.empty(),
                                Optional.empty(),
                                RuleStatus.UnusedExisting,
                                reqOrResp, service, apiPath,
                                method.orElse(TemplateEntryMeta.METHODS_ALL),
                                templateEntry.path, 0, 0
                                , getValueMatchRequired(templateEntry),
                                getPresenceRequired(templateEntry),
                                Optional.empty());
                            learnedMetasMap
                                .putIfAbsent(templateEntryMeta, templateEntryMeta);
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


    void addLearnedTemplateMetas(Type reqOrResp, String service,
        Resolution resolution, String apiPath, Optional<String> method, String jsonPath) {

        Optional<TemplateEntryMeta> existingLearnedMeta = getLearnedMeta(service, apiPath,
            reqOrResp, method, jsonPath);

        Optional<TemplateEntryMeta> existingParentMeta =
            existingLearnedMeta.isPresent() ? Optional.empty()
                : getExistingParentMeta(service, apiPath, reqOrResp, method, jsonPath);

        Optional<TemplateEntry> defaultTemplateEntry =
            existingParentMeta.isPresent() ? Optional.empty()
                : getDefaultRule(service, apiPath, reqOrResp, method, jsonPath);


        YesOrNo valueMatchRequired;
        YesOrNo presenceRequired;
        RuleStatus oldRuleStatus;
        Integer count = 0, numViolations = 0;


        if (existingLearnedMeta.isPresent()) {
            TemplateEntryMeta meta = existingLearnedMeta.get();
            valueMatchRequired = meta.valueMatchRequired;
            presenceRequired = meta.presenceRequired;
            oldRuleStatus = meta.ruleStatus;
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
                Optional<TemplateEntry> defaultTemplateRule = getDefaultRule(service, apiPath,
                    reqOrResp, method, jsonPath);
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
                break;

            case OK_Optional:
            case OK_OtherValInvalid:
                presenceRequired = YesOrNo.no;
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
            TemplateEntryMeta meta = new TemplateEntryMeta(Optional.empty(), Optional.empty(), newRuleStatus,
                reqOrResp, service, apiPath, method.orElse(TemplateEntryMeta.METHODS_ALL),
                jsonPath, count, numViolations, valueMatchRequired,
                presenceRequired, existingParentMeta);
            learnedMetasMap.put(meta, meta);

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
                        addLearnedTemplateMetas(Type.RequestCompare, res.service, diff.resolution,
                            res.path, method, diff.path);
                        ;
                    });

                res.respCompareRes.diffs.forEach(
                    diff -> {
                        addLearnedTemplateMetas(Type.ResponseCompare, res.service, diff.resolution,
                            res.path, method, diff.path);
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

    private static class CompareTemplateMapKey {

        String apiPath;
        String service;
        Type eventType;
        String method;

        private static final String METHODS_ALL = "ALL";

        public CompareTemplateMapKey(String service, String apiPath, Type eventType,
            Optional<String> method) {
            this.apiPath = apiPath;
            this.service = service;
            this.eventType = eventType;
            this.method = method.orElse(METHODS_ALL);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CompareTemplateMapKey that = (CompareTemplateMapKey) o;
            return apiPath.equals(that.apiPath) &&
                service.equals(that.service) &&
                eventType == that.eventType &&
                (method.equals(that.method) || method.equals(METHODS_ALL) || that.method.equals(METHODS_ALL));
        }

        @Override
        public int hashCode() {
            return Objects.hash(apiPath, service, eventType);
        }
    }
}
