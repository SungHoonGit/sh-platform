package com.scraper.platform.ai;

import com.scraper.platform.model.CompanyNote;
import com.scraper.platform.model.JobPosting;
import com.scraper.platform.repository.CompanyBlacklistRepository;
import com.scraper.platform.repository.CompanyNoteRepository;
import com.scraper.platform.repository.CompanyRatingRepository;
import com.scraper.platform.repository.JobPostingRepository;
import com.scraper.platform.service.CompanyBlacklistService;
import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@EnableConfigurationProperties(LlmProperties.class)
@Transactional(readOnly = true)
public class CompanyAnalysisService {

    private static final int SAMPLE_SIZE = 12;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CompanyNoteRepository noteRepository;
    private final JobPostingRepository jobPostingRepository;
    private final CompanyRatingRepository ratingRepository;
    private final CompanyBlacklistRepository blacklistRepository;
    private final LlmProperties properties;
    private final List<LlmClient> clients;

    public CompanyAnalysisService(CompanyNoteRepository noteRepository,
                                  JobPostingRepository jobPostingRepository,
                                  CompanyRatingRepository ratingRepository,
                                  CompanyBlacklistRepository blacklistRepository,
                                  LlmProperties properties,
                                  List<LlmClient> clients) {
        this.noteRepository = noteRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.ratingRepository = ratingRepository;
        this.blacklistRepository = blacklistRepository;
        this.properties = properties;
        this.clients = clients;
    }

    public CompanyAnalysisResult analyze(Long accountId, Long id) {
        if (!properties.configured()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        CompanyNote note = noteRepository.findById(id)
                .filter(n -> n.getAccountId().equals(accountId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        String normalized = note.getCompanyNameNormalized();
        List<JobPosting> recent = jobPostingRepository.findRecentByNormalizedCompany(normalized, PageRequest.of(0, SAMPLE_SIZE));
        CompanyAnalysisResult.Stats stats = buildStats(recent);
        List<CompanyAnalysisResult.Citation> citations = recent.stream()
                .limit(8)
                .map(j -> new CompanyAnalysisResult.Citation(safe(j.getPosition()), safe(j.getSiteName()),
                        j.getCrawledAt() == null ? null : j.getCrawledAt().toString(), safe(j.getUrl())))
                .toList();

        String prompt = buildPrompt(note, stats, recent);
        LlmClient client = clients.stream()
                .filter(c -> c.supports(properties.provider()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        String markdown = sanitizeMarkdown(client.generate(prompt, properties), note.getCompanyNameDisplay(), stats, citations);
        return new CompanyAnalysisResult(
                note.getCompanyNameDisplay(),
                markdown,
                stats,
                citations,
                properties.provider(),
                properties.model(),
                LocalDateTime.now().format(TS)
        );
    }

    private CompanyAnalysisResult.Stats buildStats(List<JobPosting> recent) {
        String recentDate = recent.stream()
                .map(JobPosting::getCrawledAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(Object::toString)
                .orElse(null);
        Map<String, Long> siteCounts = recent.stream()
                .collect(Collectors.groupingBy(j -> safe(j.getSiteName(), "unknown"), LinkedHashMap::new, Collectors.counting()));
        return new CompanyAnalysisResult.Stats(recent.size(), recentDate, siteCounts);
    }

    private String buildPrompt(CompanyNote note, CompanyAnalysisResult.Stats stats, List<JobPosting> recent) {
        StringBuilder sb = new StringBuilder();
        sb.append("너는 채용공고 수집 데이터 기반 회사 분석 보조자다. 아래 DB 사실/표본만 사용해 한국어 마크다운 메모를 작성하라.\n");
        sb.append("규칙: 추측 금지. 근거가 없으면 '근거 없음'이라고 써라. 수치/날짜를 임의 생성하지 말라. 민감한 내부정보 단정 금지.\n");
        sb.append("출력 형식(마크다운): ## 요약 / ## 최근 채용 신호(DB 근거) / ## 관찰 포인트 / ## 확인 필요(근거 없음 포함 가능).\n\n");
        sb.append("회사명: ").append(note.getCompanyNameDisplay()).append("\n");
        sb.append("정규화명: ").append(note.getCompanyNameNormalized()).append("\n");
        sb.append("내 별점: ").append(note.getMyStars() == null ? "미지정" : note.getMyStars()).append("\n");
        sb.append("북마크: ").append(Boolean.TRUE.equals(note.getIsBookmarked()) ? "Y" : "N").append("\n");
        blacklistRepository.findByAccountIdAndCompanyNameNormalized(note.getAccountId(), note.getCompanyNameNormalized())
                .ifPresent(b -> sb.append("차단 여부: Y (사유: ").append(safe(b.getReason(), "-")).append(")\n"));
        ratingRepository.findByCompanyNameIn(List.of(note.getCompanyNameDisplay())).stream().findFirst().ifPresent(r ->
                sb.append("크롤링 평점: 평균=").append(r.getAverageScore()).append(", 잡플래닛=").append(r.getJobplanetScore())
                        .append(", 잡코리아=").append(r.getJobkoreaScore()).append(", 사람인=").append(r.getSaraminScore()).append("\n"));
        if (note.getNoteMd() != null && !note.getNoteMd().isBlank()) {
            sb.append("기존 사용자 메모(참고, 사실 아님):\n").append(truncate(note.getNoteMd(), 1200)).append("\n");
        }
        sb.append("\n[DB 집계 사실]\n");
        sb.append("- 최근 표본 공고 수: ").append(stats.totalCount()).append("\n");
        sb.append("- 표본 기준 최근 수집일: ").append(stats.recentCrawledAt() == null ? "근거 없음" : stats.recentCrawledAt()).append("\n");
        sb.append("- 사이트별 표본 건수: ").append(stats.siteCounts().isEmpty() ? "근거 없음" : stats.siteCounts()).append("\n");
        sb.append("\n[최근 공고 표본(인용 근거)]\n");
        if (recent.isEmpty()) {
            sb.append("- 근거 없음: 저장된 관련 공고 표본이 없습니다.\n");
        } else {
            int i = 1;
            for (JobPosting j : recent) {
                sb.append(i++).append(". [").append(safe(j.getSiteName())).append("] ")
                        .append(safe(j.getPosition())).append(" | 수집일=").append(j.getCrawledAt())
                        .append(" | URL=").append(safe(j.getUrl())).append("\n");
            }
        }
        sb.append("\n마지막에 '## 근거' 섹션을 추가하고 위 표본 번호/URL을 목록으로 포함하라. 표본이 없으면 근거 없음을 명시하라.");
        return sb.toString();
    }

    private String sanitizeMarkdown(String text, String companyName, CompanyAnalysisResult.Stats stats,
                                    List<CompanyAnalysisResult.Citation> citations) {
        String normalized = CompanyBlacklistService.normalize(companyName);
        String md = text == null ? "" : text.replace("\r\n", "\n").trim();
        if (md.length() > 12000) md = md.substring(0, 12000) + "\n\n_(truncated)_";
        if (!md.toLowerCase(Locale.ROOT).contains("근거")) {
            StringBuilder tail = new StringBuilder(md).append("\n\n## 근거\n");
            if (citations.isEmpty()) tail.append("- 근거 없음: 저장된 관련 공고 표본이 없습니다.\n");
            else citations.forEach(c -> tail.append("- [").append(c.siteName()).append("] ")
                    .append(c.position()).append(" (").append(c.crawledAt()).append(") ").append(c.url()).append("\n"));
            md = tail.toString();
        }
        return md;
    }

    private String safe(String v) { return safe(v, "-"); }
    private String safe(String v, String fallback) { return v == null || v.isBlank() ? fallback : v.trim(); }
    private String truncate(String v, int max) { return v == null ? "" : (v.length() <= max ? v : v.substring(0, max) + "..."); }
}
