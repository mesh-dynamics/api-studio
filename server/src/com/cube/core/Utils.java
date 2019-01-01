/**
 * Copyright Cube I O
 */
package com.cube.core;

import java.util.EnumSet;
import java.util.Optional;

/**
 * @author prasad
 *
 */
public class Utils {

	public static <T extends Enum<T>> Optional<T> valueOf(Class<T> clazz, String name) {
	    return EnumSet.allOf(clazz).stream().filter(v -> v.name().equals(name))
	                    .findAny();
	}
}
