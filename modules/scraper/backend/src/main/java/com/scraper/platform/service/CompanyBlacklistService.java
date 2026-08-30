package com.scraper.platform.service;

import com.scraper.platform.model.BlockReason;
import com.scraper.platform.model.CompanyBlacklist;
import com.scraper.platform.repository.BlockReasonRepository;
import com.scraper.platform.repository.CompanyBlacklistRepository;
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
    private final BlockReasonRepository blockReasonRepository;
    private final BlockReasonService blockReasonService;

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
        var result = new java.util.ArrayList<BlockReason>();
        if (reasonIds != null) {
            result.addAll(blockReasonRepository.findAllById(reasonIds));
        }
        if (categoryNames != null) {
            for (String name : categoryNames) {
                if (name != null && !name.isBlank()) {
                    result.add(blockReasonService.ensureCategory(name));
                }
            }
        }
        return new java.util.ArrayList<>(result.stream()
                .distinct()
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .toList());
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

    /** JobPosting.normalize 와 동일 규칙 (소문자+공백제거). 법인 표기 변형도 흡수한다. */
    public static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase()
                .replaceAll("\\(주\\)|㈜|주식회사", "")
                .replaceAll("\\s+", "");
    }
}
