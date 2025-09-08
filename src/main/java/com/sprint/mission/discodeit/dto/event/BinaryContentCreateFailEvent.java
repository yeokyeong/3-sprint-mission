package com.sprint.mission.discodeit.dto.event;

import java.util.UUID;

public record BinaryContentCreateFailEvent(UUID binaryContentId, byte[] bytes, Exception cause) {

}
