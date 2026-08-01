package com.shplatform.common.security;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT(RS256) 검증 유틸. auth 서비스가 발급한 토큰을 공용 공개키로 검증한다.
 *
 * <p>jwt.public-key 는 중앙 .env 의 {@code JWT_PUBLIC_KEY} 를 참조한다.</p>
 */
@Component
public class JwtTokenValidator {

    private final String publicKeyPem;
    private RSAPublicKey publicKey;

    public JwtTokenValidator(@Value("${jwt.public-key}") String publicKeyPem) {
        this.publicKeyPem = publicKeyPem;
    }

    @PostConstruct
    public void init() {
        this.publicKey = readPublicKey(publicKeyPem);
    }

    /**
     * JWT 서명·만료를 검증하고 클레임을 반환한다.
     *
     * @param token Bearer 접두어가 없는 JWT 문자열
     * @return userId(email, role) 클레임
     * @throws RuntimeException 서명 불일치/만료/형식 오류 시
     */
    public JwtClaims validate(String token) {
        try {
            var signedJWT = SignedJWT.parse(token);
            var verifier = new RSASSAVerifier(publicKey);
            if (!signedJWT.verify(verifier)) {
                throw new RuntimeException("Invalid token signature");
            }
            var claims = signedJWT.getJWTClaimsSet();
            if (claims.getExpirationTime() != null && claims.getExpirationTime().before(new Date())) {
                throw new RuntimeException("Token expired");
            }
            return new JwtClaims(
                    Long.parseLong(claims.getSubject()),
                    claims.getStringClaim("email"),
                    claims.getStringClaim("role")
            );
        } catch (Exception e) {
            throw new RuntimeException("Token validation failed", e);
        }
    }

    private RSAPublicKey readPublicKey(String pem) {
        try {
            var cleaned = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            var spec = new X509EncodedKeySpec(Base64.getDecoder().decode(cleaned));
            var kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA public key", e);
        }
    }

    public record JwtClaims(Long userId, String email, String role) {}
}
