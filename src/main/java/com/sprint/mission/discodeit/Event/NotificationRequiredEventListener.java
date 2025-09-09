package com.sprint.mission.discodeit.Event;

import com.sprint.mission.discodeit.dto.event.BinaryContentCreateFailEvent;
import com.sprint.mission.discodeit.dto.event.MessageCreatedEvent;
import com.sprint.mission.discodeit.dto.event.RoleUpdatedEvent;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.Notification;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.NotificationRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.scheduling.annotation.Async;
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
    private final UserRepository userRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 트랜잭션 새로 열기
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // 기본값
    @CacheEvict(value = "notifications:byUserId", allEntries = true)
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

        notificationRepository.saveAllAndFlush(notifications);
        log.info("[NotificationRequiredEventListener] MessageCreatedEvent 리스너 작업 완료");

    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // 기본값
    @CachePut(value = "notifications:byUserId", key = "#event.userId()")
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

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // 기본값
    @CacheEvict(value = "notifications:byUserId", allEntries = true)
    public void on(BinaryContentCreateFailEvent event) {
        log.info("[NotificationRequiredEventListener] BinaryContentCreateFailEvent 리스너 시작");
        String requestId = MDC.get("request-id");
        System.out.println("requestId = " + requestId);

        String title = "S3 파일 업로드 실패";
        String content = """
               RequestId: %s
                BinaryContentId: %s
                Error: %s
            """.formatted(
            requestId,
            event.binaryContentId(),
            event.cause().getMessage()
        );

        /* # 알림 내용 예시
        RequestId: 7641467e369e458a98033558a83321fb
        BinaryContentId: b0549c2a-014c-4761-8b21-4b77d3bd011c
        Error: The AWS Access Key Id you provided does not exist in our records.
         (Service: S3,
         Status Code: 403,
         Request ID: B7KCVSRCGPYJZREX,
         Extended Request ID: AWRVuJJJ3upwwOkCnd+yhHkgSajUxdg7L4195lbMVTIka6WnBpjZLLRTReoHbgIMf9zzH/QQM0Y5ZOVJCHF2F+l2mSyPG/+8Ee2XBS8hcqk=)
         (SDK Attempt Count: 1)
         */

        // 모든 관리자 유저 정보 가져옴
        List<Notification> notifications = userRepository.findAllByRole(Role.ADMIN).stream()
            .map(user -> {
                    log.info(
                        "[NotificationRequiredEventListener] BinaryContentCreateFailEvent 리스너 - notification 생성 :{}",
                        content);

                    return Notification.builder()
                        .receiverId(user.getId())
                        .title(title)
                        .content(content)
                        .build();
                }
            ).toList();

        System.out.println("notifications.size() = " + notifications.size());

        notificationRepository.saveAllAndFlush(notifications);

        log.info("[NotificationRequiredEventListener] BinaryContentCreateFailEvent 리스너 작업 완료");

    }


}
