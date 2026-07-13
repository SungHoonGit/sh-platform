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
    private VerificationCodeRepository verificationCodeRepository;
    @Mock
    private TokenProvider tokenProvider;
    @Mock
    private EmailService emailService;

    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthServiceImpl(
                userRepository, refreshTokenRepository, verificationCodeRepository,
                userMapper, tokenProvider, passwordEncoder, emailService
        );
    }

    // ──────────────────────────────────────────────
    //  signup
    // ──────────────────────────────────────────────

    @Test
    void signup_shouldCreateUser_whenEmailNotDuplicate() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
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
        verify(userRepository).save(any());
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
        when(tokenProvider.createAccessToken(anyLong(), anyString(), anyString())).thenReturn("access-token");
        when(tokenProvider.createRefreshToken()).thenReturn("refresh-token");
        when(tokenProvider.getRefreshTokenExpiration()).thenReturn(1_000_000_000L);

        var request = new LoginRequest("test@example.com", "CorrectPw1!");
        var result = authService.login(request);

        assertNotNull(result.accessToken());
        assertNotNull(result.refreshToken());
        verify(refreshTokenRepository).save(any());
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
        var stored = new RefreshTokenEntity();
        stored.setUserId(1L);
        stored.setToken("valid-refresh");
        stored.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(stored));

        var userEntity = new UserEntity();
        userEntity.setId(1L);
        userEntity.setEmail("test@example.com");
        userEntity.setRole(UserRole.USER);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(userEntity));
        when(tokenProvider.createAccessToken(anyLong(), anyString(), anyString())).thenReturn("new-access");
        when(tokenProvider.createRefreshToken()).thenReturn("new-refresh");
        when(tokenProvider.getRefreshTokenExpiration()).thenReturn(1_000_000_000L);

        var result = authService.refresh("valid-refresh");

        assertEquals("new-access", result.accessToken());
        assertEquals("new-refresh", result.refreshToken());
        verify(refreshTokenRepository).delete(stored);
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void refresh_shouldThrow_whenTokenNotFound() {
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        var ex = assertThrows(BusinessException.class,
                () -> authService.refresh("unknown-token"));
        assertEquals(ErrorCode.TOKEN_INVALID, ex.getErrorCode());
    }

    @Test
    void refresh_shouldThrow_whenTokenExpired() {
        var stored = new RefreshTokenEntity();
        stored.setUserId(1L);
        stored.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(stored));

        var ex = assertThrows(BusinessException.class,
                () -> authService.refresh("expired-token"));
        assertEquals(ErrorCode.TOKEN_EXPIRED, ex.getErrorCode());
        verify(refreshTokenRepository).delete(stored);
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
    }

    @Test
    void logout_shouldDoNothing_whenTokenNotExists() {
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        authService.logout("unknown-token");

        verify(refreshTokenRepository, never()).delete(any());
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
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
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
