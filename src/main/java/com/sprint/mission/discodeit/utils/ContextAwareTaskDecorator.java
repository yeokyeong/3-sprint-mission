package com.sprint.mission.discodeit.utils;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class ContextAwareTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // 1. 현재 스레드의 컨텍스트 캡처
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        // 2. 향상된 Runnable 반환
        return () -> {
            try {
                // 3. 비동기 스레드에서 컨텍스트 복원
                SecurityContextHolder.setContext(securityContext);
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                }

                // 4.원본 작업 실행
                runnable.run();
            } finally {
                // 5. 실행 후 정리 작업
                SecurityContextHolder.clearContext();
                MDC.clear();
            }
        };
    }
}