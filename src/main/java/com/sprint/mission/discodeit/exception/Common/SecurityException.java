package com.sprint.mission.discodeit.exception.Common;

import com.sprint.mission.discodeit.exception.ErrorCode;

public class SecurityException extends CommonException {

    public SecurityException() {
        super(ErrorCode.INVALID_TOKEN);
    }

    public SecurityException(String message) {
        super(ErrorCode.INVALID_TOKEN,
            String.format("보안: %s", message));
    }

    public SecurityException(String message, Throwable cause) {
        super(ErrorCode.INVALID_TOKEN,
            String.format("보안: %s", message), cause);
    }
}
