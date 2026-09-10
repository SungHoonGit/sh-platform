package com.shplatform.resume.domain;

import com.shplatform.resume.api.dto.SkillMatchResponse;
import com.shplatform.resume.infrastructure.entity.SkillMasterEntity;
import com.shplatform.resume.infrastructure.repository.ResumePortfolioItemRepository;
import com.shplatform.resume.infrastructure.repository.ResumeProjectRepository;
import com.shplatform.resume.infrastructure.repository.SkillMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 공고-이력서 기술 매칭 구현.
 * 기술 스택 마스터(name/aliases)를 기준으로 실사용 문자열을 정식명으로 정규화한 뒤
 * 공고와 이력서의 교집합을 계산한다. (점수 산식 없이 일치 목록·개수만 제공)
 */
@Service
@RequiredArgsConstructor
public class SkillMatchServiceImpl implements SkillMatchService {

    private static final String SEPARATOR = ",";

    private final SkillMasterRepository skillMasterRepository;
    private final ResumeProjectRepository projectRepository;
    private final ResumePortfolioItemRepository portfolioItemRepository;

    @Override
    public SkillMatchResponse match(Long userId, String techStack) {
        Map<String, String> nameByAlias = buildNameByAlias(skillMasterRepository
                .findByActiveTrueOrderByDisplayOrderAscNameAsc());

        List<String> requested = normalize(split(techStack), nameByAlias);
        List<String> mySkills = collectMySkills(userId, nameByAlias);

        Set<String> mySet = new HashSet<>(mySkills);
        List<String> matched = requested.stream().filter(mySet::contains).toList();

        return new SkillMatchResponse(
                matched,
                matched.size(),
                requested.size(),
                mySkills.size());
    }

    private List<String> collectMySkills(Long userId, Map<String, String> nameByAlias) {
        List<String> raw = new ArrayList<>();
        projectRepository.findByUserIdOrderByDisplayOrderAscIdAsc(userId)
                .forEach(p -> raw.addAll(split(p.getTechStack())));
        portfolioItemRepository.findByUserIdOrderByDisplayOrderAscIdAsc(userId)
                .forEach(p -> raw.addAll(split(p.getTechStack())));
        return normalize(raw, nameByAlias);
    }

    private List<String> split(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(SEPARATOR))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    /**
     * 마스터 name과 aliases(쉼표 구분)를 소문자 키로 정식명에 매핑한다.
     */
    private Map<String, String> buildNameByAlias(List<SkillMasterEntity> masters) {
        Map<String, String> map = new java.util.LinkedHashMap<>();
        for (SkillMasterEntity m : masters) {
            map.putIfAbsent(normKey(m.getName()), m.getName());
            if (StringUtils.hasText(m.getAliases())) {
                for (String alias : m.getAliases().split(SEPARATOR)) {
                    map.putIfAbsent(normKey(alias), m.getName());
                }
            }
        }
        return map;
    }

    /**
     * 입력 기술 목록을 정식명으로 정규화한다. 마스터에 없는 값은 원문을 보존한다.
     */
    private List<String> normalize(List<String> skills, Map<String, String> nameByAlias) {
        return skills.stream()
                .map(s -> nameByAlias.getOrDefault(normKey(s), s))
                .collect(Collectors.toList());
    }

    private String normKey(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}