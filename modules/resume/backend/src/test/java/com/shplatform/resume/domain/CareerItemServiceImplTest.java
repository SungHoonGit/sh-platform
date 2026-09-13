package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.CareerItemRequest;
import com.shplatform.resume.infrastructure.entity.ResumeCareerEntity;
import com.shplatform.resume.infrastructure.entity.ResumeCareerItemEntity;
import com.shplatform.resume.infrastructure.repository.ResumeCareerItemRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class CareerItemServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 999L;
    private static final Long CAREER_ID = 100L;
    private static final Long ITEM_ID = 200L;
    private static final Long DOCUMENT_ID = 100L;

    @Mock
    private ResumeCareerItemRepository careerItemRepository;
    @Mock
    private ResumeCareerRepository careerRepository;

    @InjectMocks
    private CareerItemServiceImpl careerItemService;

    private ResumeCareerEntity career() {
        var e = ResumeCareerEntity.create(USER_ID, DOCUMENT_ID);
        e.setId(CAREER_ID);
        return e;
    }

    private ResumeCareerItemEntity item() {
        var e = ResumeCareerItemEntity.create(CAREER_ID, DOCUMENT_ID);
        e.setId(ITEM_ID);
        e.setTitle("백엔드 API 개발");
        return e;
    }

    private CareerItemRequest request() {
        return new CareerItemRequest("백엔드 API 개발",
                LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31),
                "채용공고 수집·파싱 API를 개발했다.", 1);
    }

    @Test
    @DisplayName("getCareerItems: 경력의 상세 항목을 순서대로 조회한다")
    void getCareerItems_success() {
        given(careerRepository.findById(CAREER_ID)).willReturn(Optional.of(career()));
        given(careerItemRepository.findByCareerIdAndDocumentIdOrderByDisplayOrderAscIdAsc(CAREER_ID, DOCUMENT_ID))
                .willReturn(List.of(item()));

        var responses = careerItemService.getCareerItems(USER_ID, CAREER_ID, DOCUMENT_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).title()).isEqualTo("백엔드 API 개발");
    }

    @Test
    @DisplayName("getCareerItems: 남의 경력이면 FORBIDDEN 예외가 발생한다")
    void getCareerItems_forbidden() {
        given(careerRepository.findById(CAREER_ID)).willReturn(Optional.of(career()));

        assertThatThrownBy(() -> careerItemService.getCareerItems(OTHER_USER_ID, CAREER_ID, DOCUMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("getCareerItems: 존재하지 않는 경력이면 NOT_FOUND 예외가 발생한다")
    void getCareerItems_notFound() {
        given(careerRepository.findById(CAREER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> careerItemService.getCareerItems(USER_ID, CAREER_ID, DOCUMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("createCareerItem: 경력에 상세 항목을 추가한다")
    void createCareerItem_success() {
        given(careerRepository.findById(CAREER_ID)).willReturn(Optional.of(career()));
        given(careerItemRepository.save(any(ResumeCareerItemEntity.class)))
                .willAnswer(invocation -> {
                    invocation.getArgument(0, ResumeCareerItemEntity.class).setId(ITEM_ID);
                    return invocation.getArgument(0);
                });

        var response = careerItemService.createCareerItem(USER_ID, CAREER_ID, DOCUMENT_ID, request());

        ArgumentCaptor<ResumeCareerItemEntity> captor = ArgumentCaptor.forClass(ResumeCareerItemEntity.class);
        then(careerItemRepository).should(times(1)).save(captor.capture());
        assertThat(captor.getValue().getCareerId()).isEqualTo(CAREER_ID);
        assertThat(captor.getValue().getTitle()).isEqualTo("백엔드 API 개발");
        assertThat(response.id()).isEqualTo(ITEM_ID);
    }

    @Test
    @DisplayName("updateCareerItem: 내 경력의 상세 항목을 수정한다")
    void updateCareerItem_success() {
        var existing = item();
        given(careerRepository.findById(CAREER_ID)).willReturn(Optional.of(career()));
        given(careerItemRepository.findById(ITEM_ID)).willReturn(Optional.of(existing));
        given(careerItemRepository.save(any(ResumeCareerItemEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        var response = careerItemService.updateCareerItem(USER_ID, CAREER_ID, ITEM_ID, DOCUMENT_ID, request());

        then(careerItemRepository).should(times(1)).save(existing);
        assertThat(existing.getStartDate()).isEqualTo(LocalDate.of(2023, 1, 1));
        assertThat(response.title()).isEqualTo("백엔드 API 개발");
    }

    @Test
    @DisplayName("updateCareerItem: 다른 경력에 속한 항목이면 FORBIDDEN 예외가 발생한다")
    void updateCareerItem_wrongCareer() {
        var other = ResumeCareerItemEntity.create(999L, DOCUMENT_ID);
        other.setId(ITEM_ID);
        given(careerRepository.findById(CAREER_ID)).willReturn(Optional.of(career()));
        given(careerItemRepository.findById(ITEM_ID)).willReturn(Optional.of(other));

        assertThatThrownBy(() -> careerItemService.updateCareerItem(USER_ID, CAREER_ID, ITEM_ID, DOCUMENT_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("deleteCareerItem: 내 경력의 상세 항목을 삭제한다")
    void deleteCareerItem_success() {
        var existing = item();
        given(careerRepository.findById(CAREER_ID)).willReturn(Optional.of(career()));
        given(careerItemRepository.findById(ITEM_ID)).willReturn(Optional.of(existing));

        careerItemService.deleteCareerItem(USER_ID, CAREER_ID, ITEM_ID, DOCUMENT_ID);

        then(careerItemRepository).should(times(1)).delete(existing);
    }

    @Test
    @DisplayName("reorderCareerItems: 상세 항목 순서를 재배치한다")
    void reorderCareerItems_success() {
        var first = ResumeCareerItemEntity.create(CAREER_ID, DOCUMENT_ID);
        first.setId(1L);
        var second = ResumeCareerItemEntity.create(CAREER_ID, DOCUMENT_ID);
        second.setId(2L);
        given(careerRepository.findById(CAREER_ID)).willReturn(Optional.of(career()));
        given(careerItemRepository.findByCareerIdAndDocumentIdOrderByDisplayOrderAscIdAsc(CAREER_ID, DOCUMENT_ID))
                .willReturn(List.of(first, second));

        careerItemService.reorderCareerItems(USER_ID, CAREER_ID, DOCUMENT_ID, List.of(2L, 1L));

        assertThat(first.getDisplayOrder()).isEqualTo(2);
        assertThat(second.getDisplayOrder()).isEqualTo(1);
        then(careerItemRepository).should(times(1)).saveAll(List.of(first, second));
    }

    @Test
    @DisplayName("reorderCareerItems: 남의 항목이 섞여 있으면 FORBIDDEN 예외가 발생한다")
    void reorderCareerItems_forbidden() {
        var first = ResumeCareerItemEntity.create(CAREER_ID, DOCUMENT_ID);
        first.setId(1L);
        given(careerRepository.findById(CAREER_ID)).willReturn(Optional.of(career()));
        given(careerItemRepository.findByCareerIdAndDocumentIdOrderByDisplayOrderAscIdAsc(CAREER_ID, DOCUMENT_ID))
                .willReturn(List.of(first));

        assertThatThrownBy(() -> careerItemService.reorderCareerItems(USER_ID, CAREER_ID, DOCUMENT_ID, List.of(999L)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        then(careerItemRepository).should(never()).saveAll(any());
    }
}