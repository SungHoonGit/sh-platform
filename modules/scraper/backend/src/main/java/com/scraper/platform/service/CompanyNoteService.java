package com.scraper.platform.service;

import com.scraper.platform.api.dto.CompanyNoteDetailResponse;
import com.scraper.platform.api.dto.CompanyNoteRequest;
import com.scraper.platform.api.dto.CompanyNoteResponse;
import com.scraper.platform.model.CompanyBlacklist;
import com.scraper.platform.model.CompanyNote;
import com.scraper.platform.model.CompanyRating;
import com.scraper.platform.repository.CompanyBlacklistRepository;
import com.scraper.platform.repository.CompanyNoteRepository;
import com.scraper.platform.repository.CompanyRatingRepository;
import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 계정별 회사 메모 (내 별점·북마크·MD 분석).
 * 크롤링 평점({@code company_ratings}, 전역)과 차단({@code company_blacklist}, 계정별)을 조인해 응답한다.
 * 회사명 정규화는 {@link CompanyBlacklistService#normalize(String)} 단일 소스를 재사용한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyNoteService {

    private static final int MAX_PAGE_SIZE = 200;
    private static final DateTimeFormatter EXPORT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CompanyNoteRepository noteRepository;
    private final CompanyBlacklistRepository blacklistRepository;
    private final CompanyRatingRepository ratingRepository;

    /**
     * (질의형) 내 회사 목록을 탭별로 조회한다.
     * all=내 메모 전체, bookmarked=북마크만, blocked=차단 목록(메모 없어도 표시, 메모·평점 조인).
     *
     * @param accountId 소유 계정
     * @param tab all|bookmarked|blocked (기본 all)
     * @param q 회사명 부분 검색 (null/blank이면 전체)
     * @param page 0-base 페이지
     * @param size 페이지 크기 (1~200, 기본 50)
     * @return 평점·차단여부 조인 목록 페이지
     */
    public Page<CompanyNoteResponse> list(Long accountId, String tab, String q, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "updatedAt"));
        String keyword = (q == null || q.isBlank()) ? null : q.trim();
        if ("blocked".equalsIgnoreCase(tab)) {
            return listBlocked(accountId, keyword, pageable);
        }
        Page<CompanyNote> notes;
        if ("bookmarked".equalsIgnoreCase(tab)) {
            notes = (keyword == null)
                    ? noteRepository.findByAccountIdAndIsBookmarkedTrue(accountId, pageable)
                    : noteRepository.findByAccountIdAndIsBookmarkedTrueAndCompanyNameDisplayContainingIgnoreCase(
                            accountId, keyword, pageable);
        } else {
            notes = (keyword == null)
                    ? noteRepository.findByAccountId(accountId, pageable)
                    : noteRepository.findByAccountIdAndCompanyNameDisplayContainingIgnoreCase(
                            accountId, keyword, pageable);
        }
        return enrich(notes, accountId);
    }

    /**
     * (질의형) 메모 상세를 조회한다 (note_md 포함).
     *
     * @param accountId 소유 계정
     * @param id 메모 ID
     * @return 상세 응답
     * @throws BusinessException NOT_FOUND (없거나 타인 소유)
     */
    public CompanyNoteDetailResponse get(Long accountId, Long id) {
        CompanyNote note = ownedNote(accountId, id);
        return toDetail(note, blockedNames(accountId), ratingsByNormalized(displayNames(List.of(note))));
    }

    /**
     * (명령형) 회사 메모를 생성한다. 같은 정규화 회사명이 있으면 갱신한다(멱등).
     *
     * @param accountId 소유 계정
     * @param request 회사명(필수), 내 별점 1~5, 북마크, MD
     * @return 저장된 상세 응답
     * @throws BusinessException INVALID_INPUT (회사명 누락/초과, 별점 범위 벗어남)
     */
    @Transactional
    public CompanyNoteDetailResponse upsert(Long accountId, CompanyNoteRequest request) {
        validate(request, true);
        String normalized = CompanyBlacklistService.normalize(request.companyName());
        String display = request.companyName().trim();
        CompanyNote note = noteRepository.findByAccountIdAndCompanyNameNormalized(accountId, normalized)
                .orElseGet(() -> CompanyNote.builder()
                        .accountId(accountId)
                        .companyNameNormalized(normalized)
                        .companyNameDisplay(display)
                        .build());
        note.setCompanyNameDisplay(display);
        applyFields(note, request);
        CompanyNote saved = noteRepository.save(note);
        return toDetail(saved, blockedNames(accountId), ratingsByNormalized(displayNames(List.of(saved))));
    }

    /**
     * (명령형) 메모를 수정한다. 회사명 변경은 무시된다(정규화 키 유지).
     *
     * @param accountId 소유 계정
     * @param id 메모 ID
     * @param request 별점, 북마크, MD (null이면 기존 유지)
     * @return 수정된 상세 응답
     * @throws BusinessException NOT_FOUND, INVALID_INPUT
     */
    @Transactional
    public CompanyNoteDetailResponse update(Long accountId, Long id, CompanyNoteRequest request) {
        validate(request, false);
        CompanyNote note = ownedNote(accountId, id);
        applyFields(note, request);
        CompanyNote saved = noteRepository.save(note);
        return toDetail(saved, blockedNames(accountId), ratingsByNormalized(displayNames(List.of(saved))));
    }

    /**
     * (명령형) 메모를 삭제한다.
     *
     * @param accountId 소유 계정
     * @param id 메모 ID
     * @throws BusinessException NOT_FOUND (없거나 타인 소유)
     */
    @Transactional
    public void delete(Long accountId, Long id) {
        CompanyNote note = ownedNote(accountId, id);
        noteRepository.delete(note);
    }

    /**
     * (질의형) 메모를 .md 내보내기용 마크다운 텍스트로 변환한다.
     *
     * @param accountId 소유 계정
     * @param id 메모 ID
     * @return 마크다운 원문
     * @throws BusinessException NOT_FOUND
     */
    public String exportMarkdown(Long accountId, Long id) {
        CompanyNote note = ownedNote(accountId, id);
        Map<String, CompanyRating> ratings = ratingsByNormalized(displayNames(List.of(note)));
        CompanyRating rating = ratings.get(note.getCompanyNameNormalized());
        String blockedLine = blacklistRepository
                .findByAccountIdAndCompanyNameNormalized(accountId, note.getCompanyNameNormalized())
                .map(b -> "예" + (b.getReason() != null && !b.getReason().isBlank()
                        ? " (사유: " + b.getReason() + ")" : ""))
                .orElse("아니오");
        StringBuilder md = new StringBuilder();
        md.append("# ").append(note.getCompanyNameDisplay()).append("\n\n");
        md.append("- 내 별점: ").append(starsLine(note.getMyStars())).append("\n");
        md.append("- 북마크: ").append(Boolean.TRUE.equals(note.getIsBookmarked()) ? "예" : "아니오").append("\n");
        md.append("- 차단: ").append(blockedLine).append("\n");
        md.append("- 크롤링 평점: ").append(ratingLine(rating)).append("\n");
        if (note.getUpdatedAt() != null) {
            md.append("- 업데이트: ").append(note.getUpdatedAt().format(EXPORT_DATE)).append("\n");
        }
        md.append("\n## 분석 메모\n\n");
        md.append((note.getNoteMd() == null || note.getNoteMd().isBlank()) ? "_메모 없음_" : note.getNoteMd().trim())
                .append("\n");
        return md.toString();
    }

    private void validate(CompanyNoteRequest request, boolean requireName) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (requireName && (request.companyName() == null || request.companyName().isBlank())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (request.companyName() != null && request.companyName().trim().length() > 200) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (request.myStars() != null && (request.myStars() < 1 || request.myStars() > 5)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private void applyFields(CompanyNote note, CompanyNoteRequest request) {
        if (request.myStars() != null) {
            note.setMyStars(request.myStars());
        }
        if (request.isBookmarked() != null) {
            note.setIsBookmarked(request.isBookmarked());
        }
        if (request.noteMd() != null) {
            note.setNoteMd(request.noteMd().isBlank() ? null : request.noteMd());
        }
    }

    private CompanyNote ownedNote(Long accountId, Long id) {
        return noteRepository.findById(id)
                .filter(n -> n.getAccountId().equals(accountId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private Page<CompanyNoteResponse> enrich(Page<CompanyNote> notes, Long accountId) {
        Map<String, CompanyRating> ratings = ratingsByNormalized(displayNames(notes.getContent()));
        Map<String, Boolean> blocked = blockedNames(accountId);
        List<CompanyNoteResponse> items = notes.getContent().stream()
                .map(n -> toResponse(n, blocked.getOrDefault(n.getCompanyNameNormalized(), false),
                        ratings.get(n.getCompanyNameNormalized())))
                .toList();
        return new PageImpl<>(items, notes.getPageable(), notes.getTotalElements());
    }

    private Page<CompanyNoteResponse> listBlocked(Long accountId, String keyword, Pageable pageable) {
        List<CompanyBlacklist> entries = blacklistRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
        if (keyword != null) {
            String lower = keyword.toLowerCase();
            entries = entries.stream()
                    .filter(b -> b.getCompanyNameNormalized().contains(lower))
                    .toList();
        }
        List<String> normalized = entries.stream().map(CompanyBlacklist::getCompanyNameNormalized).toList();
        Map<String, CompanyNote> notes = new HashMap<>();
        for (CompanyNote n : noteRepository.findByAccountIdAndCompanyNameNormalizedIn(accountId, normalized)) {
            notes.put(n.getCompanyNameNormalized(), n);
        }
        Map<String, CompanyRating> ratings = ratingsByNormalized(normalized);
        List<CompanyNoteResponse> items = entries.stream()
                .map(b -> {
                    CompanyNote n = notes.get(b.getCompanyNameNormalized());
                    String display = (n != null) ? n.getCompanyNameDisplay() : b.getCompanyNameNormalized();
                    return toResponse(n, display, b.getCompanyNameNormalized(), true,
                            ratings.get(b.getCompanyNameNormalized()));
                })
                .toList();
        int total = items.size();
        int from = Math.min((int) pageable.getOffset(), total);
        int to = Math.min(from + pageable.getPageSize(), total);
        return new PageImpl<>(items.subList(from, to), pageable, total);
    }

    private List<String> displayNames(List<CompanyNote> notes) {
        return notes.stream().map(CompanyNote::getCompanyNameDisplay).toList();
    }

    private Map<String, Boolean> blockedNames(Long accountId) {
        Map<String, Boolean> blocked = new HashMap<>();
        for (CompanyBlacklist b : blacklistRepository.findByAccountIdOrderByCreatedAtDesc(accountId)) {
            blocked.put(b.getCompanyNameNormalized(), true);
        }
        return blocked;
    }

    /**
     * 크롤링 평점을 정규화 회사명 기준으로 매칭한다.
     * ratings 키가 원문 회사명이므로 normalize 후 비교한다 (불일치 시 해당 회사 평점 생략).
     */
    private Map<String, CompanyRating> ratingsByNormalized(List<String> displayNames) {
        Map<String, CompanyRating> matched = new HashMap<>();
        if (displayNames.isEmpty()) {
            return matched;
        }
        for (CompanyRating r : ratingRepository.findByCompanyNameIn(displayNames)) {
            if (r.getCompanyName() != null) {
                matched.put(CompanyBlacklistService.normalize(r.getCompanyName()), r);
            }
        }
        return matched;
    }

    private CompanyNoteResponse toResponse(CompanyNote note, boolean blocked, CompanyRating rating) {
        return toResponse(note, note.getCompanyNameDisplay(), note.getCompanyNameNormalized(), blocked, rating);
    }

    private CompanyNoteResponse toResponse(CompanyNote note, String display, String normalized,
                                           boolean blocked, CompanyRating rating) {
        return new CompanyNoteResponse(
                note != null ? note.getId() : null,
                display,
                note != null ? note.getMyStars() : null,
                note != null && Boolean.TRUE.equals(note.getIsBookmarked()),
                blocked,
                note != null && note.getNoteMd() != null && !note.getNoteMd().isBlank(),
                rating != null ? rating.getAverageScore() : null,
                rating != null ? rating.getJobplanetScore() : null,
                rating != null ? rating.getJobkoreaScore() : null,
                rating != null ? rating.getSaraminScore() : null,
                note != null ? note.getUpdatedAt() : null);
    }

    private CompanyNoteDetailResponse toDetail(CompanyNote note, Map<String, Boolean> blocked,
                                              Map<String, CompanyRating> ratings) {
        CompanyRating rating = ratings.get(note.getCompanyNameNormalized());
        return new CompanyNoteDetailResponse(
                note.getId(),
                note.getCompanyNameDisplay(),
                note.getMyStars(),
                Boolean.TRUE.equals(note.getIsBookmarked()),
                blocked.getOrDefault(note.getCompanyNameNormalized(), false),
                note.getNoteMd(),
                rating != null ? rating.getAverageScore() : null,
                rating != null ? rating.getJobplanetScore() : null,
                rating != null ? rating.getJobkoreaScore() : null,
                rating != null ? rating.getSaraminScore() : null,
                note.getUpdatedAt());
    }

    private String starsLine(Integer stars) {
        if (stars == null) {
            return "미지정";
        }
        return "★".repeat(stars) + "☆".repeat(5 - stars) + " (" + stars + "/5)";
    }

    private String ratingLine(CompanyRating rating) {
        if (rating == null || rating.getAverageScore() == null) {
            return "없음";
        }
        StringBuilder sb = new StringBuilder(String.valueOf(rating.getAverageScore()));
        sb.append(" (잡플래닛 ").append(scoreOrDash(rating.getJobplanetScore()));
        sb.append(" · 잡코리아 ").append(scoreOrDash(rating.getJobkoreaScore()));
        sb.append(" · 사람인 ").append(scoreOrDash(rating.getSaraminScore())).append(")");
        return sb.toString();
    }

    private String scoreOrDash(Double score) {
        return score == null ? "-" : String.valueOf(score);
    }
}
