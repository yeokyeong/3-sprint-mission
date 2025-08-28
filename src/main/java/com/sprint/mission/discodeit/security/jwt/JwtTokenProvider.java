package com.sprint.mission.discodeit.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sprint.mission.discodeit.dto.data.DiscodeitUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

//(핵심로직) 토큰을 발급, 갱신, 유효성 검사를 담당하는 컴포넌트(JwtTokenProvider)를 구현하세요.
@Component
public class JwtTokenProvider {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";

    private final int accessTokenExpirationMs;
    private final int refreshTokenExpirationMs;

    private final JWSSigner accessTokenSigner;
    private final JWSVerifier accessTokenVerifier;

    private final JWSSigner refreshTokenSigner;
    private final JWSVerifier refreshTokenVerifier;


    public JwtTokenProvider(
        @Value("${jwt.access-token.exp}") int accessTokenExpirationMs,
        @Value("${jwt.access-token.secret}") String accessTokenSecret,
        @Value("${jwt.refresh-token.exp}") int refreshTokenExpirationMs,
        @Value("${jwt.refresh-token.secret}") String refreshTokenSecret
    ) throws JOSEException {
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;

        byte[] accessTokenSecretBytes = accessTokenSecret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenSigner = new MACSigner(accessTokenSecretBytes);
        this.accessTokenVerifier = new MACVerifier(accessTokenSecretBytes);

        byte[] refreshTokenSecretBytes = refreshTokenSecret.getBytes(StandardCharsets.UTF_8);
        this.refreshTokenSigner = new MACSigner(refreshTokenSecretBytes);
        this.refreshTokenVerifier = new MACVerifier(refreshTokenSecretBytes);
    }

    public String generateAccessToken(DiscodeitUserDetails userDetails) throws JOSEException {
        System.out.println("[TokenProvider] generateAccessToken 호출됨: " + userDetails.getUsername()
            + "의 엑세스 토큰 생성");

        return generateToken(userDetails, accessTokenExpirationMs, accessTokenSigner, "access");
    }

    public String generateRefreshToken(DiscodeitUserDetails userDetails) throws JOSEException {
        System.out.println("[TokenProvider] generateRefreshToken 호출됨: " + userDetails.getUsername()
            + "의 리프레쉬 토큰 생성");

        return generateToken(userDetails, refreshTokenExpirationMs, refreshTokenSigner, "refresh");
    }


    public void addRefreshTokenAtCookie(HttpServletResponse response, String refreshToken) {
        System.out.println("[TokenProvider] addRefreshTokenAtCookie 호출됨: RT 쿠키 응답에 추가");
        Cookie cookie = generateCookie(refreshToken);

        response.addCookie(cookie);
    }


    public boolean validateRefreshToken(String token) {
        System.out.println("validateRefreshToken, REFRESH token = " + token);
        return verifyToken(token, refreshTokenVerifier, "refresh");
    }

    public boolean validateAccessToken(String token) {
        System.out.println("validateAccessToken, ACCESS token = " + token);
        return verifyToken(token, accessTokenVerifier, "access");
    }

    public void expireRefreshCookie(HttpServletResponse response) {
        System.out.println("[TokenProvider] expireRefreshToken 호출됨: 만료 쿠키 응답에 추가");
        Cookie cookie = generateRefreshTokenExpirationCookie();
        response.addCookie(cookie);

    }

    private Cookie generateRefreshTokenExpirationCookie() {
        System.out.println(
            "[TokenProvider] generateCookieToExpireRefreshToken 호출됨: Refresh Token 만료 쿠키 생성");
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");

        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        return cookie;
    }

    public String getUsername(String token) {
        try {
            SignedJWT parsedJWT = SignedJWT.parse(token);
            return parsedJWT.getJWTClaimsSet().getSubject();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    public String getTokenId(String token) {
        try {
            SignedJWT parsedJWT = SignedJWT.parse(token);
            return parsedJWT.getJWTClaimsSet().getJWTID();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    public UUID getUserId(String token) {
        try {
            SignedJWT parsedJWT = SignedJWT.parse(token);
            UUID userId = UUID.fromString((String) parsedJWT.getJWTClaimsSet().getClaim("userId"));
            return userId;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    public Date getIssuedAt(String token) {
        try {
            SignedJWT parsedJWT = SignedJWT.parse(token);
            return parsedJWT.getJWTClaimsSet().getIssueTime();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    public Date getExpiration(String token) {
        try {
            SignedJWT parsedJWT = SignedJWT.parse(token);
            return parsedJWT.getJWTClaimsSet().getExpirationTime();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    /*----private-----*/

    private String generateToken(DiscodeitUserDetails userDetails, int expirationMs,
        JWSSigner signer, String tokenType) throws JOSEException {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        //JWT 헤더 설정
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
            .type(JOSEObjectType.JWT)
            .build();

        //클레임 설정
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .jwtID(UUID.randomUUID().toString())
            .subject(userDetails.getUsername())
            .issueTime(now)
            .expirationTime(expiryDate)
            .claim("userId", userDetails.getUserDto().getId())
            .claim("type", tokenType)
            .claim("roles",
                userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
            .build();

        //JWT 토큰 생성
        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        signedJWT.sign(signer);

        //JWT 토큰을 컴팩트 형식으로 변경
        String compactToken = signedJWT.serialize();
        System.out.println("JWT 토큰 생성 완료 (" + tokenType + ") : " + compactToken);
        return compactToken;
    }

    private Cookie generateCookie(String refreshToken) {
        System.out.println("[TokenProvider] generateRefreshTokenCookie 호출됨: Refresh Token 쿠키 생성");

        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);

        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(refreshTokenExpirationMs / 1000);

        System.out.println(
            "[TokenProvider] generateRefreshTokenCookie 완료: Max-Age=" + (refreshTokenExpirationMs
                / 1000));

        return cookie;

    }

    private boolean verifyToken(String token, JWSVerifier tokenVerifier, String expectedType) {
        try {
            SignedJWT parsedJWT = SignedJWT.parse(token);

            //1. 서명 무결성 검증
            if (!parsedJWT.verify(tokenVerifier)) {
                System.out.println("====== 토큰 검증 실패(서명 무결성) ======");
                return false;
            }
            JWTClaimsSet parsedClaims = parsedJWT.getJWTClaimsSet();

            //2. 토큰 타입 검증
            String tokenType = parsedClaims.getClaim("type").toString();
            if (!expectedType.equals(tokenType)) {
                System.out.println("====== 토큰 검증 실패(토큰 타입) ======");
                return false;
            }
            //3. 토큰 exp 검증
            Date exp = parsedClaims.getExpirationTime();
            if (exp == null || exp.before(new Date())) {
                System.out.println("====== 토큰 검증 실패(exp 검증) ======");
                return false;
            }

            System.out.println("====== 토큰 검증 성공 ======");
            return true;

        } catch (Exception e) {
            System.out.println("====== 토큰 검증 실패 ======" + e.getMessage());
            return false;
        }
    }

}
