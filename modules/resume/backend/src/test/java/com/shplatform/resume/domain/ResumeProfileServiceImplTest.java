package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.ProfileRequest;
import com.shplatform.resume.api.dto.ProfileResponse;
import com.shplatform.resume.infrastructure.entity.ResumeProfileEntity;
import com.shplatform.resume.infrastructure.repository.ResumeProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ResumeProfileServiceImplTest {

    private static final Long USER_ID = 1L;

    @Mock
    private ResumeProfileRepository profileRepository;

    @InjectMocks
    private ResumeProfileServiceImpl profileService;

    private ProfileRequest request() {
        return new ProfileRequest(
                "홍길동", "hong@example.com", "010-1234-5678",
                "서울시 강남구", LocalDate.of(1995, 3, 15),
                "https://photo.example.com/hong.png", "3년차 백엔드 개발자"
        );
    }

    private ResumeProfileEntity entity() {
        var e = ResumeProfileEntity.create(USER_ID);
        e.setId(10L);
        e.setName("홍길동");
        e.setEmail("hong@example.com");
        return e;
    }

    @Test
    @DisplayName("getMyProfile: 등록된 인적사항을 조회한다")
    void getMyProfile_success() {
        given(profileRepository.findByUserId(USER_ID)).willReturn(Optional.of(entity()));

        ProfileResponse response = profileService.getMyProfile(USER_ID);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.email()).isEqualTo("hong@example.com");
    }

    @Test
    @DisplayName("getMyProfile: 인적사항이 없으면 NOT_FOUND 예외가 발생한다")
    void getMyProfile_notFound() {
        given(profileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getMyProfile(USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("upsertProfile: 신규 등록 시 userId가 설정되어 저장된다")
    void upsertProfile_create() {
        given(profileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
        given(profileRepository.save(any(ResumeProfileEntity.class)))
                .willAnswer(invocation -> {
                    var e = invocation.getArgument(0, ResumeProfileEntity.class);
                    e.setId(11L);
                    return e;
                });

        ProfileResponse response = profileService.upsertProfile(USER_ID, request());

        ArgumentCaptor<ResumeProfileEntity> captor = ArgumentCaptor.forClass(ResumeProfileEntity.class);
        then(profileRepository).should(times(1)).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getName()).isEqualTo("홍길동");
        assertThat(response.id()).isEqualTo(11L);
    }

    @Test
    @DisplayName("upsertProfile: 기존 인적사항이 있으면 수정한다")
    void upsertProfile_update() {
        var existing = entity();
        given(profileRepository.findByUserId(USER_ID)).willReturn(Optional.of(existing));
        given(profileRepository.save(any(ResumeProfileEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        var newRequest = new ProfileRequest(
                "김철수", "kim@example.com", null, null, null, null, "신규 소개"
        );
        ProfileResponse response = profileService.upsertProfile(USER_ID, newRequest);

        then(profileRepository).should(times(1)).save(existing);
        assertThat(response.name()).isEqualTo("김철수");
        assertThat(response.headline()).isEqualTo("신규 소개");
    }
}
