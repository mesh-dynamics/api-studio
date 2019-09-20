/**
 * Copyright Cube I O
 */
package com.cube.core;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

import com.cube.agent.FnReqResponse;
import com.cube.cache.RequestComparatorCache;
import com.cube.cache.ResponseComparatorCache;
import com.cube.cache.TemplateKey;
import com.cube.golden.TemplateSet;


/**
 * @author prasad
 *
 */
public class Utils {

    private static final Logger LOGGER = LogManager.getLogger(Utils.class);

	public static <T extends Enum<T>> Optional<T> valueOf(Class<T> clazz, String name) {
	    return EnumSet.allOf(clazz).stream().filter(v -> v.name().equals(name))
	                    .findAny();
	}

	// copied from jdk.internal.net.http.common.Utils, since it is private there and we
	// need this list
	// TODO: Always keep this in sync
    private static final Set<String> DISALLOWED_HEADERS_SET;

    static {
        // A case insensitive TreeSet of strings.
        TreeSet<String> treeSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        treeSet.addAll(Set.of("connection", "content-length",
                "date", "expect", "from", "host", "origin",
                "referer", "upgrade",
                "via", "warning"));
        DISALLOWED_HEADERS_SET = Collections.unmodifiableSet(treeSet);
    }

    public static final Predicate<String>
            ALLOWED_HEADERS = (header) -> !DISALLOWED_HEADERS_SET.contains(header);

	/**
	 * @param s
	 * @return
	 */
	public static Optional<Integer> strToInt(String s) {
		try {
			return Optional.ofNullable(Integer.valueOf(s));
		} catch (Exception e) {
			return Optional.empty();
		}
	}


	public static Optional<Double> strToDouble(String s) {
		try {
			return Optional.ofNullable(Double.valueOf(s));
		} catch (Exception e) {
			return Optional.empty();
		}
	}


    public static Optional<Long> strToLong(String s) {
        try {
            return Optional.ofNullable(Long.valueOf(s));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static Optional<Boolean> strToBool(String b) {
        try {
            return Optional.of(Boolean.valueOf(b));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

	public static <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
		CompletableFuture<Void> allDoneFuture =
				CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
		return allDoneFuture.thenApply(v ->
				futures.stream().
						map(future -> future.join()).
						collect(Collectors.<T>toList())
		);
	}

	public static ValidateCompareTemplate validateTemplateSet(TemplateSet templateSet) {
        for (CompareTemplateVersioned template: templateSet.templates) {
            ValidateCompareTemplate validateCompareTemplate = template.validate();
            if(!validateCompareTemplate.isValid()) {
                return validateCompareTemplate;
            }
        }
        return new ValidateCompareTemplate(true, Optional.of(""));
    }


    public static IntNode intToJson(Integer val) {
		return IntNode.valueOf(val);
    }

	public static TextNode strToJson(String val) {
		return TextNode.valueOf(val);
	}

    public static Pattern analysisTimestampPattern = Pattern.compile("\\\\\"timestamp\\\\\":\\d{13},");
	public static Pattern recordingTimestampPattern = Pattern.compile(",\"timestamp_dt\":\\{\"name\":\"timestamp_dt\",\"value\":\".+\"\\}");

    public static Pattern replayMetaIdPattern = Pattern.compile("\"id\":\\{\"name\":\"id\",\"value\":\"(.+?)\"},");
    public static Pattern replayIdPattern = Pattern.compile("\"replayid_s\":\\{\"name\":\"replayid_s\",\"value\":\"(.+?)\"},");
    public static Pattern timestampIdPattern = Pattern.compile(",\"creationtimestamp_s\":\\{\"name\":\"creationtimestamp_s\",\"value\":\"(.+?)\"}");
    public static Pattern versionPattern = Pattern.compile(",\"version_s\":\\{\"name\":\"version_s\"}");

    public static String removePatternFromString(String val, Pattern pattern) {
	    Matcher matcher = pattern.matcher(val);
	    return matcher.replaceAll("");
    }

    public static void preProcess(FnReqResponse fnReqResponse) {
	    try {
            if (fnReqResponse.name.equals("add")
                && fnReqResponse.argVals.length > 0) {
                if (fnReqResponse.argVals[0].contains("\"type_s\":{\"name\":\"type_s\",\"value\":\"Analysis\"}")) {
                    String newVal = removePatternFromString(fnReqResponse.argVals[0], analysisTimestampPattern);
                    fnReqResponse.argVals[0] = newVal;
                    fnReqResponse.argsHash[0] = newVal.hashCode();
                } else if (fnReqResponse.argVals[0].contains("{\"type_s\":{\"name\":\"type_s\",\"value\":\"Recording\"}")) {
                    String newVal = removePatternFromString(fnReqResponse.argVals[0], recordingTimestampPattern);
                    fnReqResponse.argVals[0] = newVal;
                    fnReqResponse.argsHash[0] = newVal.hashCode();
                } else if (fnReqResponse.argVals[0].startsWith("{\"id\":{\"name\":\"id\",\"value\":\"ReplayMeta-")) {
                    String newVal = removePatternFromString(fnReqResponse.argVals[0], replayMetaIdPattern);
                    newVal = removePatternFromString(newVal, replayIdPattern);
                    newVal = removePatternFromString(newVal, timestampIdPattern);
                    newVal = removePatternFromString(newVal, versionPattern);
                    fnReqResponse.argVals[0] = newVal;
                    fnReqResponse.argsHash[0] = newVal.hashCode();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error while preprocessing fn req resp object :: " + e.getMessage());
        }
    }

    static public TemplateSet templateRegistriesToTemplateSet(TemplateRegistries registries,
                                                              String customerId, String appId,
                                                              Optional<String> templateVersion) {
        List<TemplateRegistry> templateRegistries = registries.getTemplateRegistryList();

        List<CompareTemplateVersioned> compareTemplateVersionedList =
            templateRegistries
                .stream()
                .map(registry -> new CompareTemplateVersioned(Optional.of(registry.getService()),
            Optional.of(registry.getPath()), registry.getType(), registry.getTemplate()))
                .collect(Collectors.toList());

        // pass null for version if version is empty and timestamp so that new version number is created automatically
        TemplateSet templateSet = new TemplateSet(templateVersion.orElse(null), customerId, appId, null,
            compareTemplateVersionedList);

        return templateSet;

    }

    static public void invalidateCacheFromTemplateSet(TemplateSet templateSet,
                                                 RequestComparatorCache requestComparatorCache,
                                                 ResponseComparatorCache responseComparatorCache)
    {
        templateSet.templates.stream().forEach(compareTemplateVersioned -> {
            TemplateKey key =
                new TemplateKey(Optional.of(templateSet.version), templateSet.customer, templateSet.app,
                    compareTemplateVersioned.service,
                    compareTemplateVersioned.prefixpath, compareTemplateVersioned.type);
            requestComparatorCache.invalidateKey(key);
            responseComparatorCache.invalidateKey(key);
        });
    }

    static Pattern templateKeyPattern = Pattern.compile("TemplateKey\\{customerId=(.+?), appId=(.+?), serviceId=(.+?), path=(.+?), version=(.+?), type=(.+?)}");

    static public Optional<TemplateKey> templateKeyFromSerializedString(String serialized) {
        Optional<TemplateKey> toReturn = Optional.empty();
        Matcher m = templateKeyPattern.matcher(serialized);
        TemplateKey templateKey = null;
        if (m.matches()) {
            String customerId = m.group(1);
            String appId = m.group(2);
            String service = m.group(3);
            String path = m.group(4);
            String version = m.group(5);
            String type = m.group(6);
            //System.out.println(customerId + " " + appId + " " + service + " " + path + " " + version + " " + type);
            templateKey = new TemplateKey(Optional.of(version) , customerId, appId, service, path, ("Request".equalsIgnoreCase(type) ?
                TemplateKey.Type.Request : TemplateKey.Type.Response));
            toReturn = Optional.of(templateKey);
        } else {
            LOGGER.error("Unable to deserialize template key from string :: " + templateKey);
        }
        return toReturn;
    }

    /**
     * https://stackoverflow.com/questions/7498030/append-relative-url-to-java-net-url
     * @param baseUrl Base Url
     * @param suffix Relative path to append to the base url
     * @return Concatenated Normalized Path (// are converted to /)
     * @throws Exception Exception if Any
     */
    static public String appendUrlPath(String baseUrl, String suffix) throws Exception {
        URIBuilder uriBuilder = new URIBuilder(baseUrl);
        return uriBuilder.setPath(uriBuilder.getPath() + "/" + suffix)
            .build().normalize().toString();
    }

}
