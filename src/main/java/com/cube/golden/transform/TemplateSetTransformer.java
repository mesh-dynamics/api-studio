package com.cube.golden.transform;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cube.cache.TemplateKey;
import com.cube.core.CompareTemplateVersioned;
import com.cube.core.TemplateEntry;
import com.cube.golden.OperationType;
import com.cube.golden.SingleTemplateUpdateOperation;
import com.cube.golden.TemplateEntryOperation;
import com.cube.golden.TemplateSet;
import com.cube.golden.TemplateUpdateOperationSet;

public class TemplateSetTransformer {

    private static final Logger LOGGER = LogManager.getLogger(TemplateSetTransformer.class);

    /**
     * Update a template set based on a given template update operation set
     * @param sourceTemplateSet Source Template Set
     * @param templateSetUpdateSpec Update Operations to arrive at the new Template Set
     * @return Updated Template Set (needs to be stored in backend explicitly later)
     */
    public TemplateSet updateTemplateSet(TemplateSet sourceTemplateSet, TemplateUpdateOperationSet templateSetUpdateSpec) {
        List<CompareTemplateVersioned> sourceTemplates = sourceTemplateSet.templates;
        Map<TemplateKey, SingleTemplateUpdateOperation> updates =   templateSetUpdateSpec.getTemplateUpdates();
        String newVersion = UUID.randomUUID().toString();
        List<CompareTemplateVersioned> updatedCompareTemplates = new ArrayList<>();
        sourceTemplates.forEach(template -> {
                TemplateKey key = new TemplateKey(Optional.of(sourceTemplateSet.version),
                    sourceTemplateSet.customer, sourceTemplateSet.app, template.service, template.requestPath, template.type);
                // for each existing template (identified by a template key) ,
                // check if any updates are specified in the update set ,
                // create a new template with new rules , and add it to the set of new templates ,
                // note that we are only performing updates here, for any new service / api
                // path we won't be adding any new templates to the template set
                // (that might be taken care of in forward testing)
                updatedCompareTemplates.add(updateTemplate(template, Optional.ofNullable(updates.get(key))));
            }
        );
        return new TemplateSet(newVersion, sourceTemplateSet.customer
            , sourceTemplateSet.app, Instant.now(), updatedCompareTemplates);
    }

    /**
     * Update a template given update operations
     * @param sourceTemplate The existing template
     * @param updateOpt The update operations for the template
     * @return The updated template
     */
    private CompareTemplateVersioned updateTemplate(CompareTemplateVersioned sourceTemplate
        , Optional<SingleTemplateUpdateOperation> updateOpt) {

        return updateOpt.map(update -> {
            Collection<TemplateEntryOperation> atomicUpdateOperations = update.getOperationList();
            Collection<TemplateEntry> originalRules = sourceTemplate.getRules();
            // this will the final set of rules for the new template
            Map<String, TemplateEntry> pathVsEntry = new HashMap<>();
            originalRules.forEach(templateEntry -> {
                pathVsEntry.put(templateEntry.getPath(), templateEntry);
            });
            atomicUpdateOperations.forEach(updateOperation -> {
                String path = updateOperation.getPath();
                OperationType operationType = updateOperation.getType();
                if (operationType.equals(OperationType.REMOVE)) {
                    // remove the rule on a delete operation
                    pathVsEntry.remove(path);
                } else if (operationType.equals(OperationType.ADD) || operationType.equals(OperationType.REPLACE)) {
                    if (updateOperation.getNewRule().isEmpty()) {
                        LOGGER.error("New Rule Not Available for Add/Replace Operation for Path :: " + path);
                    } else {
                        // for add or replace ... for the given json path .. add the new rule
                        // (in case of replace the entire rule will be replaced with the new rule)
                        pathVsEntry.put(path, updateOperation.getNewRule().get());
                    }
                }
            });
            return new CompareTemplateVersioned(sourceTemplate, pathVsEntry.values());
        }).orElse(new CompareTemplateVersioned(sourceTemplate));
    }
}
