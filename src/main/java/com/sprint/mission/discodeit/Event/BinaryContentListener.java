package com.sprint.mission.discodeit.Event;

import com.sprint.mission.discodeit.dto.event.BinaryContentCreatedEvent;
import com.sprint.mission.discodeit.entity.BinaryContentStatus;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BinaryContentListener {

    private final BinaryContentStorage binaryContentStorage;
    private final BinaryContentRepository binaryContentRepository;

    @EventListener
    public void HandleBinaryContentCreated(BinaryContentCreatedEvent event) {
        UUID binaryContentId = event.binaryContentId();
        BinaryContentStatus newBinaryContentStatus = binaryContentStorage.put(binaryContentId,
            event.bytes());
        // DB에 status값 업데이트
        binaryContentRepository.findById(binaryContentId)
            .ifPresent(binaryContent -> binaryContent.updateStatus(newBinaryContentStatus));

    }
}
