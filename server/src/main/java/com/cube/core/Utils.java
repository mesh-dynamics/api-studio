/**
 * Copyright Cube I O
 */
package com.cube.core;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * @author prasad
 *
 */
public class Utils {

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

	public static <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
		CompletableFuture<Void> allDoneFuture =
				CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
		return allDoneFuture.thenApply(v ->
				futures.stream().
						map(future -> future.join()).
						collect(Collectors.<T>toList())
		);
	}


    public static IntNode intToJson(Integer val) {
		return IntNode.valueOf(val);
    }

	public static TextNode strToJson(String val) {
		return TextNode.valueOf(val);
	}

}
