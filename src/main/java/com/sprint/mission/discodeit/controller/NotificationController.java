package com.sprint.mission.discodeit.controller;


import com.sprint.mission.discodeit.controller.api.NotificationApi;
import com.sprint.mission.discodeit.dto.data.NotificationDto;
import com.sprint.mission.discodeit.exception.User.UserAuthorizationException;
import com.sprint.mission.discodeit.exception.User.UserNotFoundException;
import com.sprint.mission.discodeit.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController implements NotificationApi {

    private final NotificationService notificationService;

    /* 특정 사용자가 읽지 않은 모든 알림 조회 */
    @GetMapping
    @Override
    public ResponseEntity<List<NotificationDto>> findAllUnreadNotification(
        HttpServletRequest request) {
        try {
            Principal principal = request.getUserPrincipal();

            List<NotificationDto> notificationDtos = notificationService.findAll(principal);

            return ResponseEntity
                .status(HttpStatus.OK)
                .body(notificationDtos);
        } catch (Exception e) {
            // HTTP 401
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .build();
        }
    }

    /* 알림 읽음 */
    @DeleteMapping("/{notificationId}")
    @Override
    public ResponseEntity<Void> readNotification(
        @PathVariable UUID notificationId, HttpServletRequest request) {
        try {
            Principal principal = request.getUserPrincipal();
            notificationService.deleteByNotificationId(notificationId, principal);
            // HTTP 204
            return ResponseEntity.noContent().build();
        } catch (UserNotFoundException e) {
            // HTTP 401 - "I don't know who you are."
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (UserAuthorizationException e) {
            // HTTP 403 - "I know who you are, but you are not allowed to do that or access this."
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            // HTTP 404
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        }

    }

}
