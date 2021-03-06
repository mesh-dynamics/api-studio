/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.md.utils;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Taken from https://stackoverflow.com/questions/19757300/java-8-lambda-streams-filter-by-method-with-exception
 *
 */
public final class UtilException {

	@FunctionalInterface
	public interface Consumer_WithExceptions<T, E extends Exception> {
		void accept(T t) throws E;
	}

	@FunctionalInterface
	public interface BiConsumer_WithExceptions<T, U, E extends Exception> {
		void accept(T t, U u) throws E;
	}

	@FunctionalInterface
	public interface Function_WithExceptions<T, R, E extends Exception> {
		R apply(T t) throws E;
	}

	@FunctionalInterface
	public interface Supplier_WithExceptions<T, E extends Exception> {
		T get() throws E;
	}

	@FunctionalInterface
	public interface Runnable_WithExceptions<E extends Exception> {
		void run() throws E;
	}

	/**
	 * .forEach(rethrowConsumer(name -> System.out.println(Class.forName(name))));
	 * or .forEach(rethrowConsumer(ClassNameUtil::println));
	 */
	public static <T, E extends Exception> Consumer<T> rethrowConsumer(Consumer_WithExceptions<T, E> consumer)
		throws E {
		return t -> {
			try {
				consumer.accept(t);
			} catch (Exception exception) {
				throwAsUnchecked(exception);
			}
		};
	}

	public static <T, U, E extends Exception> BiConsumer<T, U> rethrowBiConsumer(
		BiConsumer_WithExceptions<T, U, E> biConsumer) throws E {
		return (t, u) -> {
			try {
				biConsumer.accept(t, u);
			} catch (Exception exception) {
				throwAsUnchecked(exception);
			}
		};
	}

	/**
	 * .map(rethrowFunction(name -> Class.forName(name))) or
	 * .map(rethrowFunction(Class::forName))
	 */
	public static <T, R, E extends Exception> Function<T, R> rethrowFunction(Function_WithExceptions<T, R, E> function)
		throws E {
		return t -> {
			try {
				return function.apply(t);
			} catch (Exception exception) {
				throwAsUnchecked(exception);
				return null;
			}
		};
	}

	/**
	 * rethrowSupplier(() -> new StringJoiner(new String(new byte[]{77, 97, 114,
	 * 107}, "UTF-8"))),
	 */
	public static <T, E extends Exception> Supplier<T> rethrowSupplier(Supplier_WithExceptions<T, E> function)
		throws E {
		return () -> {
			try {
				return function.get();
			} catch (Exception exception) {
				throwAsUnchecked(exception);
				return null;
			}
		};
	}

	/** uncheck(() -> Class.forName("xxx")); */
	public static void uncheck(Runnable_WithExceptions<?> t) {
		try {
			t.run();
		} catch (Exception exception) {
			throwAsUnchecked(exception);
		}
	}

	/** uncheck(() -> Class.forName("xxx")); */
	public static <R, E extends Exception> R uncheck(Supplier_WithExceptions<R, E> supplier) {
		try {
			return supplier.get();
		} catch (Exception exception) {
			throwAsUnchecked(exception);
			return null;
		}
	}

	/** uncheck(Class::forName, "xxx"); */
	public static <T, R, E extends Exception> R uncheck(Function_WithExceptions<T, R, E> function, T t) {
		try {
			return function.apply(t);
		} catch (Exception exception) {
			throwAsUnchecked(exception);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static <E extends Throwable> void throwAsUnchecked(Exception exception) throws E {
		throw (E) exception;
	}

	@SuppressWarnings("unchecked")
	public static <E extends Throwable> void throwAsUnchecked(Throwable exception) throws E {
		throw (E) exception;
	}

	public static String extractFirstStackTraceLocation(StackTraceElement[] stackTraces) {
		return (stackTraces.length > 0) ? (stackTraces[0]).getClassName()
			+ " " + (stackTraces[0]).getLineNumber() : "";
	}

}
