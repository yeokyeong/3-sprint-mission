package com.sprint.mission.discodeit.controller.api;

import com.sprint.mission.discodeit.dto.data.NotificationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Notification", description = "Notification API")
public interface NotificationApi {

    @Operation(summary = "알림 조회")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", description = "알림 조회 성공",
            content = @Content(schema = @Schema(implementation = NotificationDto.class))
        ),
        @ApiResponse(
            responseCode = "400", description = "알림을 찾을 수 없음",
            content = @Content(examples = @ExampleObject(value = ""))
        )
    })
    ResponseEntity<List<NotificationDto>> findAllUnreadNotification(HttpServletRequest request);

    /* 특정 사용자가 읽지 않은 모든 알림 조회 */
    @DeleteMapping("/{notificationId}")
    ResponseEntity<Void> readNotification(@PathVariable UUID notificationId,
        HttpServletRequest request);
}
