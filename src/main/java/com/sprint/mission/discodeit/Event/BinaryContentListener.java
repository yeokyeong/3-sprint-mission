package com.sprint.mission.discodeit.Event;

import com.sprint.mission.discodeit.dto.event.BinaryContentCreatedEvent;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BinaryContentListener {

    private final BinaryContentStorage binaryContentStorage;

    @EventListener
    public void HandleBinaryContentCreated(BinaryContentCreatedEvent event) {
        binaryContentStorage.put(event.binaryContentId(), event.bytes());
    }
}
