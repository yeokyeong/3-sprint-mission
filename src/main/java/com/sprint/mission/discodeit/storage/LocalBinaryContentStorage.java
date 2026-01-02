package com.sprint.mission.discodeit.storage;

import com.sprint.mission.discodeit.dto.data.BinaryContentDto;
import com.sprint.mission.discodeit.dto.event.BinaryContentCreateFailEvent;
import com.sprint.mission.discodeit.entity.BinaryContentStatus;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "discodeit.storage.type", havingValue = "local")
@Slf4j
public class LocalBinaryContentStorage implements BinaryContentStorage {

    private final Path root;
    private final ApplicationEventPublisher eventPublisher;

    public LocalBinaryContentStorage(@Value("${discodeit.storage.local.root-path}") Path root, ApplicationEventPublisher eventPublisher) {
        this.root = root;
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    public void init() {
        if (!Files.exists(this.root)) {
            try {
                Files.createDirectories(this.root);
            } catch (IOException e) {
                // TODO : 다른 에러로 변경
                throw new RuntimeException(e);
            }
        }
    }

    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 4, // 최초호출 + 재시도 3회
            backoff = @Backoff(delay = 500, multiplier = 2.0)  // 0.5s -> 1s -> 2s
    )
    @Override
    public BinaryContentStatus put(UUID binaryContentId, byte[] bytes) {
        // 객체를 저장할 파일 path 생성
        Path filePath = this.resolvePath(binaryContentId);
        if (Files.exists(filePath)) {
            throw new IllegalArgumentException(
                    "File with key " + binaryContentId + " already exists");
        }
        try (OutputStream outputStream = Files.newOutputStream(filePath)) {
            Thread.sleep(3000);
            outputStream.write(bytes);
            return BinaryContentStatus.SUCCESS;
        } catch (IOException e) {
            log.error("I/O error while writing file: {}", binaryContentId, e);
            return BinaryContentStatus.FAIL;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while simulating delay", e);
        }

        // 의도적 실패
//        throw new RuntimeException("의도적으로 실패함");
    }

    @Recover
    public BinaryContentStatus recoverOnFailure(Exception cause, UUID binaryContentId, byte[] bytes) {
        String requestId = MDC.get("request_id");
        System.out.println("recoverOnFailure requestId = " + requestId);
        log.error("[S3] putObject final failure. key={}, cause={}", binaryContentId,
                cause.toString());

        //관리자에게 파일 업로드 실패 알림 보내기
        BinaryContentCreateFailEvent failedEvent = new BinaryContentCreateFailEvent(binaryContentId, bytes, cause);

        System.out.println("cause.getMessage() = " + cause.getMessage());
        eventPublisher.publishEvent(failedEvent);
        return BinaryContentStatus.FAIL;
    }

    @Override
    public InputStream get(UUID binaryContentId) {
        Path filePath = this.resolvePath(binaryContentId);
        if (Files.notExists(filePath)) {
            throw new NoSuchElementException(
                    "File with key " + binaryContentId + " does not exist");
        }
        try {
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResponseEntity<Resource> download(BinaryContentDto metaData) {
        InputStream inputStream = get(metaData.id());
        Resource resource = new InputStreamResource(inputStream);

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + metaData.fileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, metaData.contentType())
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(metaData.size()))
                .body(resource);
    }

    private Path resolvePath(UUID id) {
        return this.root.resolve(id.toString());
    }

}
