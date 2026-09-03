package com.shplatform.auth.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.shplatform.auth.api.dto.*;
import com.shplatform.auth.infrastructure.*;
import com.shplatform.shared.exception.BusinessException;
import com.shplatform.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private VerificationCodeRepository verificationCodeRepository;
    @Mock
    private TokenProvider tokenProvider;
    @Mock
    private EmailService emailService;
    @Mock
    private SessionService sessionService;
    @Mock
    private LoginLogService loginLogService;

    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthServiceImpl(
                userRepository, refreshTokenRepository, refreshTokenService,
                verificationCodeRepository, userMapper, tokenProvider,
                passwordEncoder, emailService, sessionService, loginLogService
        );
    }

    // ──────────────────────────────────────────────
    //  signup
    // ──────────────────────────────────────────────

    @Test
    void signup_shouldCreateVerifiedUser_whenVerificationCodeIsVerified() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        var codeRecord = new VerificationCodeEntity();
        codeRecord.setEmail("test@example.com");
        codeRecord.setCode("123456");
        codeRecord.setPurpose("SIGNUP");
        codeRecord.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        codeRecord.setVerified(true);
        when(verificationCodeRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(
                anyString(), anyString())).thenReturn(Optional.of(codeRecord));
        var saved = new UserEntity();
        saved.setId(1L);
        saved.setEmail("test@example.com");
        saved.setName("테스터");
        when(userRepository.save(any())).thenReturn(saved);

        var request = new SignupRequest("test@example.com", "Password1!", "테스터");
        var result = authService.signup(request);

        assertEquals("test@example.com", result.email());
        assertEquals("테스터", result.name());
        assertEquals("LOCAL", result.provider());
        var captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertTrue(captor.getValue().isEmailVerified());
    }

    @Test
    void signup_shouldThrow_whenEmailNotVerified() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(verificationCodeRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(
                anyString(), anyString())).thenReturn(Optional.empty());

        var request = new SignupRequest("test@example.com", "Password1!", "테스터");
        var ex = assertThrows(BusinessException.class, () -> authService.signup(request));
        assertEquals(ErrorCode.EMAIL_NOT_VERIFIED, ex.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void signup_shouldThrow_whenEmailDuplicate() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        var request = new SignupRequest("dup@example.com", "Password1!", "테스터");
        var ex = assertThrows(BusinessException.class, () -> authService.signup(request));
        assertEquals(ErrorCode.DUPLICATE_EMAIL, ex.getErrorCode());
    }

    // ──────────────────────────────────────────────
    //  login
    // ──────────────────────────────────────────────

    @Test
    void login_shouldSucceed_whenCredentialsMatch() {
        var entity = new UserEntity();
        entity.setId(1L);
        entity.setEmail("test@example.com");
        entity.setPassword(passwordEncoder.encode("CorrectPw1!"));
        entity.setEmailVerified(true);
        entity.setRole(UserRole.USER);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(entity));
        when(tokenProvider.createAccessToken(anyLong(), anyString(), anyString(), nullable(String.class))).thenReturn("access-token");
        when(tokenProvider.createRefreshToken()).thenReturn("refresh-token");

        var request = new LoginRequest("test@example.com", "CorrectPw1!");
        var result = authService.login(request);

        assertNotNull(result.accessToken());
        assertNotNull(result.refreshToken());
        verify(refreshTokenService).save(eq("refresh-token"), eq(1L), nullable(String.class));
    }

    @Test
    void login_shouldThrow_whenEmailNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        var request = new LoginRequest("no@email.com", "password");
        var ex = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    void login_shouldThrow_whenPasswordMismatch() {
        var entity = new UserEntity();
        entity.setEmail("test@example.com");
        entity.setPassword(passwordEncoder.encode("CorrectPw1!"));
        entity.setEmailVerified(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(entity));

        var request = new LoginRequest("test@example.com", "WrongPw1!");
        var ex = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    void login_shouldThrow_whenEmailNotVerified() {
        var entity = new UserEntity();
        entity.setEmail("test@example.com");
        entity.setPassword(passwordEncoder.encode("CorrectPw1!"));
        entity.setEmailVerified(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(entity));

        var request = new LoginRequest("test@example.com", "CorrectPw1!");
        var ex = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals(ErrorCode.EMAIL_NOT_VERIFIED, ex.getErrorCode());
    }

    // ──────────────────────────────────────────────
    //  sendVerificationEmail / verifyCode
    // ──────────────────────────────────────────────

    @Test
    void sendVerificationEmail_shouldSaveCodeAndSendEmail() {
        authService.sendVerificationEmail("test@example.com", "SIGNUP");

        verify(verificationCodeRepository).save(any());
        verify(emailService).sendVerificationCode(eq("test@example.com"), anyString());
    }

    @Test
    void sendVerificationEmail_shouldThrow_whenDuplicateEmailForSignup() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        var ex = assertThrows(BusinessException.class,
                () -> authService.sendVerificationEmail("test@example.com", "SIGNUP"));

        assertEquals(ErrorCode.DUPLICATE_EMAIL, ex.getErrorCode());
        verify(verificationCodeRepository, never()).save(any());
        verify(emailService, never()).sendVerificationCode(anyString(), anyString());
    }

    @Test
    void verifyCode_shouldSucceed_whenCodeMatch() {
        var record = new VerificationCodeEntity();
        record.setEmail("test@example.com");
        record.setCode("123456");
        record.setPurpose("SIGNUP");
        record.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(verificationCodeRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(
                anyString(), anyString())).thenReturn(Optional.of(record));

        authService.verifyCode("test@example.com", "123456", "SIGNUP");

        assertTrue(record.isVerified());
        verify(verificationCodeRepository).save(record);
    }

    @Test
    void verifyCode_shouldThrow_whenCodeNotFound() {
        when(verificationCodeRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(
                anyString(), anyString())).thenReturn(Optional.empty());

        var ex = assertThrows(BusinessException.class,
                () -> authService.verifyCode("test@example.com", "000000", "SIGNUP"));
        assertEquals(ErrorCode.INVALID_CODE, ex.getErrorCode());
    }

    @Test
    void verifyCode_shouldThrow_whenCodeExpired() {
        var record = new VerificationCodeEntity();
        record.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(verificationCodeRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(
                anyString(), anyString())).thenReturn(Optional.of(record));

        var ex = assertThrows(BusinessException.class,
                () -> authService.verifyCode("test@example.com", "000000", "SIGNUP"));
        assertEquals(ErrorCode.CODE_EXPIRED, ex.getErrorCode());
    }

    @Test
    void verifyCode_shouldThrow_whenCodeMismatch() {
        var record = new VerificationCodeEntity();
        record.setCode("123456");
        record.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(verificationCodeRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(
                anyString(), anyString())).thenReturn(Optional.of(record));

        var ex = assertThrows(BusinessException.class,
                () -> authService.verifyCode("test@example.com", "000000", "SIGNUP"));
        assertEquals(ErrorCode.INVALID_CODE, ex.getErrorCode());
    }

    // ──────────────────────────────────────────────
    //  refresh
    // ──────────────────────────────────────────────

    @Test
    void refresh_shouldSucceed_whenTokenValid() {
        when(refreshTokenService.getUserId("valid-refresh")).thenReturn(1L);

        var userEntity = new UserEntity();
        userEntity.setId(1L);
        userEntity.setEmail("test@example.com");
        userEntity.setRole(UserRole.USER);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(userEntity));
        when(tokenProvider.createAccessToken(anyLong(), anyString(), anyString(), nullable(String.class))).thenReturn("new-access");
        when(tokenProvider.createRefreshToken()).thenReturn("new-refresh");

        var result = authService.refresh("valid-refresh");

        assertEquals("new-access", result.accessToken());
        assertEquals("new-refresh", result.refreshToken());
        verify(refreshTokenService).delete("valid-refresh");
        verify(refreshTokenService).save(eq("new-refresh"), eq(1L), nullable(String.class));
    }

    @Test
    void refresh_shouldSucceed_whenTokenNotFoundInRedisButExistsInMariaDB() {
        when(refreshTokenService.getUserId("old-refresh")).thenReturn(null);

        var stored = new RefreshTokenEntity();
        stored.setUserId(1L);
        stored.setToken("old-refresh");
        stored.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByToken("old-refresh")).thenReturn(Optional.of(stored));

        var userEntity = new UserEntity();
        userEntity.setId(1L);
        userEntity.setEmail("test@example.com");
        userEntity.setRole(UserRole.USER);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(userEntity));
        when(tokenProvider.createAccessToken(anyLong(), anyString(), anyString(), nullable(String.class))).thenReturn("new-access");
        when(tokenProvider.createRefreshToken()).thenReturn("new-refresh");

        var result = authService.refresh("old-refresh");

        assertEquals("new-access", result.accessToken());
        verify(refreshTokenRepository).delete(stored);
    }

    @Test
    void refresh_shouldThrow_whenTokenNotFound() {
        when(refreshTokenService.getUserId("unknown-token")).thenReturn(null);
        when(refreshTokenRepository.findByToken("unknown-token")).thenReturn(Optional.empty());

        var ex = assertThrows(BusinessException.class,
                () -> authService.refresh("unknown-token"));
        assertEquals(ErrorCode.TOKEN_INVALID, ex.getErrorCode());
    }

    // ──────────────────────────────────────────────
    //  logout
    // ──────────────────────────────────────────────

    @Test
    void logout_shouldDeleteRefreshToken_whenExists() {
        var stored = new RefreshTokenEntity();
        stored.setUserId(1L);
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(stored));

        authService.logout("some-token");

        verify(refreshTokenRepository).delete(stored);
        verify(refreshTokenService).delete("some-token");
    }

    @Test
    void logout_shouldDeleteRedisToken_whenNotInMariaDB() {
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        authService.logout("redis-only-token");

        verify(refreshTokenRepository, never()).delete(any());
        verify(refreshTokenService).delete("redis-only-token");
    }

    // ──────────────────────────────────────────────
    //  getUser
    // ──────────────────────────────────────────────

    @Test
    void getUser_shouldReturnUser_whenFound() {
        var entity = new UserEntity();
        entity.setId(1L);
        entity.setEmail("test@example.com");
        entity.setName("테스터");
        entity.setRole(UserRole.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));

        var result = authService.getUser(1L);

        assertEquals(1L, result.id());
        assertEquals("test@example.com", result.email());
        assertEquals("테스터", result.name());
    }

    @Test
    void getUser_shouldThrow_whenNotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        var ex = assertThrows(BusinessException.class,
                () -> authService.getUser(999L));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    // ──────────────────────────────────────────────
    //  updateProfile
    // ──────────────────────────────────────────────

    @Test
    void updateProfile_shouldUpdateNameAndLocale() {
        var entity = new UserEntity();
        entity.setId(1L);
        entity.setName("old");
        entity.setLocale("ko");
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdateProfileRequest("new-name", "en");
        var result = authService.updateProfile(1L, request);

        assertEquals("new-name", result.name());
        assertEquals("en", result.locale());
    }

    @Test
    void updateProfile_shouldUpdateOnlyProvidedFields() {
        var entity = new UserEntity();
        entity.setId(1L);
        entity.setName("old");
        entity.setLocale("ko");
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdateProfileRequest("new-name", null);
        var result = authService.updateProfile(1L, request);

        assertEquals("new-name", result.name());
        assertEquals("ko", result.locale());
    }

    @Test
    void updateProfile_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        var request = new UpdateProfileRequest("name", null);
        var ex = assertThrows(BusinessException.class,
                () -> authService.updateProfile(999L, request));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    // ──────────────────────────────────────────────
    //  changePassword
    // ──────────────────────────────────────────────

    @Test
    void changePassword_shouldUpdate_whenCurrentPasswordMatch() {
        var entity = new UserEntity();
        entity.setPassword(passwordEncoder.encode("OldPw1!"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));

        var request = new ChangePasswordRequest("OldPw1!", "NewPw1@34");
        authService.changePassword(1L, request);

        assertTrue(passwordEncoder.matches("NewPw1@34", entity.getPassword()));
        verify(userRepository).save(entity);
    }

    @Test
    void changePassword_shouldThrow_whenCurrentPasswordWrong() {
        var entity = new UserEntity();
        entity.setPassword(passwordEncoder.encode("OldPw1!"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));

        var request = new ChangePasswordRequest("WrongPw!", "NewPw1@34");
        var ex = assertThrows(BusinessException.class,
                () -> authService.changePassword(1L, request));
        assertEquals(ErrorCode.WRONG_CURRENT_PASSWORD, ex.getErrorCode());
    }

    @Test
    void changePassword_shouldThrow_whenOAuth2User() {
        var entity = new UserEntity();
        entity.setPassword(null);
        entity.setProvider("KAKAO");
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));

        var request = new ChangePasswordRequest("any", "NewPw1@34");
        var ex = assertThrows(BusinessException.class,
                () -> authService.changePassword(1L, request));
        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void changePassword_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        var request = new ChangePasswordRequest("any", "NewPw1@34");
        var ex = assertThrows(BusinessException.class,
                () -> authService.changePassword(999L, request));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    // ──────────────────────────────────────────────
    //  setPassword
    // ──────────────────────────────────────────────

    @Test
    void setPassword_shouldSetPassword_whenOAuth2User() {
        var entity = new UserEntity();
        entity.setPassword(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));

        var request = new SetPasswordRequest("NewPw1@34");
        authService.setPassword(1L, request);

        assertTrue(passwordEncoder.matches("NewPw1@34", entity.getPassword()));
        verify(userRepository).save(entity);
    }

    @Test
    void setPassword_shouldThrow_whenPasswordAlreadySet() {
        var entity = new UserEntity();
        entity.setPassword(passwordEncoder.encode("OldPw1!"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));

        var request = new SetPasswordRequest("NewPw1@34");
        var ex = assertThrows(BusinessException.class,
                () -> authService.setPassword(1L, request));
        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void setPassword_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        var request = new SetPasswordRequest("NewPw1@34");
        var ex = assertThrows(BusinessException.class,
                () -> authService.setPassword(999L, request));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    // ──────────────────────────────────────────────
    //  deleteAccount
    // ──────────────────────────────────────────────

    @Test
    void deleteAccount_shouldDeleteLocalUser_whenPasswordMatch() {
        var entity = new UserEntity();
        entity.setId(1L);
        entity.setPassword(passwordEncoder.encode("MyPw1!"));
        entity.setProvider("LOCAL");
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));

        authService.deleteAccount(1L, "MyPw1!");

        verify(refreshTokenRepository).deleteByUserId(1L);
        verify(userRepository).delete(entity);
    }

    @Test
    void deleteAccount_shouldDeleteOAuth2User_withoutPassword() {
        var entity = new UserEntity();
        entity.setId(1L);
        entity.setPassword(null);
        entity.setProvider("KAKAO");
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));

        authService.deleteAccount(1L, null);

        verify(userRepository).delete(entity);
    }

    @Test
    void deleteAccount_shouldThrow_whenLocalUserPasswordWrong() {
        var entity = new UserEntity();
        entity.setPassword(passwordEncoder.encode("RealPw1!"));
        entity.setProvider("LOCAL");
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));

        var ex = assertThrows(BusinessException.class,
                () -> authService.deleteAccount(1L, "WrongPw!"));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    void deleteAccount_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        var ex = assertThrows(BusinessException.class,
                () -> authService.deleteAccount(999L, null));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }
}
