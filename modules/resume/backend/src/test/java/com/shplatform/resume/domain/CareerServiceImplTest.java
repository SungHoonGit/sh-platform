package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.CareerRequest;
import com.shplatform.resume.api.dto.CareerResponse;
import com.shplatform.resume.infrastructure.entity.ResumeCareerEntity;
import com.shplatform.resume.infrastructure.repository.ResumeCareerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class CareerServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 999L;
    private static final Long CAREER_ID = 100L;

    @Mock
    private ResumeCareerRepository careerRepository;

    @InjectMocks
    private CareerServiceImpl careerService;

    private CareerRequest request() {
        return new CareerRequest(
                "테크컴퍼니", "백엔드 개발자",
                LocalDate.of(2023, 1, 2), null,
                "API 개발", 1
        );
    }

    private ResumeCareerEntity entity(Long userId) {
        var e = ResumeCareerEntity.create(userId);
        e.setId(CAREER_ID);
        e.setCompany("테크컴퍼니");
        e.setTitle("백엔드 개발자");
        e.setDisplayOrder(1);
        return e;
    }

    @Test
    @DisplayName("getCareers: 표시 순서대로 경력 목록을 조회한다")
    void getCareers_success() {
        given(careerRepository.findByUserIdOrderByDisplayOrderAscIdAsc(USER_ID))
                .willReturn(List.of(entity(USER_ID)));

        List<CareerResponse> responses = careerService.getCareers(USER_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).company()).isEqualTo("테크컴퍼니");
    }

    @Test
    @DisplayName("createCareer: 경력을 추가한다")
    void createCareer_success() {
        given(careerRepository.save(any(ResumeCareerEntity.class)))
                .willAnswer(invocation -> {
                    var e = invocation.getArgument(0, ResumeCareerEntity.class);
                    e.setId(CAREER_ID);
                    return e;
                });

        CareerResponse response = careerService.createCareer(USER_ID, request());

        ArgumentCaptor<ResumeCareerEntity> captor = ArgumentCaptor.forClass(ResumeCareerEntity.class);
        then(careerRepository).should(times(1)).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getCompany()).isEqualTo("테크컴퍼니");
        assertThat(response.id()).isEqualTo(CAREER_ID);
    }

    @Test
    @DisplayName("updateCareer: 내 경력을 수정한다")
    void updateCareer_success() {
        var existing = entity(USER_ID);
        given(careerRepository.findById(CAREER_ID)).willReturn(Optional.of(existing));
        given(careerRepository.save(any(ResumeCareerEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        var newRequest = new CareerRequest(
                "뉴컴퍼니", "리드 개발자",
                LocalDate.of(2024, 5, 1), LocalDate.of(2026, 1, 31),
                "팀 리딩", 2
        );
        CareerResponse response = careerService.updateCareer(USER_ID, CAREER_ID, newRequest);

        then(careerRepository).should(times(1)).save(existing);
        assertThat(response.company()).isEqualTo("뉴컴퍼니");
        assertThat(response.title()).isEqualTo("리드 개발자");
    }

    @Test
    @DisplayName("updateCareer: 경력이 없으면 NOT_FOUND 예외가 발생한다")
    void updateCareer_notFound() {
        given(careerRepository.findById(CAREER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> careerService.updateCareer(USER_ID, CAREER_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("updateCareer: 다른 사용자의 경력이면 FORBIDDEN 예외가 발생한다")
    void updateCareer_forbidden() {
        given(careerRepository.findById(CAREER_ID)).willReturn(Optional.of(entity(OTHER_USER_ID)));

        assertThatThrownBy(() -> careerService.updateCareer(USER_ID, CAREER_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("deleteCareer: 내 경력을 삭제한다")
    void deleteCareer_success() {
        var existing = entity(USER_ID);
        given(careerRepository.findById(CAREER_ID)).willReturn(Optional.of(existing));

        careerService.deleteCareer(USER_ID, CAREER_ID);

        then(careerRepository).should(times(1)).delete(existing);
    }

    @Test
    @DisplayName("deleteCareer: 다른 사용자의 경력이면 FORBIDDEN 예외가 발생한다")
    void deleteCareer_forbidden() {
        given(careerRepository.findById(CAREER_ID)).willReturn(Optional.of(entity(OTHER_USER_ID)));

        assertThatThrownBy(() -> careerService.deleteCareer(USER_ID, CAREER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}
