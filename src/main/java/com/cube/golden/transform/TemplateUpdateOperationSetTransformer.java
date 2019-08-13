package com.cube.golden.transform;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.cube.cache.TemplateKey;
import com.cube.golden.SingleTemplateUpdateOperation;
import com.cube.golden.TemplateEntryOperation;
import com.cube.golden.TemplateUpdateOperationSet;

public class TemplateUpdateOperationSetTransformer {

    /**
     * Merge update operations for a single template
     * @param sourceUpdates existing update operations
     * @param newUpdates new update operations
     * @return Merged update operations
     */
    private SingleTemplateUpdateOperation mergeTemplateUpdate(SingleTemplateUpdateOperation sourceUpdates,
                                                              SingleTemplateUpdateOperation newUpdates) {
        // first create a fresh copy of all existing rules
        Map<String, TemplateEntryOperation> sourceUpdateMap = sourceUpdates.getOperationList().stream()
            .collect(Collectors.toMap(TemplateEntryOperation::getPath, operation -> operation));
        // for each existing json path in the update set, if new rule exists , just overwrite the existing rule
        // add rules for new paths as well to the update set (which earlier did not exist in the update set)
        newUpdates.getOperationList().forEach(newUpdate -> {
            sourceUpdateMap.put(newUpdate.getPath(), newUpdate);
        });
        return new SingleTemplateUpdateOperation(sourceUpdateMap.values());
    }

    /**
     * Add operations to an already existing template update operation set
     * @param sourceOperationSet Existing template update operation set
     * @param updates The updates to be added to the operations set
     * @return Merged Template Update Operation Set
     */
    public TemplateUpdateOperationSet updateTemplateOperationSet(TemplateUpdateOperationSet sourceOperationSet,
                                                                 Map<TemplateKey, SingleTemplateUpdateOperation> updates) {
        Map<TemplateKey, SingleTemplateUpdateOperation> existingOperations = Optional.of(sourceOperationSet
            .getTemplateUpdates()).orElse(new HashMap<>());
        updates.forEach((key, update) -> {
            if (existingOperations.containsKey(key)) {
                // merge update rules for templates, for which already some updates are specified
                SingleTemplateUpdateOperation existing = existingOperations.get(key);
                SingleTemplateUpdateOperation merged = mergeTemplateUpdate(existing, update);
                existingOperations.put(key, merged);
            } else {
                // add update rules for new templates (which were not present yet in the update set), as it is
                existingOperations.put(key, update);
            }
        });
        return new TemplateUpdateOperationSet(sourceOperationSet.getTemplateUpdateOperationSetId() , existingOperations);
    }


}
