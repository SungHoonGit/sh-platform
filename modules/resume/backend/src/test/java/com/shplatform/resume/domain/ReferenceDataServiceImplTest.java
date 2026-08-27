package com.shplatform.resume.domain;

import com.shplatform.resume.infrastructure.entity.MajorEntity;
import com.shplatform.resume.infrastructure.entity.SchoolEntity;
import com.shplatform.resume.infrastructure.repository.MajorRepository;
import com.shplatform.resume.infrastructure.repository.SchoolRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReferenceDataServiceImplTest {

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private MajorRepository majorRepository;

    @InjectMocks
    private ReferenceDataServiceImpl referenceDataService;

    @Test
    @DisplayName("학교 검색: 유형 필터 없이 이름으로 검색한다")
    void searchSchools_withoutType() {
        var school = SchoolEntity.of("서울대학교", "대학교");
        given(schoolRepository.findTop20ByNameContainingOrderByNameAsc("서울"))
                .willReturn(List.of(school));

        var result = referenceDataService.searchSchools("서울", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("서울대학교");
        assertThat(result.get(0).schoolType()).isEqualTo("대학교");
    }

    @Test
    @DisplayName("학교 검색: 유형 필터와 함께 검색한다")
    void searchSchools_withType() {
        var school = SchoolEntity.of("서울대학교 대학원", "대학원");
        given(schoolRepository.findTop20ByNameContainingAndSchoolTypeOrderByNameAsc("서울", "대학원"))
                .willReturn(List.of(school));

        var result = referenceDataService.searchSchools("서울", "대학원");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).schoolType()).isEqualTo("대학원");
    }

    @Test
    @DisplayName("학교 검색: 검색어가 없으면 빈 목록을 반환한다")
    void searchSchools_emptyKeyword() {
        var result = referenceDataService.searchSchools("   ", null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("전공 검색: 이름으로 검색한다")
    void searchMajors() {
        var major = MajorEntity.of("컴퓨터공학");
        given(majorRepository.findTop20ByNameContainingOrderByNameAsc("컴퓨터"))
                .willReturn(List.of(major));

        var result = referenceDataService.searchMajors("컴퓨터");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("컴퓨터공학");
    }

    @Test
    @DisplayName("전공 검색: 검색어가 없으면 빈 목록을 반환한다")
    void searchMajors_emptyKeyword() {
        var result = referenceDataService.searchMajors("  ");

        assertThat(result).isEmpty();
    }
}
