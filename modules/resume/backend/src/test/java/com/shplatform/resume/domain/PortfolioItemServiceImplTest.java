package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.PortfolioItemRequest;
import com.shplatform.resume.infrastructure.entity.ResumePortfolioItemEntity;
import com.shplatform.resume.infrastructure.repository.ResumePortfolioItemRepository;
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
class PortfolioItemServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 999L;
    private static final Long ITEM_ID = 700L;

    @Mock
    private ResumePortfolioItemRepository portfolioItemRepository;

    @InjectMocks
    private PortfolioItemServiceImpl portfolioItemService;

    private PortfolioItemRequest linkRequest() {
        return new PortfolioItemRequest("포트폴리오 사이트", "LINK",
                "https://portfolio.example.com", "개인 포트폴리오", 1);
    }

    private ResumePortfolioItemEntity entity(Long userId) {
        var e = ResumePortfolioItemEntity.create(userId);
        e.setId(ITEM_ID);
        e.setTitle("포트폴리오 사이트");
        e.setItemType("LINK");
        return e;
    }

    @Test
    @DisplayName("getPortfolioItems: 작업물 목록을 조회한다")
    void getPortfolioItems_success() {
        given(portfolioItemRepository.findByUserIdOrderByDisplayOrderAscIdAsc(USER_ID))
                .willReturn(List.of(entity(USER_ID)));

        var responses = portfolioItemService.getPortfolioItems(USER_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).title()).isEqualTo("포트폴리오 사이트");
    }

    @Test
    @DisplayName("createPortfolioItem: LINK 작업물을 추가한다")
    void createPortfolioItem_success() {
        given(portfolioItemRepository.save(any(ResumePortfolioItemEntity.class)))
                .willAnswer(invocation -> {
                    invocation.getArgument(0, ResumePortfolioItemEntity.class).setId(ITEM_ID);
                    return invocation.getArgument(0);
                });

        var response = portfolioItemService.createPortfolioItem(USER_ID, linkRequest());

        ArgumentCaptor<ResumePortfolioItemEntity> captor = ArgumentCaptor.forClass(ResumePortfolioItemEntity.class);
        then(portfolioItemRepository).should(times(1)).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getItemType()).isEqualTo("LINK");
        assertThat(response.id()).isEqualTo(ITEM_ID);
    }

    @Test
    @DisplayName("createPortfolioItem: FILE 타입이면 INVALID_INPUT 예외가 발생한다 (Phase 5 지원 예정)")
    void createPortfolioItem_fileRejected() {
        var fileRequest = new PortfolioItemRequest("첨부파일", "FILE",
                null, null, 1);

        assertThatThrownBy(() -> portfolioItemService.createPortfolioItem(USER_ID, fileRequest))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("updatePortfolioItem: 내 작업물을 수정한다")
    void updatePortfolioItem_success() {
        var existing = entity(USER_ID);
        given(portfolioItemRepository.findById(ITEM_ID)).willReturn(Optional.of(existing));
        given(portfolioItemRepository.save(any(ResumePortfolioItemEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        var response = portfolioItemService.updatePortfolioItem(USER_ID, ITEM_ID, linkRequest());

        then(portfolioItemRepository).should(times(1)).save(existing);
        assertThat(response.title()).isEqualTo("포트폴리오 사이트");
    }

    @Test
    @DisplayName("updatePortfolioItem: 다른 사용자의 작업물이면 FORBIDDEN 예외가 발생한다")
    void updatePortfolioItem_forbidden() {
        given(portfolioItemRepository.findById(ITEM_ID)).willReturn(Optional.of(entity(OTHER_USER_ID)));

        assertThatThrownBy(() -> portfolioItemService.updatePortfolioItem(USER_ID, ITEM_ID, linkRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("deletePortfolioItem: 내 작업물을 삭제한다")
    void deletePortfolioItem_success() {
        var existing = entity(USER_ID);
        given(portfolioItemRepository.findById(ITEM_ID)).willReturn(Optional.of(existing));

        portfolioItemService.deletePortfolioItem(USER_ID, ITEM_ID);

        then(portfolioItemRepository).should(times(1)).delete(existing);
    }
}
