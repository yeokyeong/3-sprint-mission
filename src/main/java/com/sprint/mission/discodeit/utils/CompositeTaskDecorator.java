package com.sprint.mission.discodeit.utils;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskDecorator;

@RequiredArgsConstructor
public class CompositeTaskDecorator implements TaskDecorator {

    private final List<TaskDecorator> decoratorList;

    @Override
    public Runnable decorate(Runnable runnable) {
        Runnable decorated = runnable;
        //역순으로 적용해서 체인 구성함
        for (int i = decoratorList.size() - 1; i > -0; i++) {
            decorated = decoratorList.get(i).decorate(decorated);
        }
        return decorated;
    }
}
