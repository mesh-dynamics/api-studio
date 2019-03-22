/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.core;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonPointer;

import static com.cube.core.Comparator.Resolution.*;
import static com.cube.core.CompareTemplate.DataType.Default;

public class TemplateEntry {

    private static final Logger LOGGER = LogManager.getLogger(TemplateEntry.class);

    /**
     * @param path
     * @param dt
     * @param pt
     * @param ct
     * @param customization
     */
    // Adding appropriate annotations for json serialization/deserialization
    @JsonCreator
    public TemplateEntry(@JsonProperty("path") String path, @JsonProperty("dt") CompareTemplate.DataType dt,
                         @JsonProperty("pt") CompareTemplate.PresenceType pt,
                         @JsonProperty("ct") CompareTemplate.ComparisonType ct,
                         @JsonProperty("customization") Optional<String> customization) {
        super();
        this.path = path;
        this.dt = dt;
        this.pt = pt;
        this.ct = ct;
        this.customization = customization;
        this.pathptr = JsonPointer.valueOf(path);
        if (ct == CompareTemplate.ComparisonType.CustomRegex) {
            // default pattern is to match everything
            regex = Optional.ofNullable(Pattern.compile(customization.orElse(".*")));
        } else {
            regex = Optional.empty();
        }
    }

    /**
     * @param path
     * @param dt
     * @param pt
     * @param ct
     */
    public TemplateEntry(String path, CompareTemplate.DataType dt, CompareTemplate.PresenceType pt, CompareTemplate.ComparisonType ct) {
        this(path, dt, pt, ct, Optional.empty());
    }

    @JsonProperty("path")
    String path;
    @JsonProperty("dt")
    CompareTemplate.DataType dt;
    @JsonProperty("pt")
    CompareTemplate.PresenceType pt;
    @JsonProperty("ct")
    CompareTemplate.ComparisonType ct;
    @JsonProperty("customization")
    Optional<String> customization; // metadata for fuzzy match. For e.g. this could be the regex
    JsonPointer pathptr; // compiled form of path
    Optional<Pattern> regex; // compiled form of regex if ct == CustomRegex

    /*
     * Assuming compare type is not ignore or default
     */
    private Comparator.Resolution rhsmissing() {
        switch (pt) {
            case Required:
                return ERR_Required;
            case Optional:
                return OK_Optional;
            case Default:
                return OK;
        }
        return OK;
    }

    /*
     * Assuming rhs is present
     * Assuming compare type is not ignore or default
     */
    Comparator.Resolution lhsmissing() {
        switch (ct) {
            case Ignore:
                return OK_Ignore;
            case Default:
                return OK;
            default:
                return OK_OtherValInvalid;
        }
        // return OK_OtherValInvalid;
    }


    Comparator.Resolution checkMatchStr(Optional<String> lhs, Optional<String> rhs) {
        Comparator.Resolution resolution = checkTypeAndPresence(CompareTemplate.DataType.Str, rhs);
        if (resolution.isErr()) {
            return resolution;
        }

        switch (ct) {
            case CustomRegex:
                Pattern pattern = regex.orElseGet(() -> {
                    LOGGER.error("Internal logical error - compiled pattern missing for regex");
                    return Pattern.compile(customization.orElse(".*"));
                });
                return rhs.map(rval -> {
                    Matcher matcher = pattern.matcher(rval);
                    if (!matcher.matches()) {
                        return Comparator.Resolution.ERR_ValFormatMismatch;
                    }
                    return lhs.map(lval -> {
                        Matcher lhsmatcher = pattern.matcher(lval);
                        if (!lhsmatcher.matches()) {
                            return Comparator.Resolution.OK_OtherValInvalid;
                        }
                        if (matcher.groupCount() != lhsmatcher.groupCount()) {
                            return Comparator.Resolution.ERR_ValMismatch;
                        }
                        for (int i = 0; i < matcher.groupCount(); ++i) {
                            if (!matcher.group(i).equals(lhsmatcher.group(i))) {
                                return Comparator.Resolution.ERR_ValMismatch;
                            }
                        }
                        return Comparator.Resolution.OK_CustomMatch;
                    }).orElse(lhsmissing());
                }).orElse(rhsmissing());
            case Equal:
            case EqualOptional:
                return checkEqual(lhs, rhs, ct == CompareTemplate.ComparisonType.EqualOptional);
            case Ignore:
                return OK_Ignore;
            case Default:
                return OK;
            default:
                return ERR_ValTypeMismatch; // could be CustomRound, Floor, Ceil

        }

    }

    void checkMatchStr(Optional<String> lhs, Optional<String> rhs, Comparator.Match match,
                       boolean needDiff, String prefixpath) {

        Comparator.Resolution resolution = checkMatchStr(lhs, rhs);
        match.mergeStr(resolution, path, needDiff, prefixpath, rhs, lhs);
    }

    public void checkMatchStr(Optional<String> lhs, Optional<String> rhs, Comparator.Match match,
                              boolean needDiff) {

        checkMatchStr(lhs, rhs, match, needDiff, "");
    }


    public void checkMatchStr(String lhs, String rhs, Comparator.Match match, boolean needDiff) {
        checkMatchStr(Optional.ofNullable(lhs), Optional.ofNullable(rhs), match, needDiff, "");
    }

    Comparator.Resolution checkMatchInt(Optional<Integer> lhs, Optional<Integer> rhs) {
        Comparator.Resolution resolution = checkTypeAndPresence(CompareTemplate.DataType.Int, rhs);
        if (resolution.isErr()) {
            return resolution;
        }

        switch (ct) {
            case Equal:
            case EqualOptional:
                return checkEqual(lhs, rhs, ct == CompareTemplate.ComparisonType.EqualOptional);
            case Ignore:
                return OK_Ignore;
            case Default:
                return OK;
            default:
                return ERR_ValTypeMismatch; // could be CustomRound, Floor, Ceil, CustomReqex

        }
    }

    private void checkMatchInt(Optional<Integer> lhs, Optional<Integer> rhs, Comparator.Match match, boolean needDiff) {

        Comparator.Resolution resolution = checkMatchInt(lhs, rhs);
        match.mergeInt(resolution, path, needDiff, "", rhs, lhs);
    }

    public void checkMatchInt(int lhs, int rhs, Comparator.Match match, boolean needDiff) {
        checkMatchInt(Optional.ofNullable(lhs), Optional.ofNullable(rhs), match, needDiff);
    }

    private <T> Comparator.Resolution checkEqual(Optional<T> lhs, Optional<T> rhs, boolean isEqualOptional) {
        return rhs.map(rval -> {
            return lhs.map(lval -> {
                if (rval.equals(lval)) {
                    return OK;
                } else {
                    return isEqualOptional ? OK_OptionalMismatch : ERR_ValMismatch;
                }
            }).orElse(lhsmissing());
        }).orElse(rhsmissing());
    }

    private <T> Comparator.Resolution checkTypeAndPresence(CompareTemplate.DataType expectedType, Optional<T> val) {
        // check type
        if (dt != expectedType && dt != Default) {
            return Comparator.Resolution.ERR_ValTypeMismatch;
        }
        if (pt == CompareTemplate.PresenceType.Required && val.isEmpty()) {
            return ERR_Required;
        }
        return OK;
    }

    Comparator.Resolution checkMatchDbl(Optional<Double> lhs, Optional<Double> rhs) {
        Comparator.Resolution resolution = checkTypeAndPresence(CompareTemplate.DataType.Float, rhs);
        if (resolution.isErr()) {
            return resolution;
        }

        switch (ct) {
            case CustomFloor:
            case CustomCeil:
            case CustomRound:
                return rhs.map(rval -> {
                    return lhs.map(lval -> {
                        int numdecimal = customization.flatMap(Utils::strToInt).orElse(0);
                        double lval1 = adjustDblVal(ct, lval, numdecimal);
                        double rval1 = adjustDblVal(ct, rval, numdecimal);
                        return (lval1 == rval1) ? Comparator.Resolution.OK_CustomMatch : Comparator.Resolution.ERR_ValMismatch;
                    }).orElse(lhsmissing());
                }).orElse(rhsmissing());
            case Equal:
            case EqualOptional:
                return checkEqual(lhs, rhs, ct == CompareTemplate.ComparisonType.EqualOptional);
            case Ignore:
                return OK_Ignore;
            case Default:
                return OK;
            default:
                return ERR_ValTypeMismatch; // could be CustomRegex

        }

    }

    private static double adjustDblVal(CompareTemplate.ComparisonType ct, double val, int numdecimal) {
        double multiplier = Math.pow(10, numdecimal);
        switch(ct) {
            case CustomCeil:
                return Math.ceil(val * multiplier)/multiplier;
            case CustomFloor:
                return Math.floor(val * multiplier)/multiplier;
            case CustomRound:
                return Math.round(val * multiplier)/multiplier;
            default:
                return val;
        }
    }


}
