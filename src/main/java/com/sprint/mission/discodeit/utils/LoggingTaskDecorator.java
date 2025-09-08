package com.sprint.mission.discodeit.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskDecorator;

@Slf4j
public class LoggingTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        return () -> {
            long start = System.currentTimeMillis();
            try {
                log.debug("Async task started: {}", Thread.currentThread().getName());
                runnable.run();
            } catch (Exception e) {
                log.error("Exception in async task", e);
                throw e;
            } finally {
                long end = System.currentTimeMillis();
                log.debug("Async task finished: {} ({} ms)",
                    Thread.currentThread().getName(),
                    (end - start));
            }
        };
    }
}