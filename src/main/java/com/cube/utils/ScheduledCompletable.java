/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.utils;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import io.md.utils.Constants;

/*
 * Created by IntelliJ IDEA.
 * Date: 04/08/20
 * Taken from: https://www.artificialworlds.net/blog/2019/04/05/scheduling-a-task-in-java-within-a-completablefuture/
 */
public class ScheduledCompletable {

    private static final Logger LOGGER = LogManager.getLogger(ScheduledCompletable.class);

    public static <T> CompletableFuture<T> schedule(
        ScheduledExecutorService executor,
        Supplier<T> command,
        long delay,
        TimeUnit unit
    ) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        executor.schedule(
            (() -> {
                try {
                    return completableFuture.complete(command.get());
                } catch (Throwable t) {
                    LOGGER.error(new ObjectMessage(Map.of(Constants.ERROR, "Error in finishing scheduled task ", Constants.REASON, t.getMessage())) , t);
                    return completableFuture.completeExceptionally(t);
                }
            }),
            delay,
            unit
        );
        return completableFuture;
    }

    public static <T> CompletableFuture<T> scheduleAsync(
        ScheduledExecutorService executor,
        Supplier<CompletableFuture<T>> command,
        long delay,
        TimeUnit unit
    ) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        executor.schedule(
            (() -> {
                command.get().thenAccept(
                    t -> {completableFuture.complete(t);}
                )
                    .exceptionally(
                        t -> {completableFuture.completeExceptionally(t);return null;}
                    );
            }),
            delay,
            unit
        );
        return completableFuture;
    }
}
