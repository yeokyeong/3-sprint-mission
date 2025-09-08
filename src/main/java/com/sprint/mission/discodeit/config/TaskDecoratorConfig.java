package com.sprint.mission.discodeit.config;

import com.sprint.mission.discodeit.utils.ContextAwareTaskDecorator;
import com.sprint.mission.discodeit.utils.LoggingTaskDecorator;
import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.CompositeTaskDecorator;

@Configuration
public class TaskDecoratorConfig {

    @Bean
    public TaskDecorator contextAwareTaskDecorator() {
        return new ContextAwareTaskDecorator();
    }

    @Bean
    public TaskDecorator loggingTaskDecorator() {
        return new LoggingTaskDecorator();
    }

    @Primary
    @Bean
    public TaskDecorator compositeTaskDecorator(TaskDecorator contextAwareTaskDecorator,
        TaskDecorator loggingTaskDecorator) {
        return new CompositeTaskDecorator(
            Arrays.asList(contextAwareTaskDecorator, loggingTaskDecorator)
        );
    }


}
