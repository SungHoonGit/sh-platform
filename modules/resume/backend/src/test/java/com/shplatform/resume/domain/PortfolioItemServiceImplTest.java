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
                null, null, null, null, null, "https://portfolio.example.com", "개인 포트폴리오", 1);
    }

    private PortfolioItemRequest enhancedRequest() {
        return new PortfolioItemRequest("sh-platform", "LINK",
                "6/202608/thumb.png", "https://github.com/owner/repo", "https://demo.example.com",
                "https://youtube.com/watch?v=abc", null, null, "채용공고 스크래핑 플랫폼", 1);
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
    @DisplayName("createPortfolioItem: FILE 타입인데 filePath가 없으면 INVALID_INPUT 예외가 발생한다")
    void createPortfolioItem_fileWithoutPathRejected() {
        var fileRequest = new PortfolioItemRequest("첨부파일", "FILE",
                null, null, null, null, null, null, null, 1);

        assertThatThrownBy(() -> portfolioItemService.createPortfolioItem(USER_ID, fileRequest))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("createPortfolioItem: FILE 타입 작업물을 filePath와 함께 추가한다 (Phase 5)")
    void createPortfolioItem_fileWithPathSuccess() {
        given(portfolioItemRepository.save(any(ResumePortfolioItemEntity.class)))
                .willAnswer(invocation -> {
                    invocation.getArgument(0, ResumePortfolioItemEntity.class).setId(ITEM_ID);
                    return invocation.getArgument(0);
                });
        var fileRequest = new PortfolioItemRequest("기획서", "FILE",
                null, null, null, null, "6/202608/uuid.pptx", null, "서비스 기획서", 2);

        portfolioItemService.createPortfolioItem(USER_ID, fileRequest);

        ArgumentCaptor<ResumePortfolioItemEntity> captor = ArgumentCaptor.forClass(ResumePortfolioItemEntity.class);
        then(portfolioItemRepository).should(times(1)).save(captor.capture());
        assertThat(captor.getValue().getItemType()).isEqualTo("FILE");
        assertThat(captor.getValue().getFilePath()).isEqualTo("6/202608/uuid.pptx");
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
    @DisplayName("createPortfolioItem: 썸네일·다중링크(깃허브/데모/영상)가 저장된다")
    void createPortfolioItem_enhancedFields() {
        given(portfolioItemRepository.save(any(ResumePortfolioItemEntity.class)))
                .willAnswer(invocation -> {
                    invocation.getArgument(0, ResumePortfolioItemEntity.class).setId(ITEM_ID);
                    return invocation.getArgument(0);
                });

        var response = portfolioItemService.createPortfolioItem(USER_ID, enhancedRequest());

        ArgumentCaptor<ResumePortfolioItemEntity> captor = ArgumentCaptor.forClass(ResumePortfolioItemEntity.class);
        then(portfolioItemRepository).should(times(1)).save(captor.capture());
        assertThat(captor.getValue().getThumbnailPath()).isEqualTo("6/202608/thumb.png");
        assertThat(captor.getValue().getGithubUrl()).isEqualTo("https://github.com/owner/repo");
        assertThat(captor.getValue().getDemoUrl()).isEqualTo("https://demo.example.com");
        assertThat(captor.getValue().getVideoUrl()).isEqualTo("https://youtube.com/watch?v=abc");
        assertThat(response.thumbnailPath()).isEqualTo("6/202608/thumb.png");
        assertThat(response.linkUrl()).isNull();
    }

    @Test
    @DisplayName("updatePortfolioItem: 썸네일·다중링크 필드가 갱신된다")
    void updatePortfolioItem_enhancedFields() {
        var existing = entity(USER_ID);
        given(portfolioItemRepository.findById(ITEM_ID)).willReturn(Optional.of(existing));
        given(portfolioItemRepository.save(any(ResumePortfolioItemEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        var response = portfolioItemService.updatePortfolioItem(USER_ID, ITEM_ID, enhancedRequest());

        assertThat(existing.getGithubUrl()).isEqualTo("https://github.com/owner/repo");
        assertThat(existing.getDemoUrl()).isEqualTo("https://demo.example.com");
        assertThat(existing.getVideoUrl()).isEqualTo("https://youtube.com/watch?v=abc");
        assertThat(response.videoUrl()).isEqualTo("https://youtube.com/watch?v=abc");
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

    @Test
    @DisplayName("reorderPortfolioItems: 전달된 id 순서대로 displayOrder를 재지정한다")
    void reorderPortfolioItems_success() {
        var first = entity(USER_ID);
        first.setId(701L);
        first.setDisplayOrder(1);
        var second = entity(USER_ID);
        second.setId(702L);
        second.setDisplayOrder(2);
        given(portfolioItemRepository.findByUserIdOrderByDisplayOrderAscIdAsc(USER_ID))
                .willReturn(List.of(first, second));
        given(portfolioItemRepository.saveAll(any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        portfolioItemService.reorderPortfolioItems(USER_ID, List.of(702L, 701L));

        assertThat(first.getDisplayOrder()).isEqualTo(2);
        assertThat(second.getDisplayOrder()).isEqualTo(1);
        then(portfolioItemRepository).should(times(1)).saveAll(List.of(first, second));
    }

    @Test
    @DisplayName("reorderPortfolioItems: 본인 소유가 아닌 작업물 id가 포함되면 FORBIDDEN 예외가 발생한다")
    void reorderPortfolioItems_forbidden() {
        given(portfolioItemRepository.findByUserIdOrderByDisplayOrderAscIdAsc(USER_ID))
                .willReturn(List.of(entity(USER_ID)));

assertThatThrownBy(() -> portfolioItemService.reorderPortfolioItems(USER_ID, List.of(999L)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}
