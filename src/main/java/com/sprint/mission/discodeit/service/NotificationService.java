package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.data.NotificationDto;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public interface NotificationService {

    List<NotificationDto> findAll(Principal principal);

    @Transactional
    void deleteByNotificationId(UUID notificationId, Principal principal);
}
