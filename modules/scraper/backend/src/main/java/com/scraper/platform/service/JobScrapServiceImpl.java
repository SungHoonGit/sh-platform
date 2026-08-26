package com.scraper.platform.service;

import com.scraper.platform.api.dto.JobScrapResponse;
import com.scraper.platform.model.JobPosting;
import com.scraper.platform.model.JobScrap;
import com.scraper.platform.repository.JobPostingRepository;
import com.scraper.platform.repository.JobScrapRepository;
import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobScrapServiceImpl implements JobScrapService {

    private final JobScrapRepository jobScrapRepository;
    private final JobPostingRepository jobPostingRepository;

    @Override
    @Transactional
    public void scrap(Long userId, Long postingId) {
        if (jobScrapRepository.existsByUserIdAndPostingId(userId, postingId)) {
            return;
        }
        JobPosting posting = jobPostingRepository.findById(postingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        jobScrapRepository.save(JobScrap.builder()
                .userId(userId)
                .postingId(posting.getId())
                .build());
    }

    @Override
    @Transactional
    public Long scrapLive(Long userId, com.scraper.platform.api.dto.LiveScrapRequest request) {
        String dedupKey = JobPosting.generateDedupKey(
                request.company(), request.position(), request.location(), request.siteName());
        java.time.LocalDate today = java.time.LocalDate.now();

        JobPosting posting = jobPostingRepository.findByDedupKeyAndCrawledAt(dedupKey, today)
                .orElseGet(() -> jobPostingRepository.save(JobPosting.builder()
                        .siteName(request.siteName())
                        .url(request.url())
                        .company(request.company())
                        .position(request.position())
                        .career(request.career())
                        .tech(request.tech())
                        .location(request.location())
                        .deadline(request.deadline())
                        .dedupKey(dedupKey)
                        .crawledAt(today)
                        .savedByAccountId(userId)
                        .build()));

        if (jobScrapRepository.existsByUserIdAndPostingId(userId, posting.getId())) {
            return posting.getId();
        }
        jobScrapRepository.save(JobScrap.builder()
                .userId(userId)
                .postingId(posting.getId())
                .build());
        return posting.getId();
    }

    @Override
    @Transactional
    public void unscrap(Long userId, Long postingId) {
        jobScrapRepository.findByUserIdAndPostingId(userId, postingId)
                .ifPresent(jobScrapRepository::delete);
    }

    @Override
    public List<JobScrapResponse> getMyScraps(Long userId) {
        List<Long> postingIds = jobScrapRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(JobScrap::getPostingId)
                .toList();
        var postingsById = jobPostingRepository.findAllById(postingIds).stream()
                .collect(java.util.stream.Collectors.toMap(JobPosting::getId, p -> p));
        return jobScrapRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(scrap -> toResponse(scrap, postingsById.get(scrap.getPostingId())))
                .toList();
    }

    @Override
    public boolean isScrapped(Long userId, Long postingId) {
        return jobScrapRepository.existsByUserIdAndPostingId(userId, postingId);
    }

    private JobScrapResponse toResponse(JobScrap scrap, JobPosting posting) {
        if (posting == null) {
            return new JobScrapResponse(scrap.getId(), scrap.getPostingId(),
                    null, "(삭제된 공고)", null, null, null, null, null, null,
                    scrap.getCreatedAt());
        }
        return new JobScrapResponse(scrap.getId(), posting.getId(),
                posting.getSiteName(), posting.getCompany(), posting.getPosition(),
                posting.getUrl(), posting.getCareer(), posting.getTech(),
                posting.getLocation(), posting.getDeadline(), scrap.getCreatedAt());
    }
}
