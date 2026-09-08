package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.CertificateRequest;
import com.shplatform.resume.infrastructure.entity.ResumeCertificateEntity;
import com.shplatform.resume.infrastructure.repository.ResumeCertificateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class CertificateServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 999L;
    private static final Long CERTIFICATE_ID = 400L;

    @Mock
    private ResumeCertificateRepository certificateRepository;

    @InjectMocks
    private CertificateServiceImpl certificateService;

    private CertificateRequest request() {
        return new CertificateRequest("정보처리기사", "한국산업인력공단", null, 1);
    }

    private ResumeCertificateEntity entity(Long userId) {
        var e = ResumeCertificateEntity.create(userId);
        e.setId(CERTIFICATE_ID);
        e.setName("정보처리기사");
        return e;
    }

    @Test
    @DisplayName("getCertificates: 자격증 목록을 조회한다")
    void getCertificates_success() {
        given(certificateRepository.findByUserIdOrderByDisplayOrderAscIdAsc(USER_ID))
                .willReturn(List.of(entity(USER_ID)));

        var responses = certificateService.getCertificates(USER_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).name()).isEqualTo("정보처리기사");
    }

    @Test
    @DisplayName("createCertificate: 자격증을 추가한다")
    void createCertificate_success() {
        given(certificateRepository.save(any(ResumeCertificateEntity.class)))
                .willAnswer(invocation -> {
                    invocation.getArgument(0, ResumeCertificateEntity.class).setId(CERTIFICATE_ID);
                    return invocation.getArgument(0);
                });

        var response = certificateService.createCertificate(USER_ID, request());

        ArgumentCaptor<ResumeCertificateEntity> captor = ArgumentCaptor.forClass(ResumeCertificateEntity.class);
        then(certificateRepository).should(times(1)).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(response.id()).isEqualTo(CERTIFICATE_ID);
    }

    @Test
    @DisplayName("updateCertificate: 내 자격증을 수정한다")
    void updateCertificate_success() {
        var existing = entity(USER_ID);
        given(certificateRepository.findById(CERTIFICATE_ID)).willReturn(Optional.of(existing));
        given(certificateRepository.save(any(ResumeCertificateEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        var response = certificateService.updateCertificate(USER_ID, CERTIFICATE_ID, request());

        then(certificateRepository).should(times(1)).save(existing);
        assertThat(response.name()).isEqualTo("정보처리기사");
    }

    @Test
    @DisplayName("updateCertificate: 다른 사용자의 자격증이면 FORBIDDEN 예외가 발생한다")
    void updateCertificate_forbidden() {
        given(certificateRepository.findById(CERTIFICATE_ID)).willReturn(Optional.of(entity(OTHER_USER_ID)));

        assertThatThrownBy(() -> certificateService.updateCertificate(USER_ID, CERTIFICATE_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("deleteCertificate: 내 자격증을 삭제한다")
    void deleteCertificate_success() {
        var existing = entity(USER_ID);
        given(certificateRepository.findById(CERTIFICATE_ID)).willReturn(Optional.of(existing));

        certificateService.deleteCertificate(USER_ID, CERTIFICATE_ID);

        then(certificateRepository).should(times(1)).delete(existing);
    }

    @Test
    @DisplayName("reorderCertificates: 전달된 id 순서대로 displayOrder를 재지정한다")
    void reorderCertificates_success() {
        var first = entity(USER_ID);
        first.setId(401L);
        first.setDisplayOrder(1);
        var second = entity(USER_ID);
        second.setId(402L);
        second.setDisplayOrder(2);
        given(certificateRepository.findByUserIdOrderByDisplayOrderAscIdAsc(USER_ID))
                .willReturn(List.of(first, second));
        given(certificateRepository.saveAll(any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        certificateService.reorderCertificates(USER_ID, List.of(402L, 401L));

        assertThat(first.getDisplayOrder()).isEqualTo(2);
        assertThat(second.getDisplayOrder()).isEqualTo(1);
        then(certificateRepository).should(times(1)).saveAll(List.of(first, second));
    }

    @Test
    @DisplayName("reorderCertificates: 본인 소유가 아닌 자격증 id가 포함되면 FORBIDDEN 예외가 발생한다")
    void reorderCertificates_forbidden() {
        given(certificateRepository.findByUserIdOrderByDisplayOrderAscIdAsc(USER_ID))
                .willReturn(List.of(entity(USER_ID)));

        assertThatThrownBy(() -> certificateService.reorderCertificates(USER_ID, List.of(999L)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}
