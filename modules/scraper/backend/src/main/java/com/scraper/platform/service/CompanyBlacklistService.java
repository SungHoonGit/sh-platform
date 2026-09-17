package com.scraper.platform.service;

import com.scraper.platform.model.BlockReason;
import com.scraper.platform.model.CompanyBlacklist;
import com.scraper.platform.model.CompanyNote;
import com.scraper.platform.repository.CompanyBlacklistRepository;
import com.scraper.platform.repository.CompanyNoteRepository;
import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 개인 단위 회사 블랙리스트. 차단된 회사의 공고는 모든 목록에서 숨겨진다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyBlacklistService {

    private final CompanyBlacklistRepository repository;
    private final BlockReasonService blockReasonService;
    private final CompanyNoteRepository noteRepository;

    public List<CompanyBlacklist> list(Long accountId) {
        return repository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    /** 회사명을 정규화해 등록한다. 중복이면 카테고리를 갱신한다(멱등). */
    @Transactional
    public CompanyBlacklist add(Long accountId, String companyNameRaw, String reason,
                                List<Long> reasonIds, List<String> categoryNames) {
        String normalized = normalize(companyNameRaw);
        var categories = resolveCategories(reasonIds, categoryNames);
        var existing = repository.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
                .filter(b -> b.getCompanyNameNormalized().equals(normalized))
                .findFirst();
        if (existing.isPresent()) {
            var b = existing.get();
            b.setReason(reason != null && !reason.isBlank() ? reason : null);
            b.setBlockReasons(new java.util.ArrayList<>(categories));
            return repository.save(b);
        }
        return repository.save(CompanyBlacklist.builder()
                .accountId(accountId)
                .companyNameNormalized(normalized)
                .reason(reason != null && !reason.isBlank() ? reason : null)
                .blockReasons(new java.util.ArrayList<>(categories))
                .build());
    }

    /** 선택 id + 사용자 신규 입력 카테고리를 합쳐 정렬순 BlockReason 목록으로 변환한다. */
    private List<BlockReason> resolveCategories(List<Long> reasonIds, List<String> categoryNames) {
        return blockReasonService.resolveCategories(reasonIds, categoryNames);
    }

    /** 기존 차단 항목의 카테고리를 교체한다(자유 메모는 보존). 본인 항목이 아니면 무시한다. */
    @Transactional
    public CompanyBlacklist update(Long accountId, Long id, List<Long> reasonIds, List<String> categoryNames) {
        return update(accountId, id, null, null, reasonIds, categoryNames);
    }

    /**
     * (명령형) 차단 항목을 수정한다. 회사명(차단 키워드) 변경 시 연결된 회사 메모의
     * 정규화 키도 함께 이관한다. 본인 항목이 아니면 null을 반환한다.
     *
     * @param accountId 소유 계정
     * @param id 차단 항목 ID
     * @param companyName 새 회사명 (null/blank이면 키워드 유지)
     * @param reason 새 자유 메모 (null이면 유지, blank이면 삭제)
     * @param reasonIds 기존 카테고리 id 목록
     * @param categoryNames 신규 입력 카테고리명 목록
     * @return 수정된 항목, 본인 항목이 아니면 null
     * @throws BusinessException DUPLICATE_NAME 변경 후 키워드가 다른 내 차단/메모와 충돌
     */
    @Transactional
    public CompanyBlacklist update(Long accountId, Long id, String companyName, String reason,
                                   List<Long> reasonIds, List<String> categoryNames) {
        var categories = resolveCategories(reasonIds, categoryNames);
        return repository.findById(id)
                .filter(b -> b.getAccountId().equals(accountId))
                .map(b -> {
                    if (companyName != null && !companyName.isBlank()) {
                        rename(accountId, b, companyName.trim());
                    }
                    if (reason != null) {
                        b.setReason(reason.isBlank() ? null : reason);
                    }
                    b.setBlockReasons(new java.util.ArrayList<>(categories));
                    return repository.save(b);
                })
                .orElse(null);
    }

    /**
     * 차단 키워드를 변경하고 연결된 회사 메모를 함께 이관한다.
     * 변경 후 키워드가 다른 내 차단 항목이나 회사 메모와 충돌하면 병합 손실 방지를 위해 거부한다.
     */
    private void rename(Long accountId, CompanyBlacklist entry, String companyName) {
        String normalized = normalize(companyName);
        if (normalized.isEmpty() || normalized.equals(entry.getCompanyNameNormalized())) {
            return;
        }
        boolean clash = repository.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
                .anyMatch(b -> !b.getId().equals(entry.getId())
                        && b.getCompanyNameNormalized().equals(normalized))
                || noteRepository.findByAccountIdAndCompanyNameNormalized(accountId, normalized).isPresent();
        if (clash) {
            throw new BusinessException(ErrorCode.DUPLICATE_NAME);
        }
        String oldNormalized = entry.getCompanyNameNormalized();
        entry.setCompanyNameNormalized(normalized);
        noteRepository.findByAccountIdAndCompanyNameNormalized(accountId, oldNormalized)
                .ifPresent(note -> {
                    note.setCompanyNameNormalized(normalized);
                    note.setCompanyNameDisplay(companyName);
                    noteRepository.save(note);
                });
    }

    @Transactional
    public void remove(Long accountId, Long id) {
        repository.findById(id)
                .filter(b -> b.getAccountId().equals(accountId))
                .ifPresent(repository::delete);
    }

    public Set<String> normalizedNames(Long accountId) {
        return list(accountId).stream()
                .map(CompanyBlacklist::getCompanyNameNormalized)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * (질의형) 내 블랙리스트의 카테고리별 차단 수를 집계한다 (데이터 마이닝).
     * 카테고리는 blacklist_block_reason 연결 테이블로 코드화되어 있으므로 같은 카테고리의
     * 선택 빈도를 그대로 집계한다. 카테고리 없는(레거시 자유 메모) 항목은 uncategorized로 집계한다.
     *
     * @param accountId 사용자 PK
     * @return 전체 차단 수, 카테고리 없는 수, 카테고리별 사용 빈도(내림차순)
     */
    public BlockStatsResponse stats(Long accountId) {
        var items = repository.findByAccountIdOrderByCreatedAtDesc(accountId);
        var countByName = new java.util.HashMap<String, Integer>();
        var reasonInfo = new java.util.HashMap<String, BlockStatsResponse.CategoryStat>();
        long uncategorized = 0;
        for (var item : items) {
            if (item.getBlockReasons().isEmpty()) {
                uncategorized++;
                continue;
            }
            for (var reason : item.getBlockReasons()) {
                countByName.merge(reason.getName(), 1, Integer::sum);
                reasonInfo.putIfAbsent(reason.getName(),
                        new BlockStatsResponse.CategoryStat(reason.getId(), reason.getName(), reason.getCategory(), 0));
            }
        }
        var categories = reasonInfo.values().stream()
                .map(stat -> new BlockStatsResponse.CategoryStat(
                        stat.id(), stat.name(), stat.category(), countByName.getOrDefault(stat.name(), 0)))
                .sorted(java.util.Comparator.comparingLong(BlockStatsResponse.CategoryStat::count).reversed()
                        .thenComparing(BlockStatsResponse.CategoryStat::name))
                .toList();
        return new BlockStatsResponse(items.size(), uncategorized, categories);
    }

    /** 블랙리스트 통계 응답. categories = 블랙리스트에서 실제 사용된 카테고리 빈도. */
    public record BlockStatsResponse(long total, long uncategorized, List<CategoryStat> categories) {
        /** 단일 카테고리 사용 빈도. category = company_type / reason / user. */
        public record CategoryStat(Long id, String name, String category, long count) {}
    }

    /** JobPosting.normalize 와 동일 규칙 (소문자+공백제거). 법인 표기 변형도 흡수한다. */
    public static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase()
                .replaceAll("\\(주\\)|㈜|주식회사", "")
                .replaceAll("\\s+", "");
    }
}
