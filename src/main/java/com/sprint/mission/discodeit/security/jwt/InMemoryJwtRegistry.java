package com.sprint.mission.discodeit.security.jwt;

import com.sprint.mission.discodeit.dto.data.JwtInformation;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;

@RequiredArgsConstructor
public class InMemoryJwtRegistry implements JwtRegistry {

    private final Map<UUID, Queue<JwtInformation>> origin = new ConcurrentHashMap<>();
    private final Set<String> accessTokenIndexes = ConcurrentHashMap.newKeySet();
    private final Set<String> refreshTokenIndexes = ConcurrentHashMap.newKeySet();

    private final int maxActiveJwtCount;
    private final JwtTokenProvider jwtTokenProvider;


    /* 로그인 시 새로 발급한 토큰 정보 저장 */
    @Override
    public void registerJwtInformation(JwtInformation jwtInformation) {
        origin.compute(jwtInformation.getUserDto().getId(), (key, queue) -> {
            if (queue == null) {
                queue = new ConcurrentLinkedQueue<>();
            }
            // queue 사이즈가 초과되면 가장 오래된 토큰 삭제
            if (queue.size() >= maxActiveJwtCount) {
                JwtInformation deprecatedJwtInformation = queue.poll();
                if (deprecatedJwtInformation != null) {
                    removeTokenIndex(
                        deprecatedJwtInformation.getAccessToken(),
                        deprecatedJwtInformation.getRefreshToken()
                    );
                }
            }
            queue.add(jwtInformation);
            addTokenIndex(
                jwtInformation.getAccessToken(),
                jwtInformation.getRefreshToken()
            );
            return queue;
        });
    }

    /* 사용자의 토큰 정보를 삭제 & 권한 변경할때도 토큰 정보 삭제해야함 */
    @Override
    public void invalidateJwtInformationByUserId(UUID userId) {
        origin.computeIfPresent(userId, (key, queue) -> {
            queue.forEach((jwtInformation -> {
                removeTokenIndex(
                    jwtInformation.getAccessToken(),
                    jwtInformation.getRefreshToken()
                );
            }));
            queue.clear(); // 해당 유저의 큐 삭제
            return null; // registry에서 해당 유저 삭제
        });

    }

    /* 사용자의 토큰 정보가 레지스트리에 존재하는지 확인 */
    @Override
    public boolean hasActiveJwtInformationByUserId(UUID userId) {
        return origin.containsKey(userId);
    }

    /* 엑세스 토큰이 레지스트리에 존재하는지 확인 & 모든 요청에 대한 확인, 없으면 유효하지 않은 토큰으로 간주 */
    @Override
    public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
        return accessTokenIndexes.contains(accessToken);
    }

    /* 리프레시 토큰이 레지스트리에 존재하는지 확인 */
    @Override
    public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
        return refreshTokenIndexes.contains(refreshToken);
    }

    /* 액세스 토큰을 재발급 할 때 Rotation 전략을 활용해 토큰 정보를 업데이트 */
    @Override
    public void rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation) {
        origin.computeIfPresent(newJwtInformation.getUserDto().getId(), (key, queue) -> {
            queue.stream()
                .filter(jwtInformation -> jwtInformation.getRefreshToken().equals(refreshToken))
                .findFirst()
                .ifPresent(jwtInformation -> {
                    removeTokenIndex(jwtInformation.getAccessToken(),
                        jwtInformation.getRefreshToken());
                    jwtInformation.rotate(
                        newJwtInformation.getAccessToken(),
                        newJwtInformation.getRefreshToken()
                    );
                    addTokenIndex(
                        newJwtInformation.getAccessToken(),
                        newJwtInformation.getRefreshToken()
                    );
                });
            return queue;
        });

    }

    @Scheduled(fixedDelay = 1000 * 60 * 5)
    @Override
    public void clearExpiredJwtInformation() {
        origin.entrySet().removeIf(entry -> {
            Queue<JwtInformation> queue = entry.getValue();
            queue.removeIf(jwtInformation -> {
                boolean isExpired =
                    !jwtTokenProvider.validateAccessToken(jwtInformation.getAccessToken()) ||
                        !jwtTokenProvider.validateRefreshToken(jwtInformation.getRefreshToken());
                if (isExpired) {
                    removeTokenIndex(
                        jwtInformation.getAccessToken(),
                        jwtInformation.getRefreshToken()
                    );
                }
                return isExpired;
            });
            return queue.isEmpty(); // Remove the entry if the queue is empty
        });
    }


    private void addTokenIndex(String accessToken, String refreshToken) {
        accessTokenIndexes.add(accessToken);
        refreshTokenIndexes.add(refreshToken);
    }

    private void removeTokenIndex(String accessToken, String refreshToken) {
        accessTokenIndexes.remove(accessToken);
        refreshTokenIndexes.remove(refreshToken);
    }
}
