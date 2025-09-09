package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.NotificationDto;
import com.sprint.mission.discodeit.entity.Notification;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BasicNotificationService implements NotificationService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final ReadStatusRepository readStatusRepository;
    private final NotificationMapper notificationMapper;
    @Lazy
    @Autowired
    private BasicNotificationService self; // 프록시 주입


    @Override
    public List<NotificationDto> findAll(Principal principal) {

        String userEmail = principal.getName();
        //FIXME : userService에 따로 빼기
        if (userEmail == null) {
            throw new RuntimeException();
        }
        //TODO: Exception 변경
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException());

        return self.findAllByUserId(user.getId()); // 자기 자신 말고 새로운 객체 호출

    }

    @Cacheable(value = "notifications:byUserId", key = "#userId")
    public List<NotificationDto> findAllByUserId(UUID userId) {
        log.info("[notificationService] findAllByUserId calling...");
        List<Notification> notifications = notificationRepository.findByReceiverId(userId);

        return notifications.stream().map(notificationMapper::toDto).toList();
    }

    @Transactional
    @Override
    @CacheEvict(value = "notifications:byUserId", allEntries = true)
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
