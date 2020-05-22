package com.cube.golden.transform;

import static io.md.core.TemplateKey.*;
import static io.md.services.DataStore.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import io.md.core.AttributeRuleMap;
import io.md.core.CompareTemplate;
import io.md.core.TemplateEntry;
import io.md.core.TemplateKey;
import io.md.dao.ReqRespUpdateOperation.OperationType;
import io.md.services.DataStore;

import com.cube.cache.ComparatorCache;
import com.cube.core.CompareTemplateVersioned;
import com.cube.dao.ReqRespStore;
import com.cube.golden.SingleTemplateUpdateOperation;
import com.cube.golden.TemplateEntryOperation;
import com.cube.golden.TemplateSet;
import com.cube.golden.TemplateUpdateOperationSet;
import com.cube.utils.Constants;

public class TemplateSetTransformer {

    private static final Logger LOGGER = LogManager.getLogger(TemplateSetTransformer.class);

    /**
     * Update a template set based on a given template update operation set
     * @param sourceTemplateSet Source Template Set
     * @param templateSetUpdateSpec Update Operations to arrive at the new Template Set
     * @param rrstore
     * @return Updated Template Set (needs to be stored in backend explicitly later)
     */
    public TemplateSet updateTemplateSet(TemplateSet sourceTemplateSet
        , TemplateUpdateOperationSet templateSetUpdateSpec, ReqRespStore rrstore)
        throws Exception {
        List<CompareTemplateVersioned> sourceTemplates = sourceTemplateSet.templates;
        Map<TemplateKey, SingleTemplateUpdateOperation> updates =   templateSetUpdateSpec.getTemplateUpdates();
        String newVersion = UUID.randomUUID().toString();

        Map<TemplateKey, CompareTemplateVersioned> sourceTemplateMap = sourceTemplates.stream()
            .collect(Collectors.toMap(template -> new TemplateKey(sourceTemplateSet.version,
            sourceTemplateSet.customer, sourceTemplateSet.app, template.service,
            template.requestPath, template.type) , Function.identity()));

        // This is the key to be set by UI for attributeLevelRules
        TemplateKey attributeTemplateKey = new TemplateKey(sourceTemplateSet.version, sourceTemplateSet.customer,
            sourceTemplateSet.app, io.md.constants.Constants.NOT_APPLICABLE, io.md.constants.Constants.NOT_APPLICABLE,
            Type.DontCare);

        // Fetch and store previous existing attribute rules.
        Map<String, TemplateEntry> pathVsEntryAttributes = sourceTemplateSet.appAttributeRuleMap
            .map(attributeRuleMap -> {
                return new HashMap(attributeRuleMap.getAttributeNameVsRule());
            }).orElse(new HashMap());

        updates.forEach((key , update) -> {
            if (key.equals(attributeTemplateKey)) {
                Collection<TemplateEntryOperation> atomicUpdateOperations = update.getOperationList();

                atomicUpdateOperations.forEach(updateOperation -> {
                    populateRules(CompareTemplateVersioned.EMPTY_COMPARE_TEMPLATE_VERSION,
                    pathVsEntryAttributes, updateOperation);
                });
            } else {
                CompareTemplateVersioned sourceTemplate = null;
                if (sourceTemplateMap.containsKey(key)) {
                    // for each existing template (identified by a template key) ,
                    // check if any updates are specified in the update set ,
                    // create a new template with new rules , and add it to the set of new templates ,
                    // note that we are only performing updates here, for any new service / api
                    // path we won't be adding any new templates to the template set
                    // (that might be taken care of in forward testing)
                    sourceTemplate = sourceTemplateMap.get(key);
                } else {
                    CompareTemplate template = null;
                    try {
                        // try to get default compare template based on event type
                        // queried from backend store for the given api path
                        template = rrstore
                            .getComparator(key).getCompareTemplate();
                    } catch (TemplateNotFoundException e) {
                        LOGGER.error(new ObjectMessage(Map.of(
                            Constants.MESSAGE, "Unable to fetch DEFAULT template from comparator " +
                                "cache during template set update", Constants.TEMPLATE_KEY_FIELD, key.toString())), e);
                        template = new CompareTemplate();
                    }
                    // no need to clone now .. as update will take care of it
                    sourceTemplate = new CompareTemplateVersioned(Optional.of(key.getServiceId())
                        , Optional.of(key.getPath()), key.getReqOrResp(), template);
                }
                CompareTemplateVersioned updated = updateTemplate(sourceTemplate , update);
                sourceTemplateMap.put(key, updated);
                LOGGER.debug(new ObjectMessage(Map.of(Constants.MESSAGE, "Updated Compare Template"
                    , Constants.TEMPLATE_UPD_OP_SET_ID_FIELD, templateSetUpdateSpec.getTemplateUpdateOperationSetId()
                    , Constants.TEMPLATE_KEY_FIELD , key.toString(), Constants.OLD_TEMPLATE_SET_VERSION,
                    sourceTemplateSet.version)));
            }
        });

        List<CompareTemplateVersioned> updatedCompareTemplates = new ArrayList<>(sourceTemplateMap.values());
        return new TemplateSet(newVersion, sourceTemplateSet.customer
            , sourceTemplateSet.app, Instant.now(), updatedCompareTemplates, pathVsEntryAttributes.isEmpty() ? Optional.empty() : Optional.of(new AttributeRuleMap(pathVsEntryAttributes)));
    }

    /**
     * Update a template given update operations
     * @param sourceTemplate The existing template
     * @param update The update operations for the template
     * @return The updated template
     */
    private CompareTemplateVersioned updateTemplate(CompareTemplateVersioned sourceTemplate
        , SingleTemplateUpdateOperation update) {
        Collection<TemplateEntryOperation> atomicUpdateOperations = update.getOperationList();
        Collection<TemplateEntry> sourceTemplateRules = sourceTemplate.getRules();
        // this will the final set of rules for the new template
        Map<String, TemplateEntry> pathVsEntryTemplates = new HashMap<>();
        sourceTemplateRules.forEach(templateEntry -> {
            pathVsEntryTemplates.put(templateEntry.getPath(), templateEntry);
        });

        atomicUpdateOperations.forEach(updateOperation -> {
                populateRules(sourceTemplate, pathVsEntryTemplates, updateOperation);
        });

        // These compare templates don't have the attribute maps in them.
        // Basically it is an inconsistent state. Unless the new version is read back
        // We don't have a valid compare template with the attribute map since it being a transient field
        return new CompareTemplateVersioned(sourceTemplate, pathVsEntryTemplates.values());
    } //).orElse(new CompareTemplateVersioned(sourceTemplate));

    private void populateRules(CompareTemplateVersioned sourceTemplate,
        Map<String, TemplateEntry> pathVsEntry, TemplateEntryOperation updateOperation) {
        String normalisedPath = sourceTemplate.getNormalisedPath(updateOperation.getPath()).toString();
        OperationType operationType = updateOperation.getOperationType();
        if (operationType.equals(OperationType.REMOVE)) {
            // remove the rule on a delete operation
            LOGGER.debug(new ObjectMessage(Map.of(Constants.MESSAGE, "Removing rule",
                Constants.JSON_PATH_FIELD , normalisedPath, Constants.SERVICE_FIELD, sourceTemplate.service,
                Constants.API_PATH_FIELD , sourceTemplate.requestPath)));
            pathVsEntry.remove(normalisedPath);
        } else if (operationType.equals(OperationType.ADD) || operationType.equals(OperationType.REPLACE)) {
            if (updateOperation.getNewRule().isEmpty()) {
                LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
                    "New Rule Not Available for Add/Replace Operation"
                    , Constants.JSON_PATH_FIELD , normalisedPath)));
            } else {
                // for add or replace ... for the given json path .. add the new rule
                // (in case of replace the entire rule will be replaced with the new rule)
                TemplateEntry newRule = updateOperation.getNewRule().get();
                // normalisedPath can also be directly used while creating new rule but getNormalisedPath is called again to be safe
                // in case the updateOperation's path  and rule's path are different
                TemplateEntry newRuleNormalised = new TemplateEntry(sourceTemplate.getNormalisedPath(newRule.getPath()).toString(), newRule.getDataType(),
                    newRule.getPresenceType(), newRule.getCompareType(), newRule.getExtractionMethod(), newRule.getCustomization());
                LOGGER.debug(new ObjectMessage(Map.of(Constants.MESSAGE, "Replacing with new rule",
                    Constants.JSON_PATH_FIELD , normalisedPath, Constants.SERVICE_FIELD, sourceTemplate.service,
                    Constants.API_PATH_FIELD , sourceTemplate.requestPath)));
                pathVsEntry.put(normalisedPath, newRuleNormalised);
            }
        }
    }

}
