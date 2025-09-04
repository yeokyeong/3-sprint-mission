package com.sprint.mission.discodeit.Event;

import com.sprint.mission.discodeit.dto.event.MessageCreatedEvent;
import com.sprint.mission.discodeit.dto.event.RoleUpdatedEvent;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.Notification;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.NotificationRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRequiredEventListener {

    private final NotificationRepository notificationRepository;
    private final ReadStatusRepository readStatusRepository;
    private final MessageRepository messageRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW) // 트랜잭션 새로 열기
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // 기본값
    public void on(MessageCreatedEvent event) {
        log.info("[NotificationRequiredEventListener] MessageCreatedEvent 리스너 시작");
        /* 생성된 메세지 가져오기 */
        Message createdMsg = messageRepository.findById(event.messageId()).orElseThrow(() -> {
            log.error(
                "[NotificationRequiredEventListener] MessageCreatedEvent 리스너 에러 - message 없음");

            throw new RuntimeException();
        });

        /* 알림 켜놓은 사용자만 알림보내기 */
        List<ReadStatus> readStatuses = readStatusRepository.findAllByChannelIdAndNotificationEnabled(
            event.channelId(), true);
        System.out.println("readStatuses.size() = " + readStatuses.size());
        System.out.println("event.authorId() = " + event.authorId());
        List<ReadStatus> readStatusesWithoutAuthor = readStatuses.stream()
            .filter(readStatus -> {
                System.out.println(
                    "readStatus.getUser().getId() = " + readStatus.getUser().getId());
                return !readStatus.getUser().getId().equals(event.authorId());
            }).toList();
        System.out.println(
            "readStatusesWithoutAuthor.size() = " + readStatusesWithoutAuthor.size());

        List<Notification> notifications = readStatusesWithoutAuthor.stream().map(readStatus -> {

            String title =
                createdMsg.getAuthor().getUsername() + "(#" + createdMsg.getChannel().getName()
                    + ")";
            Notification notification = Notification.builder()
                .receiverId(readStatus.getUser().getId())
                //Q. 이걸 api에서 처리하는게 맞을까? or 프론트
                .title(title)
                //Q. 이걸 api에서 처리하는게 맞을까? or 프론트
                .content(createdMsg.getContent())
                .build();

            log.info(
                "[NotificationRequiredEventListener] MessageCreatedEvent 리스너 - notification 생성: {}",
                title);

            return notification;
        }).toList();

        System.out.println("notifications.size() = " + notifications.size());
        for (Notification noti : notifications) {
            System.out.println("noti = " + noti);
        }

//        에러!!!!! DB에 저장이 안됨
        notificationRepository.saveAllAndFlush(notifications);
        log.info("[NotificationRequiredEventListener] MessageCreatedEvent 리스너 작업 완료");

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // 기본값
    public void on(RoleUpdatedEvent event) {
        log.info("[NotificationRequiredEventListener] RoleUpdatedEvent 리스너 시작");

        String content = "[" + event.oldRole() + "]에서 [" + event.newRole() + "]로 변경";
        Notification notification = Notification.builder()
            .receiverId(event.userId())
            //Q. 이걸 api에서 처리하는게 맞을까? or 프론트
            .title("권한이 변경되었습니다.")
            //Q. 이걸 api에서 처리하는게 맞을까? or 프론트
            .content(content)
            .build();
        log.info(
            "[NotificationRequiredEventListener] RoleUpdatedEvent 리스너 - notification 생성 :{}",
            content);

        notificationRepository.saveAndFlush(notification);

        log.info("[NotificationRequiredEventListener] RoleUpdatedEvent 리스너 작업 완료");

    }

}
