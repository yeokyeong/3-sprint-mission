package com.sprint.mission.discodeit.storage;

import com.sprint.mission.discodeit.dto.data.BinaryContentDto;
import com.sprint.mission.discodeit.dto.event.BinaryContentCreateFailEvent;
import com.sprint.mission.discodeit.entity.BinaryContentStatus;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Slf4j
@Component
@ConditionalOnProperty(name = "discodeit.storage.type", havingValue = "s3")
@ConfigurationProperties(prefix = "discodeit.storage.aws")
@Getter
@Setter
public class S3BinaryContentStorage implements BinaryContentStorage {

    private String accessKey;
    private String secretAccessKey;
    private String s3Bucket;
    private String s3Region;
    private String s3PresignedUrlExpiration;
    private ApplicationEventPublisher eventPublisher;

    public S3BinaryContentStorage(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    public void init() {
        log.info("accessKey = {}", accessKey);
        log.info("secretAccessKey {}", secretAccessKey);
        log.info("s3Bucket {}", s3Bucket);
        log.info("s3Region {}", s3Region);
    }

    //XXX: presignedUrl로 클라이언트에서 바로 AWS에 파일 get,put할수있는데, 이 메서드가 왜 필요하지?,
    // put은 서버에서 presignedUrl 없이 직접 파일 넣어주고, download만 presignedUrl 사용하는건가?
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 4, // 최초호출 + 재시도 3회
//        backoff = @Backoff(delay = 500, multiplier = 2.0)  // 0.5s -> 1s -> 2s
        backoff = @Backoff(delay = 0)  // 테스트용
    )
    @Override
    public BinaryContentStatus put(UUID binaryContentId, byte[] bytes) {
        try {
            log.info("[S3BinaryContentStorage - put] 시작 binaryContentId:{}", binaryContentId);
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(this.s3Bucket)
                .key(binaryContentId.toString())
                .build();

            PutObjectResponse response = this.getS3Client()
                .putObject(putObjectRequest, RequestBody.fromBytes(bytes));

            throw SdkClientException.create("테스트 실패");
//            if (response.sdkHttpResponse().isSuccessful() &&
//                response.eTag() != null) {
//                log.info("S3 Object Upload Success");
//                return BinaryContentStatus.SUCCESS;
//            } else {
//                log.error("fail to upload at S3 id: {}", binaryContentId);
//             throw new S3Exception("업로드 실패", null);
//                return BinaryContentStatus.FAIL;
//            }

        } catch (S3Exception e) {
            log.error("S3Exception while putting file: {}", binaryContentId, e);
            throw e;
        } catch (RuntimeException e) {
            log.error("RuntimeException while putting file: {}", binaryContentId, e);
            throw e;
        }

    }

    @Recover
    public BinaryContentStatus recoverOnFailure(Exception cause, UUID binaryContentId,
        byte[] bytes) {
        String requestId = MDC.get("request-id");
        System.out.println("recoverOnFailure requestId = " + requestId);
        log.error("[S3] putObject final failure. key={}, cause={}", binaryContentId,
            cause.toString());

        //관리자에게 파일 업로드 실패 알림 보내기
        BinaryContentCreateFailEvent failedEvent = new BinaryContentCreateFailEvent(binaryContentId,
            bytes, cause);
        System.out.println("cause.getMessage() = " + cause.getMessage());
        eventPublisher.publishEvent(failedEvent);

        return BinaryContentStatus.FAIL;
    }

    @Override
    public InputStream get(UUID binaryContentId) {
        String key = binaryContentId.toString();
        try {
            S3Client s3Client = getS3Client();

            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(this.s3Bucket)
                .key(key)
                .build();

            byte[] bytes = s3Client.getObjectAsBytes(request).asByteArray();
            return new ByteArrayInputStream(bytes);
        } catch (S3Exception e) {
            log.error("S3에서 파일 다운로드 실패: {}", e.getMessage());
            throw new NoSuchElementException("File with key " + key + " does not exist");
        }
    }

    @Override
    public ResponseEntity<Resource> download(BinaryContentDto binaryContent) {
        try {
            String key = binaryContent.id().toString();
            String presignedUrl = generatePresignedUrl(key, binaryContent.contentType(),
                SdkHttpMethod.GET);

            log.info("생성된 Presigned URL: {}", presignedUrl);

            return ResponseEntity
                .status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, presignedUrl)
                .build();
        } catch (Exception e) {
            log.error("Presigned URL 생성 실패: {}", e.getMessage());
            throw new RuntimeException("Presigned URL 생성 실패", e);
        }
    }


    private String generatePresignedUrl(String key, String contentType, SdkHttpMethod method) {
        if (method == SdkHttpMethod.GET) {
            return generateGetPresignedUrl(key);
        } else if (method == SdkHttpMethod.PUT) {
            return generatePutPresignedUrl(key);
        } else {
            // TODO: error handler 내부 처리
            throw new UnsupportedOperationException("Unsupported HTTP method: " + method);
        }
    }

    private String generatePutPresignedUrl(String key) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(s3Bucket)
            .key(key)
            .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(Long.parseLong(s3PresignedUrlExpiration)))
            .putObjectRequest(putObjectRequest)
            .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner().presignPutObject(presignRequest);
        return presignedRequest.url().toString();
    }

    private String generateGetPresignedUrl(String key) {

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(s3Bucket)
            .key(key)
            .build();

        // you can change expiration time here
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(60))
            .getObjectRequest(getObjectRequest)
            .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner().presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    //AWS S3 버킷에 파일 업로드/다운로드, 목록 조회, 삭제 등 일반적인 API 요청을 보낼 때 사용
    // 즉, 서버가 직접 S3에 접근해야 할 때 사용
    private S3Client getS3Client() {
        try {
            Region awsRegion = Region.of(this.s3Region);
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(this.accessKey,
                this.secretAccessKey);
            return S3Client.builder()
                .region(awsRegion)
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
        } catch (Exception e) {
            // TODO: error 타입 생성 및 처리
            throw new RuntimeException(e);
        }

    }

    private S3Presigner s3Presigner() {
        try {
            Region awsRegion = Region.of(this.s3Region);
            return S3Presigner.builder()
                .region(awsRegion)
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(this.accessKey, this.secretAccessKey)))
                .build();
        } catch (Exception e) {
            // TODO: error 타입 생성 및 처리
            throw new RuntimeException(e);
        }

    }

}
