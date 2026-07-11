package com.shplatform.auth.infrastructure;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenProvider {

    private final String privateKeyPem;
    private final String publicKeyPem;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    public TokenProvider(
            @Value("${jwt.private-key}") String privateKeyPem,
            @Value("${jwt.public-key}") String publicKeyPem,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        this.privateKeyPem = privateKeyPem;
        this.publicKeyPem = publicKeyPem;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    @PostConstruct
    public void init() {
        this.privateKey = readPrivateKey(privateKeyPem);
        this.publicKey = readPublicKey(publicKeyPem);
    }

    public String createAccessToken(Long userId, String email, String role) {
        var now = Instant.now();
        var claims = new JWTClaimsSet.Builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusMillis(accessTokenExpiration)))
                .build();
        return sign(claims);
    }

    public String createRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public Claims validate(String token) {
        try {
            var signedJWT = SignedJWT.parse(token);
            var verifier = new RSASSAVerifier(publicKey);
            if (!signedJWT.verify(verifier)) {
                throw new RuntimeException("Invalid token signature");
            }
            var claims = signedJWT.getJWTClaimsSet();
            if (claims.getExpirationTime().before(new Date())) {
                throw new RuntimeException("Token expired");
            }
            return new Claims(
                    Long.parseLong(claims.getSubject()),
                    claims.getStringClaim("email"),
                    claims.getStringClaim("role")
            );
        } catch (Exception e) {
            throw new RuntimeException("Token validation failed", e);
        }
    }

    private String sign(JWTClaimsSet claims) {
        try {
            var signer = new RSASSASigner(privateKey);
            var signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(),
                    claims
            );
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign JWT", e);
        }
    }

    private RSAPrivateKey readPrivateKey(String pem) {
        try {
            var keyBytes = parsePem(pem, "PRIVATE KEY");
            var spec = new PKCS8EncodedKeySpec(keyBytes);
            var kf = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) kf.generatePrivate(spec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load RSA private key", e);
        }
    }

    private RSAPublicKey readPublicKey(String pem) {
        try {
            var keyBytes = parsePem(pem, "PUBLIC KEY");
            var spec = new X509EncodedKeySpec(keyBytes);
            var kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load RSA public key", e);
        }
    }

    private byte[] parsePem(String pem, String marker) {
        var cleaned = pem
                .replace("-----BEGIN " + marker + "-----", "")
                .replace("-----END " + marker + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(cleaned);
    }

    public record Claims(Long userId, String email, String role) {}
}
