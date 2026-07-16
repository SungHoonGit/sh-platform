package com.shplatform.auth.domain;

import com.shplatform.auth.api.dto.*;
import com.shplatform.auth.infrastructure.*;
import com.shplatform.shared.exception.BusinessException;
import com.shplatform.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

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
        log.info("[AUTH] signup success: email={}, userId={}", request.email(), saved.getId());
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
        log.info("[AUTH] verification email sent: email={}, purpose={}", email, purpose);
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
            log.warn("[AUTH] verify code failed: email={}, purpose={}", email, purpose);
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
        log.info("[AUTH] verify code success: email={}, purpose={}", email, purpose);
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        var entity = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("[AUTH] login failed (user not found): email={}", request.email());
                    return new BusinessException(ErrorCode.UNAUTHORIZED);
                });
        if (!passwordEncoder.matches(request.password(), entity.getPassword())) {
            log.warn("[AUTH] login failed (wrong password): email={}", request.email());
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!entity.isEmailVerified()) {
            log.warn("[AUTH] login failed (email not verified): email={}", request.email());
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        log.info("[AUTH] login success: email={}, userId={}, provider=LOCAL", request.email(), entity.getId());
        return createTokens(entity.getId(), entity.getEmail(), entity.getRole().name());
    }

    @Override
    @Transactional
    public TokenResponse refresh(String refreshToken) {
        var stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> {
                    log.warn("[AUTH] token refresh failed (token not found)");
                    return new BusinessException(ErrorCode.TOKEN_INVALID);
                });
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored);
            log.warn("[AUTH] token refresh failed (token expired): userId={}", stored.getUserId());
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }
        var user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        refreshTokenRepository.delete(stored);
        log.info("[AUTH] token refresh success: userId={}", user.getId());
        return createTokens(user.getId(), user.getEmail(), user.getRole().name());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(entity -> {
                    refreshTokenRepository.delete(entity);
                    log.info("[AUTH] logout: userId={}", entity.getUserId());
                });
    }

    @Override
    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        var entity = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return userMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        var entity = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (request.name() != null) {
            entity.setName(request.name());
        }
        if (request.locale() != null) {
            entity.setLocale(request.locale());
        }
        var saved = userRepository.save(entity);
        log.info("[AUTH] profile updated: userId={}", userId);
        return userMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        var entity = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (entity.getPassword() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!passwordEncoder.matches(request.currentPassword(), entity.getPassword())) {
            log.warn("[AUTH] change password failed (wrong password): userId={}", userId);
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        entity.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(entity);
        log.info("[AUTH] password changed: userId={}", userId);
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId, String password) {
        var entity = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (entity.getProvider().equals("LOCAL")) {
            if (password == null || !passwordEncoder.matches(password, entity.getPassword())) {
                log.warn("[AUTH] delete account failed (wrong password): userId={}", userId);
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
        }
        refreshTokenRepository.deleteByUserId(userId);
        userRepository.delete(entity);
        log.info("[AUTH] account deleted: userId={}", userId);
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
