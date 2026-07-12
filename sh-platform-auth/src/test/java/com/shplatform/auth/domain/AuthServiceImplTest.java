package com.shplatform.auth.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.shplatform.auth.api.dto.LoginRequest;
import com.shplatform.auth.api.dto.SignupRequest;
import com.shplatform.auth.infrastructure.*;
import com.shplatform.shared.exception.BusinessException;
import com.shplatform.shared.exception.ErrorCode;
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
}
