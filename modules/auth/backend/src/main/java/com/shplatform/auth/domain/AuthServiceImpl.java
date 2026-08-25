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
    private final RefreshTokenService refreshTokenService;
    private final VerificationCodeRepository verificationCodeRepository;
    private final UserMapper userMapper;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SessionService sessionService;
    private final LoginLogService loginLogService;

    public AuthServiceImpl(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           RefreshTokenService refreshTokenService,
                           VerificationCodeRepository verificationCodeRepository,
                           UserMapper userMapper,
                           TokenProvider tokenProvider,
                           PasswordEncoder passwordEncoder,
                           EmailService emailService,
                           SessionService sessionService,
                           LoginLogService loginLogService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenService = refreshTokenService;
        this.verificationCodeRepository = verificationCodeRepository;
        this.userMapper = userMapper;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.sessionService = sessionService;
        this.loginLogService = loginLogService;
    }

    @Override
    @Transactional
    public User signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        // 회원가입 플로우는 이메일 인증(verify-code) 선행이 전제이므로,
        // 검증 완료된 SIGNUP 코드가 있는 경우에만 인증 완료 상태로 계정을 생성한다.
        verificationCodeRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(request.email(), "SIGNUP")
                .filter(VerificationCodeEntity::isVerified)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED));
        var domain = User.createLocal(request.email(), request.name()).verifyEmail();
        var entity = userMapper.toEntity(domain, passwordEncoder.encode(request.password()));
        var saved = userRepository.save(entity);
        log.info("[AUTH] signup success: email={}, userId={}", request.email(), saved.getId());
        return userMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void sendVerificationEmail(String email, String purpose) {
        if ("SIGNUP".equals(purpose) && userRepository.existsByEmail(email)) {
            log.warn("[AUTH] verification email rejected (duplicate email): email={}", email);
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
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
                    loginLogService.logLogin(null, request.email(), null, null, false);
                    log.warn("[AUTH] login failed (user not found): email={}", request.email());
                    return new BusinessException(ErrorCode.UNAUTHORIZED);
                });
        if (!passwordEncoder.matches(request.password(), entity.getPassword())) {
            loginLogService.logLogin(entity.getId(), request.email(), null, null, false);
            log.warn("[AUTH] login failed (wrong password): email={}", request.email());
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!entity.isEmailVerified()) {
            loginLogService.logLogin(entity.getId(), request.email(), null, null, false);
            log.warn("[AUTH] login failed (email not verified): email={}", request.email());
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        loginLogService.logLogin(entity.getId(), request.email(), null, null, true);
        log.info("[AUTH] login success: email={}, userId={}, provider=LOCAL", request.email(), entity.getId());
        return createTokens(entity.getId(), entity.getEmail(), entity.getRole().name());
    }

    @Override
    @Transactional
    public TokenResponse loginWithSession(LoginRequest request, String ip, String device) {
        var entity = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    loginLogService.logLogin(null, request.email(), ip, device, false);
                    log.warn("[AUTH] login failed (user not found): email={}", request.email());
                    return new BusinessException(ErrorCode.UNAUTHORIZED);
                });
        if (!passwordEncoder.matches(request.password(), entity.getPassword())) {
            loginLogService.logLogin(entity.getId(), request.email(), ip, device, false);
            log.warn("[AUTH] login failed (wrong password): email={}", request.email());
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!entity.isEmailVerified()) {
            loginLogService.logLogin(entity.getId(), request.email(), ip, device, false);
            log.warn("[AUTH] login failed (email not verified): email={}", request.email());
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        String sessionId = sessionService.createSession(entity.getId(), ip, device);
        loginLogService.logLogin(entity.getId(), request.email(), ip, device, true);
        log.info("[AUTH] login success: email={}, userId={}, sessionId={}", request.email(), entity.getId(), sessionId);

        return createTokens(entity.getId(), entity.getEmail(), entity.getRole().name());
    }

    @Override
    @Transactional
    public TokenResponse refresh(String refreshToken) {
        Long userId = refreshTokenService.getUserId(refreshToken);
        if (userId == null) {
            log.warn("[AUTH] token refresh failed (token not found in Redis)");
            var stored = refreshTokenRepository.findByToken(refreshToken).orElse(null);
            if (stored != null) {
                userId = stored.getUserId();
                refreshTokenRepository.delete(stored);
                log.info("[AUTH] migrated refresh token from MariaDB to Redis: userId={}", userId);
            } else {
                throw new BusinessException(ErrorCode.TOKEN_INVALID);
            }
        } else {
            refreshTokenService.delete(refreshToken);
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        log.info("[AUTH] token refresh success: userId={}", user.getId());
        return createTokens(user.getId(), user.getEmail(), user.getRole().name());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(entity -> {
                    refreshTokenRepository.delete(entity);
                    log.info("[AUTH] logout(mariadb): userId={}", entity.getUserId());
                });
        refreshTokenService.delete(refreshToken);
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
    public void setPassword(Long userId, SetPasswordRequest request) {
        var entity = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (entity.getPassword() != null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        entity.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(entity);
        log.info("[AUTH] password set: userId={}", userId);
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

        refreshTokenService.save(refreshValue, userId);

        return TokenResponse.of(accessToken, refreshValue, 3600);
    }
}
