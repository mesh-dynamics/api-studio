/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.core;

import com.cube.utils.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;


/*
 * Created by IntelliJ IDEA.
 * Date: 2019-03-14
 * @author Prasad M D
 */
public class TemplatedRRComparator {


    public static CompareTemplate EQUALITYTEMPLATE = new CompareTemplate();
    static TemplateEntry EQUALITYRULE = new TemplateEntry(Constants.BODY_PATH, CompareTemplate.DataType.Str,
        CompareTemplate.PresenceType.Required, CompareTemplate.ComparisonType.Equal);
    protected final CompareTemplate template;
    protected final CompareTemplate bodytemplate; // to be used for the body field
    protected final CompareTemplate hdrFieldTemplate;
    protected final CompareTemplate metaFieldtemplate;
    protected final Comparator bodyComparator;
    protected final Comparator fullComparator;

    public TemplatedRRComparator(CompareTemplate template, ObjectMapper jsonMapper) {
        this.template = template;


        // the fields below are computed and stored for efficiency purposes, so that we don't
        // have to redo the work on each response compare
        bodytemplate = template.subsetWithPrefix(Constants.BODY_PATH);
        // TODO: Event Redesign: hdrFieldTemplate and metaFieldTemplate can be removed
        hdrFieldTemplate = template.subsetWithPrefix(Constants.HDR_PATH);
        metaFieldtemplate = template.subsetWithPrefix(Constants.META_PATH);
        bodyComparator = new JsonComparator(bodytemplate, jsonMapper);
        fullComparator = new JsonComparator(template, jsonMapper);
    }


}
