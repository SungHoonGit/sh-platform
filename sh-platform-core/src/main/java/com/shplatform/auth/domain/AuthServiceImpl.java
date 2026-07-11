package com.shplatform.auth.domain;

import com.shplatform.auth.api.dto.LoginRequest;
import com.shplatform.auth.api.dto.SignupRequest;
import com.shplatform.auth.api.dto.TokenResponse;
import com.shplatform.auth.infrastructure.*;
import com.shplatform.shared.exception.BusinessException;
import com.shplatform.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final UserMapper userMapper;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthServiceImpl(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           VerificationCodeRepository verificationCodeRepository,
                           UserMapper userMapper,
                           TokenProvider tokenProvider,
                           PasswordEncoder passwordEncoder,
                           EmailService emailService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.userMapper = userMapper;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public User signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        var domain = User.createLocal(request.email(), request.name());
        var entity = userMapper.toEntity(domain, passwordEncoder.encode(request.password()));
        var saved = userRepository.save(entity);
        return userMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void sendVerificationEmail(String email, String purpose) {
        var code = String.format("%06d", (int) (Math.random() * 1000000));
        var entity = new VerificationCodeEntity();
        entity.setEmail(email);
        entity.setCode(code);
        entity.setPurpose(purpose);
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        verificationCodeRepository.save(entity);
        emailService.sendVerificationCode(email, code);
    }

    @Override
    @Transactional
    public void verifyCode(String email, String code, String purpose) {
        var record = verificationCodeRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CODE));
        if (record.isVerified() || record.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.CODE_EXPIRED);
        }
        if (!record.getCode().equals(code)) {
            throw new BusinessException(ErrorCode.INVALID_CODE);
        }
        record.setVerified(true);
        verificationCodeRepository.save(record);

        if ("SIGNUP".equals(purpose)) {
            userRepository.findByEmail(email).ifPresent(entity -> {
                entity.setEmailVerified(true);
                userRepository.save(entity);
            });
        }
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        var entity = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (!passwordEncoder.matches(request.password(), entity.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!entity.isEmailVerified()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        return createTokens(entity.getId(), entity.getEmail(), entity.getRole().name());
    }

    @Override
    @Transactional
    public TokenResponse refresh(String refreshToken) {
        var stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID));
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored);
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }
        var user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        refreshTokenRepository.delete(stored);
        return createTokens(user.getId(), user.getEmail(), user.getRole().name());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
    }

    private TokenResponse createTokens(Long userId, String email, String role) {
        var accessToken = tokenProvider.createAccessToken(userId, email, role);
        var refreshValue = tokenProvider.createRefreshToken();
        var expiresAt = LocalDateTime.now()
                .plusNanos(tokenProvider.getRefreshTokenExpiration() * 1_000_000);

        var entity = new RefreshTokenEntity();
        entity.setUserId(userId);
        entity.setToken(refreshValue);
        entity.setExpiresAt(expiresAt);
        refreshTokenRepository.save(entity);

        return TokenResponse.of(accessToken, refreshValue, 3600);
    }
}
