package com.scraper.platform.service;

import com.scraper.platform.api.dto.CompanyNoteDetailResponse;
import com.scraper.platform.api.dto.CompanyNoteRequest;
import com.scraper.platform.api.dto.CompanyNoteResponse;
import com.scraper.platform.api.dto.CompanyPostingItem;
import com.scraper.platform.api.dto.CompanySuggestItem;
import com.scraper.platform.api.dto.NoteCategoryResponse;
import com.scraper.platform.model.CompanyBlacklist;
import com.scraper.platform.model.CompanyNote;
import com.scraper.platform.model.CompanyRating;
import com.scraper.platform.repository.CompanyBlacklistRepository;
import com.scraper.platform.repository.CompanyNoteRepository;
import com.scraper.platform.repository.CompanyRatingRepository;
import com.scraper.platform.repository.JobPostingRepository;
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

import java.time.LocalDateTime;
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
    private final JobPostingRepository jobPostingRepository;
    private final BlockReasonService blockReasonService;

    /**
     * (질의형) 내 회사 목록을 탭별로 조회한다.
     * all=내 메모 전체 + 메모 없는 차단 회사(차단 뱃지 표시),
     * bookmarked=북마크만, blocked=차단 목록(메모 없어도 표시, 메모·평점·숨김수 조인).
     *
     * @param accountId 소유 계정
     * @param tab all|bookmarked|blocked (기본 all)
     * @param q 회사명 부분 검색 (null/blank이면 전체)
     * @param page 0-base 페이지
     * @param size 페이지 크기 (1~200, 기본 50)
     * @return 평점·차단여부 조인 목록 페이지
     */
    public Page<CompanyNoteResponse> list(Long accountId, String tab, String q, int page, int size) {
        return list(accountId, tab, q, page, size, "updated", "desc");
    }

    /**
     * (질의형) 내 회사 목록을 탭별로 조회한다. 정렬 키를 지정할 수 있다.
     *
     * @param accountId 소유 계정
     * @param tab all|bookmarked|blocked (기본 all)
     * @param q 회사명 부분 검색 (null/blank이면 전체)
     * @param page 0-base 페이지
     * @param size 페이지 크기 (1~200, 기본 50)
     * @param sort display|stars|updated (기본 updated)
     * @param dir asc|desc (기본 desc, display는 asc 권장)
     * @return 평점·차단여부 조인 목록 페이지
     */
    public Page<CompanyNoteResponse> list(Long accountId, String tab, String q, int page, int size,
                                          String sort, String dir) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, resolveSort(sort, dir));
        String keyword = (q == null || q.isBlank()) ? null : q.trim();
        if ("blocked".equalsIgnoreCase(tab)) {
            return listBlocked(accountId, keyword, pageable, sort, dir);
        }
        Page<CompanyNote> notes;
        boolean includeBlockedOnly = !"bookmarked".equalsIgnoreCase(tab);
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
        return enrich(notes, accountId, keyword, includeBlockedOnly);
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
     * 차단된 회사는 북마크가 강제 해제된다 (차단+북마크 상호배타).
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
        applyFields(accountId, note, request);
        dropBookmarkIfBlocked(accountId, note);
        CompanyNote saved = noteRepository.save(note);
        return toDetail(saved, blockedNames(accountId), ratingsByNormalized(displayNames(List.of(saved))));
    }

    /**
     * (명령형) 메모를 수정한다. 회사명 변경은 무시된다(정규화 키 유지).
     * 차단된 회사는 북마크가 강제 해제된다 (차단+북마크 상호배타).
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
        applyFields(accountId, note, request);
        dropBookmarkIfBlocked(accountId, note);
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

    /**
     * (질의형) 회사의 최근 저장 공고를 조회한다 (슬라이드 보기 탭용).
     * 정규화 키워드 정확 일치 + 수집일 내림차순, 최대 size건 (1~50, 기본 10).
     * 슬라이드 열 때 1회만 실행되므로 수천 건 규모에서 부담 없다.
     *
     * @param accountId 소유 계정 (메모 소유 확인용, 공고 자체는 전역 조회)
     * @param id 메모 ID
     * @param size 최대 건수
     * @return 최근 공고 목록
     * @throws BusinessException NOT_FOUND
     */
    public List<CompanyPostingItem> recentPostings(Long accountId, Long id, int size) {
        CompanyNote note = ownedNote(accountId, id);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(0, safeSize);
        return jobPostingRepository.findRecentByNormalizedCompany(note.getCompanyNameNormalized(), pageable)
                .stream()
                .map(j -> new CompanyPostingItem(
                        j.getPosition(), j.getSiteName(), j.getCompany(), j.getCrawledAt(), j.getUrl()))
                .toList();
    }

    /**
     * (질의형) 수집된 회사명 자동완성. 회사 추가 모달에서 뷰어 수집 회사를 검색한다.
     * DISTINCT + LIMIT 8 + 300ms 디바운스 호출 — 수천 건 규모에서 부담 없다.
     * 내 메모·차단 여부를 함께 표시해 중복 생성을 방지한다.
     *
     * @param accountId 소유 계정
     * @param q 부분 검색어 (blank이면 빈 목록)
     * @return 회사명 후보 (최대 8건)
     */
    public List<CompanySuggestItem> suggestCompanies(Long accountId, String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        List<String> names = jobPostingRepository.findDistinctCompanyByCompanyContainingIgnoreCase(
                q.trim(), PageRequest.of(0, 8));
        if (names.isEmpty()) {
            return List.of();
        }
        Map<String, CompanyNote> noteMap = new HashMap<>();
        for (CompanyNote n : noteRepository.findByAccountIdAndCompanyNameNormalizedIn(
                accountId, names.stream().map(CompanyBlacklistService::normalize).toList())) {
            noteMap.put(n.getCompanyNameNormalized(), n);
        }
        Map<String, Boolean> blocked = blockedNames(accountId);
        return names.stream()
                .map(name -> {
                    String normalized = CompanyBlacklistService.normalize(name);
                    CompanyNote n = noteMap.get(normalized);
                    return new CompanySuggestItem(
                            name,
                            normalized,
                            n != null,
                            n != null ? n.getId() : null,
                            blocked.getOrDefault(normalized, false));
                })
                .toList();
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

    private void applyFields(Long accountId, CompanyNote note, CompanyNoteRequest request) {
        if (request.myStars() != null) {
            note.setMyStars(request.myStars());
            // 별점은 북마크 필수: 별 설정 시 북마크 자동 ON
            note.setIsBookmarked(true);
        }
        if (request.isBookmarked() != null) {
            note.setIsBookmarked(request.isBookmarked());
            // 북마크 해제 시 별도 함께 삭제 (별 단독 불가)
            if (Boolean.FALSE.equals(request.isBookmarked())) {
                note.setMyStars(null);
            }
        }
        if (request.noteMd() != null) {
            note.setNoteMd(request.noteMd().isBlank() ? null : request.noteMd());
        }
        // 태그는 null이면 유지, 비어 있으면 전체 해제 (차단 카테고리와 동일 규칙)
        if (request.reasonIds() != null || request.categoryNames() != null) {
            note.setNoteReasons(blockReasonService.resolveCategories(
                    request.reasonIds(), request.categoryNames()));
        }
    }

    /**
     * 차단된 회사의 북마크·별점을 강제 해제한다. 차단+북마크/별점은 상호배타
     * (차단된 회사 공고는 숨겨지므로 북마크 의미가 없고, 별은 북마크 필수이므로 함께 삭제).
     */
    private void dropBookmarkIfBlocked(Long accountId, CompanyNote note) {
        if ((Boolean.TRUE.equals(note.getIsBookmarked()) || note.getMyStars() != null)
                && blacklistRepository.existsByAccountIdAndCompanyNameNormalized(
                        accountId, note.getCompanyNameNormalized())) {
            note.setIsBookmarked(false);
            note.setMyStars(null);
        }
    }

    private CompanyNote ownedNote(Long accountId, Long id) {
        return noteRepository.findById(id)
                .filter(n -> n.getAccountId().equals(accountId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private Page<CompanyNoteResponse> enrich(Page<CompanyNote> notes, Long accountId, String keyword,
                                              boolean includeBlockedOnly) {
        Map<String, CompanyRating> ratings = ratingsByNormalized(displayNames(notes.getContent()));
        Map<String, Boolean> blocked = blockedNames(accountId);
        // 메모 회사들의 관련 저장 공고 수도 한 번의 GROUP BY로 함께 집계한다 (비고 열 표시용).
        Map<String, Long> postingCounts = hiddenCounts(notes.getContent().stream()
                .map(CompanyNote::getCompanyNameNormalized).toList());
        List<CompanyNoteResponse> items = notes.getContent().stream()
                .map(n -> toResponse(n, blocked.getOrDefault(n.getCompanyNameNormalized(), false),
                        ratings.get(n.getCompanyNameNormalized()),
                        postingCounts.get(n.getCompanyNameNormalized())))
                .collect(java.util.ArrayList::new, java.util.ArrayList::add, java.util.ArrayList::addAll);
        long total = notes.getTotalElements();
        // 전체 탭: 메모가 없는 차단 회사도 차단 뱃지와 함께 추가한다 (차단이 바로 보이도록).
        // 차단은 메모 목록과 다른 테이블이므로 현재 페이지 꼬리에 추가한다 (사용자 규모에서 허용).
        if (includeBlockedOnly) {
            BlockedData join = blockedData(accountId, keyword);
            for (CompanyBlacklist b : join.entries()) {
                if (join.notes().containsKey(b.getCompanyNameNormalized())) {
                    continue;
                }
                items.add(toResponse(null, b.getCompanyNameNormalized(), b.getCompanyNameNormalized(), true,
                        join.ratings().get(b.getCompanyNameNormalized()),
                        join.hiddenCounts().get(b.getCompanyNameNormalized()),
                        b.getCreatedAt()));
                total++;
            }
        }
        return new PageImpl<>(items, notes.getPageable(), total);
    }

    private Page<CompanyNoteResponse> listBlocked(Long accountId, String keyword, Pageable pageable,
                                              String sort, String dir) {
        BlockedData join = blockedData(accountId, keyword);
        List<CompanyNoteResponse> items = join.entries().stream()
                .map(b -> {
                    CompanyNote n = join.notes().get(b.getCompanyNameNormalized());
                    String display = (n != null) ? n.getCompanyNameDisplay() : b.getCompanyNameNormalized();
                    String normalized = (n != null) ? n.getCompanyNameNormalized() : b.getCompanyNameNormalized();
                    // 메모가 없으면 업데이트 일시 대신 차단 일시 표시 (같은 테이블처럼 보이도록)
                    LocalDateTime updatedAt = (n != null) ? n.getUpdatedAt() : b.getCreatedAt();
                    return toResponse(n, display, normalized, true,
                            join.ratings().get(b.getCompanyNameNormalized()),
                            join.hiddenCounts().get(b.getCompanyNameNormalized()),
                            updatedAt);
                })
                .collect(java.util.ArrayList::new, java.util.ArrayList::add, java.util.ArrayList::addAll);
        sortBlocked(items, sort, dir);
        int total = items.size();
        int from = Math.min((int) pageable.getOffset(), total);
        int to = Math.min(from + pageable.getPageSize(), total);
        return new PageImpl<>(items.subList(from, to), pageable, total);
    }

    /** DB 정렬 키(display|stars|updated)를 Spring Sort 로 변환한다. */
    private Sort resolveSort(String sort, String dir) {
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return switch (sort == null ? "" : sort.toLowerCase()) {
            case "display" -> Sort.by(direction, "companyNameDisplay");
            case "stars" -> Sort.by(direction, "myStars");
            default -> Sort.by(Sort.Direction.DESC, "updatedAt");
        };
    }

    /** 차단 탭(메모리 페이징) 정렬. stars 는 null 을 항상 뒤로 보낸다. */
    private void sortBlocked(List<CompanyNoteResponse> items, String sort, String dir) {
        boolean asc = "asc".equalsIgnoreCase(dir);
        switch (sort == null ? "" : sort.toLowerCase()) {
            case "display" -> items.sort((a, b) -> asc
                    ? String.CASE_INSENSITIVE_ORDER.compare(a.companyNameDisplay(), b.companyNameDisplay())
                    : String.CASE_INSENSITIVE_ORDER.compare(b.companyNameDisplay(), a.companyNameDisplay()));
            case "stars" -> items.sort((a, b) -> {
                int av = a.myStars() == null ? -1 : a.myStars();
                int bv = b.myStars() == null ? -1 : b.myStars();
                return asc ? Integer.compare(av, bv) : Integer.compare(bv, av);
            });
            default -> {
                // 기본 차단 등록 역순 유지
            }
        }
    }

    /** 차단 목록 + 메모·평점·숨김수 조인에 필요한 데이터를 한 번에 모은다. */
    private record BlockedData(List<CompanyBlacklist> entries, Map<String, CompanyNote> notes,
                               Map<String, CompanyRating> ratings, Map<String, Long> hiddenCounts) {
    }

    private BlockedData blockedData(Long accountId, String keyword) {
        List<CompanyBlacklist> entries = blacklistRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
        if (keyword != null) {
            String lower = keyword.toLowerCase();
            entries = entries.stream()
                    .filter(b -> b.getCompanyNameNormalized().contains(lower))
                    .toList();
        }
        List<String> normalized = entries.stream().map(CompanyBlacklist::getCompanyNameNormalized).toList();
        Map<String, CompanyNote> noteMap = new HashMap<>();
        if (!normalized.isEmpty()) {
            for (CompanyNote n : noteRepository.findByAccountIdAndCompanyNameNormalizedIn(accountId, normalized)) {
                noteMap.put(n.getCompanyNameNormalized(), n);
            }
        }
        return new BlockedData(entries, noteMap, ratingsByNormalized(normalized), hiddenCounts(normalized));
    }

    /** 정규화 회사명별 저장 공고 수 (차단 키워드로 숨겨지는 공고 수). */
    private Map<String, Long> hiddenCounts(List<String> normalized) {
        Map<String, Long> counts = new HashMap<>();
        if (normalized.isEmpty()) {
            return counts;
        }
        for (Object[] row : jobPostingRepository.countByNormalizedCompanyIn(normalized)) {
            counts.put((String) row[0], ((Number) row[1]).longValue());
        }
        return counts;
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

    private CompanyNoteResponse toResponse(CompanyNote note, boolean blocked, CompanyRating rating,
                                           Long hiddenCount) {
        return toResponse(note, note.getCompanyNameDisplay(), note.getCompanyNameNormalized(), blocked, rating,
                hiddenCount, note.getUpdatedAt());
    }

    private CompanyNoteResponse toResponse(CompanyNote note, String display, String normalized,
                                           boolean blocked, CompanyRating rating, Long hiddenCount,
                                           LocalDateTime updatedAt) {
        return new CompanyNoteResponse(
                note != null ? note.getId() : null,
                display,
                normalized,
                note != null ? note.getMyStars() : null,
                note != null && Boolean.TRUE.equals(note.getIsBookmarked()),
                blocked,
                note != null && note.getNoteMd() != null && !note.getNoteMd().isBlank(),
                rating != null ? rating.getAverageScore() : null,
                rating != null ? rating.getJobplanetScore() : null,
                rating != null ? rating.getJobkoreaScore() : null,
                rating != null ? rating.getSaraminScore() : null,
                updatedAt,
                hiddenCount,
                // 목록은 N+1 회피를 위해 태그 미포함 (상세 조회에서 제공)
                List.of());
    }

    private CompanyNoteDetailResponse toDetail(CompanyNote note, Map<String, Boolean> blocked,
                                              Map<String, CompanyRating> ratings) {
        CompanyRating rating = ratings.get(note.getCompanyNameNormalized());
        List<NoteCategoryResponse> categories = note.getNoteReasons().stream()
                .map(r -> new NoteCategoryResponse(r.getId(), r.getName()))
                .toList();
        return new CompanyNoteDetailResponse(
                note.getId(),
                note.getCompanyNameDisplay(),
                note.getCompanyNameNormalized(),
                note.getMyStars(),
                Boolean.TRUE.equals(note.getIsBookmarked()),
                blocked.getOrDefault(note.getCompanyNameNormalized(), false),
                note.getNoteMd(),
                categories,
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
