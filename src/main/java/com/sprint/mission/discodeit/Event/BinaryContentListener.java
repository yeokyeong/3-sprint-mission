package com.sprint.mission.discodeit.Event;

import com.sprint.mission.discodeit.dto.event.BinaryContentCreatedEvent;
import com.sprint.mission.discodeit.entity.BinaryContentStatus;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class BinaryContentListener {

    private final BinaryContentStorage binaryContentStorage;
    private final BinaryContentRepository binaryContentRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // 기본값
    public void on(BinaryContentCreatedEvent event) {
        log.info("[BinaryContentListener] BinaryContentCreatedEvent 리스너 시작");

        UUID binaryContentId = event.binaryContentId();
        BinaryContentStatus newBinaryContentStatus = binaryContentStorage.put(binaryContentId,
            event.bytes());
        // DB에 status값 업데이트
        binaryContentRepository.findById(binaryContentId)
            .ifPresent(binaryContent -> binaryContent.updateStatus(newBinaryContentStatus));

        log.info("[BinaryContentListener] BinaryContentCreatedEvent 리스너 작업 완료");

    }
}
