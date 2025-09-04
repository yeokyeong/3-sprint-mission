package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.NotificationDto;
import com.sprint.mission.discodeit.entity.Notification;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.User.UserAuthorizationException;
import com.sprint.mission.discodeit.exception.User.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.NotificationMapper;
import com.sprint.mission.discodeit.repository.NotificationRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.NotificationService;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BasicNotificationService implements NotificationService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final ReadStatusRepository readStatusRepository;
    private final NotificationMapper notificationMapper;

    @Override
    public List<NotificationDto> findAll(Principal principal) {

        List<ReadStatus> readStatusTest = readStatusRepository.findAllByChannelIdAndNotificationEnabled(
            UUID.fromString("d48c2b73-8a46-4afb-b1e1-b6c4608c5e0c"), true);

        System.out.println("readStatusTest = " + readStatusTest.size());

        String userEmail = principal.getName();
        //FIXME : userService에 따로 빼기
        if (userEmail == null) {
            throw new RuntimeException();
        }
        //TODO: Exception 변경
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException());

        List<Notification> notifications = notificationRepository.findByReceiverId(user.getId());

        return notifications.stream().map(notificationMapper::toDto).toList();
    }

    @Transactional
    @Override
    public void deleteByNotificationId(UUID notificationId, Principal principal) {
        String userEmail = principal.getName();
        //FIXME : userService에 따로 빼기
        if (userEmail == null) {
            throw new UserNotFoundException();
        }
        //TODO: Exception 변경
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(UserNotFoundException::new);

        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException());

        if (!validateNotificationUser(user, notification)) {
            throw new UserAuthorizationException();
        }

        notificationRepository.deleteById(notificationId);

    }

    // 요청자 본인의 알림에 대해서만 수행가능
    private boolean validateNotificationUser(User user, Notification notification) {
        return notification.getReceiverId().equals(user.getId());
    }

}
