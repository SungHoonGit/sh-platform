package com.shplatform.resume.domain;

import com.shplatform.resume.api.dto.MajorResponse;
import com.shplatform.resume.api.dto.SchoolResponse;
import com.shplatform.resume.infrastructure.repository.MajorRepository;
import com.shplatform.resume.infrastructure.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 기준/마스터 데이터(학교·전공) 조회 구현.
 */
@Service
@RequiredArgsConstructor
public class ReferenceDataServiceImpl implements ReferenceDataService {

    private final SchoolRepository schoolRepository;
    private final MajorRepository majorRepository;

    @Override
    public List<SchoolResponse> searchSchools(String keyword, String schoolType) {
        String q = StringUtils.hasText(keyword) ? keyword.trim() : "";
        if (!StringUtils.hasText(q)) {
            return List.of();
        }
        var entities = StringUtils.hasText(schoolType)
                ? schoolRepository.findTop20ByNameContainingAndSchoolTypeOrderByNameAsc(q, schoolType.trim())
                : schoolRepository.findTop20ByNameContainingOrderByNameAsc(q);
        return entities.stream()
                .map(e -> new SchoolResponse(e.getId(), e.getName(), e.getSchoolType()))
                .toList();
    }

    @Override
    public List<MajorResponse> searchMajors(String keyword) {
        String q = StringUtils.hasText(keyword) ? keyword.trim() : "";
        if (!StringUtils.hasText(q)) {
            return List.of();
        }
        return majorRepository.findTop20ByNameContainingOrderByNameAsc(q).stream()
                .map(e -> new MajorResponse(e.getId(), e.getName()))
                .toList();
    }
}
