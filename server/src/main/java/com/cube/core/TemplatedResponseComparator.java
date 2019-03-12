/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.core;

import com.cube.dao.RRBase;
import com.cube.dao.Response;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-03-06
 * @author Prasad M D
 */
public class TemplatedResponseComparator implements ResponseComparator {

    static TemplateEntry EQUALITYRULE = new TemplateEntry("/body", CompareTemplate.DataType.Str,
        CompareTemplate.PresenceType.Required, CompareTemplate.ComparisonType.Equal);
    public static CompareTemplate EQUALITYTEMPLATE = new CompareTemplate();
    static {
        EQUALITYTEMPLATE.addRule(EQUALITYRULE);
    };

    /**
     *
     * @param template
     * @param jsonmapper
     */
    public TemplatedResponseComparator(CompareTemplate template, ObjectMapper jsonmapper) {
        this.template = template;

        // the fields below are computed and stored for efficiency purposes, so that we don't
        // have to redo the work on each response compare
        bodytemplate = template.subsetWithPrefix(RRBase.BODYPATH);
        hdrFieldTemplate = template.subsetWithPrefix(RRBase.HDRPATH);
        metaFieldtemplate = template.subsetWithPrefix(RRBase.METAPATH);
        bodyComparator = new JsonComparator(bodytemplate, jsonmapper);
    }

    @Override
    public Comparator.Match compare(Response lhs, Response rhs) {
        return lhs.compare(rhs, template, metaFieldtemplate, hdrFieldTemplate, bodyComparator);
    }

    private final CompareTemplate template;
    private final CompareTemplate bodytemplate; // to be used for the body field
    private final CompareTemplate hdrFieldTemplate;
    private final CompareTemplate metaFieldtemplate;
    private final Comparator bodyComparator;

}
