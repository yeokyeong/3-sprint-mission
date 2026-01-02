package com.sprint.mission.discodeit.Event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.dto.event.BinaryContentCreateFailEvent;
import com.sprint.mission.discodeit.dto.event.MessageCreatedEvent;
import com.sprint.mission.discodeit.dto.event.RoleUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/// *분산환경이라고 가정하고 각 이벤트가 발생했을때 kafka로 이벤트 내용을 전송함*/
@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaProduceRequiredEventListener {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // 기본값
    @CacheEvict(value = "notifications:byUserId", allEntries = true)
    public void on(MessageCreatedEvent event) {
        log.info("[KafkaProduceRequiredEventListener] MessageCreatedEvent 리스너 시작");
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("discodeit.MessageCreatedEvent", payload);
        } catch (JsonProcessingException e) {
            log.error(
                    "[KafkaProduceRequiredEventListener] MessageCreatedEvent 리스너 에러 - json parse 실패");
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error(
                    "[KafkaProduceRequiredEventListener] MessageCreatedEvent 리스너 에러");
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // 기본값
    public void on(RoleUpdatedEvent event) {
        log.info("[KafkaProduceRequiredEventListener] RoleUpdatedEvent 리스너 시작");
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("discodeit.RoleUpdatedEvent", payload);
        } catch (JsonProcessingException e) {
            log.error(
                    "[KafkaProduceRequiredEventListener] RoleUpdatedEvent 리스너 에러 - json parse 실패");
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error(
                    "[KafkaProduceRequiredEventListener] RoleUpdatedEvent 리스너 에러");
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // 기본값
    public void on(BinaryContentCreateFailEvent event) {
        log.info("[KafkaProduceRequiredEventListener] BinaryContentCreateFailEvent 리스너 시작");
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("discodeit.BinaryContentCreateFailEvent", payload);
        } catch (JsonProcessingException e) {
            log.error(
                    "[KafkaProduceRequiredEventListener] BinaryContentCreateFailEvent 리스너 에러 - json parse 실패");
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error(
                    "[KafkaProduceRequiredEventListener] BinaryContentCreateFailEvent 리스너 에러");
        }
    }

}
