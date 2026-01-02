package com.sprint.mission.discodeit.Event;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/// *분산환경의 notification 서비스라고 가정*/
@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationRequiredTopicListener {

    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;
    private final MessageRepository messageRepository;
    private final ReadStatusRepository readStatusRepository;
    private final UserRepository userRepository;


    @KafkaListener(topics = "discodeit.MessageCreatedEvent")
    public void onMessageCreatedEvent(String kafkaEvent) {
        try {
            MessageCreatedEvent event = objectMapper.readValue(kafkaEvent,
                    MessageCreatedEvent.class);
            /* 생성된 메세지 가져오기 -> notification 서비스가 message repository를 가지고있을수없는디? */
            Message createdMsg = messageRepository.findByIdWithAuthorAndChannel(event.messageId()).orElseThrow(() -> {
                log.error(
                        "[NotificationRequiredTopicListener] MessageCreatedEvent 리스너 에러 - message 없음");
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

            List<Notification> notifications = readStatusesWithoutAuthor.stream()
                    .map(readStatus -> {

                        String title =
                                createdMsg.getAuthor().getUsername() + "(#" + createdMsg.getChannel()
                                        .getName()
                                        + ")";
                        Notification notification = Notification.builder()
                                .receiverId(readStatus.getUser().getId())
                                //Q. 이걸 api에서 처리하는게 맞을까? or 프론트
                                .title(title)
                                //Q. 이걸 api에서 처리하는게 맞을까? or 프론트
                                .content(createdMsg.getContent())
                                .build();

                        log.info(
                                "[NotificationRequiredTopicListener] MessageCreatedEvent 리스너 - notification 생성: {}",
                                title);

                        return notification;
                    }).toList();

            System.out.println("notifications.size() = " + notifications.size());
            for (Notification noti : notifications) {
                System.out.println("noti = " + noti);
            }

            notificationRepository.saveAllAndFlush(notifications);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "discodeit.RoleUpdatedEvent")
    public void onRoleUpdatedEvent(String kafkaEvent) {
        try {
            RoleUpdatedEvent event = objectMapper.readValue(kafkaEvent,
                    RoleUpdatedEvent.class);

            String content = "[" + event.oldRole() + "]에서 [" + event.newRole() + "]로 변경";
            Notification notification = Notification.builder()
                    .receiverId(event.userId())
                    //Q. 이걸 api에서 처리하는게 맞을까? or 프론트
                    .title("권한이 변경되었습니다.")
                    //Q. 이걸 api에서 처리하는게 맞을까? or 프론트
                    .content(content)
                    .build();
            log.info(
                    "[NotificationRequiredTopicListener] RoleUpdatedEvent 리스너 - notification 생성 :{}",
                    content);

            notificationRepository.saveAndFlush(notification);

            log.info("[NotificationRequiredTopicListener] RoleUpdatedEvent 리스너 작업 완료");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "discodeit.BinaryContentCreateFailEvent")
    public void onBinaryContentCreateFailEvent(String kafkaEvent) {
        try {
            BinaryContentCreateFailEvent event = objectMapper.readValue(kafkaEvent,
                    BinaryContentCreateFailEvent.class);

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
                                        "[NotificationRequiredTopicListener] BinaryContentCreateFailEvent 리스너 - notification 생성 :{}",
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

            log.info("[NotificationRequiredTopicListener] BinaryContentCreateFailEvent 리스너 작업 완료");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
