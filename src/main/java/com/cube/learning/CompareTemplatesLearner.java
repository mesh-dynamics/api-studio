package com.cube.learning;

import com.cube.core.CompareTemplateVersioned;
import com.cube.dao.ReqRespStore;
import com.cube.golden.TemplateSet;
import com.cube.learning.TemplateEntryMeta.Action;
import com.cube.learning.TemplateEntryMeta.RuleStatus;
import io.md.core.Comparator.Diff;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompareTemplatesLearner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompareTemplatesLearner.class);

    String customer;
    String app;
    String templateVersion;
    ReqRespStore rrstore;

    public CompareTemplatesLearner(String customer, String app, String templateVersion,
        ReqRespStore rrstore) {
        this.customer = customer;
        this.app = app;
        this.templateVersion = templateVersion;
        this.rrstore = rrstore;
    }

    Optional<TemplateEntry> getEffectiveTemplateEntry(RulesKey key) {
        return getTemplate(key.getTemplateKey())
            .map(template -> template.getRule(key.getJsonPath()));
    }

    Optional<CompareTemplate> getTemplate(TemplateKey key){
        try {
            return Optional.of(rrstore.getComparator(key).getCompareTemplate());
        }catch (TemplateNotFoundException e) {
            return Optional.empty();
        }
    }

    Optional<TemplateEntryMeta> getParentMeta(LearningContext context, RulesKey key, TemplateEntry templateEntry) {

        if (templateEntry.getClass().equals(TemplateEntryAsRule.class)) {
            Optional<String> parentPath = ((TemplateEntryAsRule) templateEntry).parentPath;
            return parentPath.flatMap(path -> getLearnedMeta(context,
                new RulesKey(key.getTemplateKey(), path)));
        } else {
            return Optional.empty();
        }
    }

    Optional<TemplateEntryMeta> getLearnedMeta(LearningContext context, RulesKey key) {
        return context.getRule(key);
    }

    void convertTemplateEntriesToRules(LearningContext context, String service, String requestPath,
        Type type, Optional<String> method, CompareTemplate template) {

        TemplateKey templateKey = new TemplateKey(templateVersion, customer, app,
            service, requestPath, type, method, TemplateKey.DEFAULT_RECORDING);
//                existingCompareTemplatesMap.putIfAbsent(templateKey, template);
        context.addCoveredKey(templateKey);

        if (type == Type.RequestCompare || type == Type.ResponseCompare) {
            // Convert existing template rules to learnt template metas
            template.getRules().forEach(templateEntry ->
                {
                    context.addRule(
                        new RulesKey(templateKey, templateEntry.path),
                        new TemplateEntryMeta(Action.Remove, type,
                            service,
                            requestPath, method, templateEntry.path,
                            templateEntry.getCompareType(),
                            templateEntry.getPresenceType(),
                            Optional.empty(), Optional.empty(), Optional.empty(),
                            RuleStatus.UnusedExisting
                        ));
                }
            );
        }
    }

    RuleStatus violatesRule(RuleStatus status) {
        RuleStatus negatedStatus;
        switch (status) {
            case ConformsToDefault:
            case ViolatesDefault:
                negatedStatus = RuleStatus.ViolatesDefault;
                break;
            case ConformsToExact:
            case ViolatesExact:
                negatedStatus = RuleStatus.ViolatesExact;
                break;
            case ConformsToInherited:
            case ViolatesInherited:
                negatedStatus = RuleStatus.ViolatesInherited;
                break;
            default:
                negatedStatus = status;
        }
        return negatedStatus;
    }


    void addLearnedTemplateMetas(LearningContext context, Type reqOrResp, String service, Resolution resolution,
        String apiPath,
        Optional<String> method, String jsonPath) {

        TemplateKey templateKey = new TemplateKey(templateVersion, customer, app, service, apiPath,
            reqOrResp);

        if (!context.isKeyCovered(templateKey)) {
            context.addCoveredKey(templateKey);
            getTemplate(templateKey)
                .ifPresent(template -> convertTemplateEntriesToRules(context, service,
                    apiPath, reqOrResp, method, template));
        }

        RulesKey rulesKey = new RulesKey(templateKey, jsonPath);

        Optional<TemplateEntryMeta> existingLearnedMeta = context.getRule(rulesKey);

        Optional<TemplateEntryMeta> existingParentMeta = Optional.empty();

        ComparisonType currentCt;
        PresenceType currentPt;
        Optional<ComparisonType> newCt = Optional.empty();
        Optional<PresenceType> newPt = Optional.empty();
        RuleStatus ruleStatus;
        Integer count = 0, numViolationsComparison = 0, numViolationsPresence = 0;
        Action action = Action.None;

        if (existingLearnedMeta.isPresent()) {
            TemplateEntryMeta meta = existingLearnedMeta.get();
            currentCt = meta.currentCt;
            currentPt = meta.currentPt;
            ruleStatus = meta.ruleStatus == RuleStatus.UnusedExisting ? RuleStatus.ConformsToExact
                    : meta.ruleStatus;
            count = meta.count + 1;
            numViolationsComparison = meta.numViolationsComparison;
            numViolationsPresence = meta.numViolationsPresence;
            action = meta.action;
        } else {
            Optional<TemplateEntry> effectiveTemplateEntry = getEffectiveTemplateEntry(rulesKey);
            if (effectiveTemplateEntry.isPresent()) {
                existingParentMeta = getParentMeta(context, rulesKey, effectiveTemplateEntry.get());
                if (existingParentMeta.isPresent()) {
                    TemplateEntryMeta parentMeta = existingParentMeta.get();
                    currentCt = parentMeta.currentCt;
                    currentPt = parentMeta.currentPt;
                    ruleStatus = RuleStatus.ConformsToInherited;
                    count = 1;

                    // Update parent's status.
                    // Note: a rule cannot simultaneously be used as both exact and inherited rule,
                    // as the moment it is inherited, that means there are sub-fields,
                    // so exact path match is not possible.

                    parentMeta.ruleStatus = RuleStatus.UsedAsInherited;
                    parentMeta.count++;
                    parentMeta.action = Action.None;
                } else {
                    // This must be a default rule
                    currentCt = effectiveTemplateEntry.get().ct;
                    currentPt = effectiveTemplateEntry.get().pt;
                    count = 1;
                    ruleStatus = RuleStatus.ConformsToDefault;
                }
            } else {
                // This branch indicates no applicable exact or inherited rule exists for this diff
                // -- which can only happen unless template rule not found and eventType cannot be determined
                // to fetch template rule!
                LOGGER.error(String.format(
                    "No default rule found as eventType couldn't be determined. "
                        + "No recorded event in database for template=%s "
                        + "customer=%s app=%s api=%s service=%s", templateVersion, customer,
                    app, apiPath, service));
                currentCt = ComparisonType.Ignore;
                currentPt = PresenceType.Optional;
                ruleStatus = RuleStatus.ConformsToDefault;
                count = 1;
            }
        }

        switch (resolution) {

            case OK_Ignore:
            case OK_DefaultCT:
            case OK_DefaultPT:
            case OK_Optional:
            case OK_OtherValInvalid:
                // Diff conforms to existing rules, so no action required
                break;

            case ERR_ValMismatch:
                newCt = Optional.of(ComparisonType.Ignore);
                ruleStatus = violatesRule(ruleStatus);
                numViolationsComparison++;
                action = Action.Create;
                break;

            case ERR_NewField:
            case ERR_Required:
            case ERR_RequiredGolden:
                newPt = Optional.of(PresenceType.Optional);
                ruleStatus = violatesRule(ruleStatus);
                numViolationsPresence++;
                action = Action.Create;
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

        TemplateEntryMeta meta = new TemplateEntryMeta(action, reqOrResp, service, apiPath,
            method, jsonPath, currentCt, currentPt, newCt, newPt, existingParentMeta, ruleStatus);

        meta.count = count;
        meta.numViolationsComparison = numViolationsComparison;
        meta.numViolationsPresence = numViolationsPresence;
        context.addRule(rulesKey, meta);
    }

    private void diffToLearnedTemplateMetas(LearningContext context, List<Diff> diffs, Type type, String service,
        String apiPath, Optional<String> method) {
        diffs.forEach(diff ->
            addLearnedTemplateMetas(context, type, service, diff.resolution, apiPath, method, diff.path));
    }

    public List<TemplateEntryMeta> learnComparisonRules(Map<String, String> reqIdToMethodMap,
        List<ReqRespMatchResult> reqRespMatchResultList,
        Optional<TemplateSet> existingTemplateSet,
        Boolean includeConforming) {

        LearningContext context = new LearningContext();

        existingTemplateSet.ifPresent(templateSet -> templateSet.templates
            .forEach(template -> convertTemplateEntriesToRules(context, template.service,
                template.requestPath, template.type, template.method, template)));

        reqRespMatchResultList.forEach(res -> {
                Optional<String> method = res.recordReqId.map(reqIdToMethodMap::get);

                diffToLearnedTemplateMetas(context, res.reqCompareRes.diffs, Type.RequestCompare, res.service,
                    res.path, method);
                diffToLearnedTemplateMetas(context, res.respCompareRes.diffs, Type.ResponseCompare, res.service,
                    res.path, method);
            }
        );

        return generateComparisonRules(context, includeConforming);
    }

    public List<TemplateEntryMeta> generateComparisonRules(LearningContext context,
        Boolean includeConforming) {
        List<TemplateEntryMeta> templateEntryMetaList = context.getAllRules();

        if (!includeConforming) {
            templateEntryMetaList.removeIf(templateEntryMeta -> templateEntryMeta.ruleStatus
                == RuleStatus.ConformsToInherited);
        }

        Collections.sort(templateEntryMetaList);

        int[] id = {0};
        templateEntryMetaList.forEach(meta -> meta.id = String.valueOf(id[0]++));
        templateEntryMetaList.forEach(
            meta -> meta.parentMeta.ifPresent(parentMeta -> {
                meta.inheritedRuleId = parentMeta.id;
                meta.sourceRulePath = parentMeta.jsonPath;
            }));
        return templateEntryMetaList;
    }

    private static class RulesKey {

        private static final String EMPTY_RECORDING = "";
        private final String jsonPath;
        private final TemplateKey templateKey;

        public String getJsonPath() {
            return jsonPath;
        }

        public TemplateKey getTemplateKey() {
            return templateKey;
        }

        public RulesKey(TemplateKey templateKey, String jsonPath) {
            this.templateKey = templateKey;
            this.jsonPath = jsonPath;
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
            return this.templateKey.equals(that.templateKey) &&
                this.jsonPath.equals(that.jsonPath);
        }

        @Override
        public int hashCode() {
            return Objects
                .hash(templateKey.getPath(), templateKey.getServiceId(), templateKey.getReqOrResp(),
                    getJsonPath());
        }
    }

    private static class LearningContext {

        private final HashMap<RulesKey, TemplateEntryMeta> learnedMetasMap = new HashMap<>();
        private final Set<TemplateKey> coveredTemplates = new HashSet();

        public void addCoveredKey(TemplateKey key) {
            coveredTemplates.add(key);
        }

        public boolean isKeyCovered(TemplateKey key){
            return coveredTemplates.contains(key);
        }

        public void addRule(RulesKey key, TemplateEntryMeta rule) {
            learnedMetasMap.put(key, rule);
        }

        public Optional<TemplateEntryMeta> getRule(RulesKey key) {
            return Optional.ofNullable(learnedMetasMap.get(key)).or(() ->
                key.templateKey.getMethod().flatMap(method -> {
                        // try querying with key without method
                        TemplateKey templateKey = key.templateKey;
                        final TemplateKey templateKeyWithoutMethod = new TemplateKey(
                            templateKey.getVersion(), templateKey.getCustomerId(),
                            templateKey.getAppId(), templateKey.getServiceId(), templateKey.getPath(),
                            templateKey.getReqOrResp(), Optional.empty(), templateKey.getRecording());
                    return Optional.ofNullable(learnedMetasMap
                        .get(new RulesKey(templateKeyWithoutMethod, key.getJsonPath())));
                    }
                )
            );
        }

        public List<TemplateEntryMeta> getAllRules(){
            return new ArrayList<>(learnedMetasMap.values());
        }
    }
}
