package com.shplatform.auth.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

import com.shplatform.auth.infrastructure.AdminAuditLogEntity;
import com.shplatform.auth.infrastructure.AdminAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminAuditServiceTest {

    @Mock
    private AdminAuditLogRepository repository;

    private AdminAuditService adminAuditService;

    @BeforeEach
    void setUp() {
        adminAuditService = new AdminAuditService(repository);
    }

    @Test
    void record_shouldSaveEntity_withAllFields() {
        adminAuditService.record(1L, AdminAuditService.ACTION_ROLE_CHANGE,
                2L, "USER", "ADMIN", "127.0.0.1");

        ArgumentCaptor<AdminAuditLogEntity> captor = ArgumentCaptor.forClass(AdminAuditLogEntity.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        var saved = captor.getValue();
        assertEquals(1L, saved.getActorUserId());
        assertEquals("ROLE_CHANGE", saved.getAction());
        assertEquals(2L, saved.getTargetUserId());
        assertEquals("USER", saved.getBeforeValue());
        assertEquals("ADMIN", saved.getAfterValue());
        assertEquals("127.0.0.1", saved.getIp());
    }

    @Test
    void record_shouldNotThrow_whenRepositoryFails() {
        org.mockito.Mockito.when(repository.save(any(AdminAuditLogEntity.class)))
                .thenThrow(new RuntimeException("db down"));

        assertDoesNotThrow(() ->
                adminAuditService.record(1L, AdminAuditService.ACTION_DELETE_USER, 2L, "a@b.c", null, null));
    }
}
