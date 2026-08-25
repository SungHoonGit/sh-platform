package com.shplatform.auth.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.shplatform.shared.config.RedisRepository;
import com.shplatform.shared.exception.BusinessException;
import com.shplatform.shared.exception.ErrorCode;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    private static final String USER_SESSIONS_KEY = "user:sessions:1";

    @Mock
    private RedisRepository redisRepository;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(redisRepository);
        ReflectionTestUtils.setField(sessionService, "maxSessionsPerUser", 1);
        ReflectionTestUtils.setField(sessionService, "sessionTimeoutMinutes", 180);
        ReflectionTestUtils.setField(sessionService, "preventDuplicateLogin", true);
    }

    @Test
    void createSession_shouldCreate_whenUnderLimit() {
        when(redisRepository.getSetMembers(USER_SESSIONS_KEY)).thenReturn(Set.of());

        String sessionId = sessionService.createSession(1L, "127.0.0.1", "UA");

        assertNotNull(sessionId);
        verify(redisRepository).addToSet(eq(USER_SESSIONS_KEY), anyString());
        verify(redisRepository, never()).delete(anyString());
    }

    @Test
    void createSession_shouldThrow_whenLimitReachedAndPreventDuplicate() {
        when(redisRepository.getSetMembers(USER_SESSIONS_KEY)).thenReturn(Set.<Object>of("existing"));
        when(redisRepository.hasKey("session:1:existing")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sessionService.createSession(1L, "127.0.0.1", "UA"));

        assertEquals(ErrorCode.SESSION_LIMIT_EXCEEDED, ex.getErrorCode());
        verify(redisRepository, never()).addToSet(anyString(), anyString());
    }

    @Test
    void createSession_shouldEvictOldest_whenLimitReachedWithoutPreventDuplicate() {
        ReflectionTestUtils.setField(sessionService, "preventDuplicateLogin", false);
        when(redisRepository.getSetMembers(USER_SESSIONS_KEY)).thenReturn(Set.<Object>of("old-session"));
        when(redisRepository.hasKey("session:1:old-session")).thenReturn(true);
        when(redisRepository.hashGet("session:1:old-session", "loginAt")).thenReturn("2026-01-01T10:00:00");

        String sessionId = sessionService.createSession(1L, "127.0.0.1", "UA");

        assertNotNull(sessionId);
        verify(redisRepository).delete("session:1:old-session");
        verify(redisRepository).removeFromSet(USER_SESSIONS_KEY, "old-session");
        verify(redisRepository).addToSet(eq(USER_SESSIONS_KEY), anyString());
    }

    @Test
    void getActiveSessionCount_shouldPruneGhostMembers() {
        when(redisRepository.getSetMembers(USER_SESSIONS_KEY))
                .thenReturn(Set.<Object>of("alive", "ghost"));
        when(redisRepository.hasKey("session:1:alive")).thenReturn(true);
        when(redisRepository.hasKey("session:1:ghost")).thenReturn(false);

        int count = sessionService.getActiveSessionCount(1L);

        assertEquals(1, count);
        verify(redisRepository).removeFromSet(USER_SESSIONS_KEY, "ghost");
    }

    @Test
    void createSession_shouldIgnoreGhosts_whenCheckingLimit() {
        when(redisRepository.getSetMembers(USER_SESSIONS_KEY)).thenReturn(Set.<Object>of("ghost"));
        when(redisRepository.hasKey("session:1:ghost")).thenReturn(false);

        String sessionId = sessionService.createSession(1L, "127.0.0.1", "UA");

        assertNotNull(sessionId);
        verify(redisRepository).removeFromSet(USER_SESSIONS_KEY, "ghost");
        verify(redisRepository).addToSet(eq(USER_SESSIONS_KEY), anyString());
    }
}
