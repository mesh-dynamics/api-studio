package com.cube.learning;

import com.cube.learning.TemplateEntryMeta.RequestOrResponse;
import com.cube.learning.TemplateEntryMeta.YesOrNo;
import io.md.core.Comparator.Resolution;
import io.md.dao.ReqRespMatchResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class CompareTemplatesLearner {

    HashMap<TemplateEntryMeta, TemplateEntryMeta> templateEntryToObjectMap = new HashMap<>();

    void addTemplateEntryMeta(RequestOrResponse requestOrResponse, String service,
        Resolution resolution, String apiPath, String jsonPath) {

        YesOrNo ignoreValue = YesOrNo.no;
        YesOrNo ignorePresence = YesOrNo.no;
        YesOrNo requiresReview = YesOrNo.no;

        switch (resolution){

            case OK_Ignore:
            case OK_DefaultCT:
                ignoreValue = YesOrNo.yes;
                break;

            case OK_Optional:
            case OK_DefaultPT:
            case OK_OtherValInvalid:
                ignorePresence = YesOrNo.yes;
                break;

            case ERR_ValMismatch:
                ignoreValue = YesOrNo.yes;
                requiresReview = YesOrNo.yes;
                break;

            case ERR_NewField:
            case ERR_Required:
            case ERR_RequiredGolden:
                ignorePresence = YesOrNo.yes;
                requiresReview = YesOrNo.yes;
                break;

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

        TemplateEntryMeta templateEntryMeta = new TemplateEntryMeta(requiresReview,
            requestOrResponse, service, apiPath, jsonPath, 0, ignoreValue, ignorePresence);

        templateEntryToObjectMap.computeIfAbsent(templateEntryMeta, k -> templateEntryMeta).count++;

    }

    public List<TemplateEntryMeta> learnCompareTemplates(
        Stream<ReqRespMatchResult> reqRespMatchResultStream) {

        reqRespMatchResultStream.forEach(res -> {

                res.reqCompareRes.diffs.forEach(
                    diff -> {
                        addTemplateEntryMeta(RequestOrResponse.Request, res.service, diff.resolution,
                            res.path, diff.path);;
                    });

                res.respCompareRes.diffs.forEach(
                    diff -> {
                        addTemplateEntryMeta(RequestOrResponse.Response, res.service, diff.resolution,
                            res.path, diff.path);
                    });

            }
        );

        List<TemplateEntryMeta> templateEntryMetaList = new ArrayList<>(
            templateEntryToObjectMap.keySet());
        Collections.sort(templateEntryMetaList);
        return templateEntryMetaList;
    }

}
