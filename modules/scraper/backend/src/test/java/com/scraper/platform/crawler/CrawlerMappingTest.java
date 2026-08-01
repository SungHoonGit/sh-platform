package com.scraper.platform.crawler;

import com.scraper.platform.service.SiteSearchMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("크롤러 매핑 함수 테스트")
class CrawlerMappingTest {

    private final SiteSearchMapper mockMapper = mock(SiteSearchMapper.class);

    @Nested
    @DisplayName("SaraminCrawler 매핑")
    class SaraminMapping {

        private final SaraminCrawler crawler = new SaraminCrawler(mockMapper);

        @Nested
        @DisplayName("mapCareerCode 메서드")
        class MapCareerCode {

            @Test
            @DisplayName("신입을 코드 1로 매핑한다")
            void 신입_매핑() {
                assertEquals("1", crawler.mapCareerCode("신입"));
            }

            @Test
            @DisplayName("경력을 코드 2로 매핑한다")
            void 경력_매핑() {
                assertEquals("2", crawler.mapCareerCode("경력"));
            }

            @Test
            @DisplayName("1~3년을 코드 3으로 매핑한다")
            void 경력_1_3년_매핑() {
                assertEquals("3", crawler.mapCareerCode("1~3년"));
            }

            @Test
            @DisplayName("3~5년을 코드 5로 매핑한다")
            void 경력_3_5년_매핑() {
                assertEquals("5", crawler.mapCareerCode("3~5년"));
            }

            @Test
            @DisplayName("5~10년을 코드 8로 매핑한다")
            void 경력_5_10년_매핑() {
                assertEquals("8", crawler.mapCareerCode("5~10년"));
            }

            @Test
            @DisplayName("10년이상을 코드 12로 매핑한다")
            void 경력_10년이상_매핑() {
                assertEquals("12", crawler.mapCareerCode("10년이상"));
            }

            @Test
            @DisplayName("알 수 없는 값은 빈 문자열을 반환한다")
            void 알수없는값_빈문자열() {
                assertEquals("", crawler.mapCareerCode("알수없음"));
            }
        }

        @Nested
        @DisplayName("mapLocationCode 메서드")
        class MapLocationCode {

            @Test
            @DisplayName("서울을 법정동코드 101000으로 매핑한다")
            void 서울_매핑() {
                assertEquals("101000", crawler.mapLocationCode("서울"));
            }

            @Test
            @DisplayName("경기를 법정동코드 102000으로 매핑한다")
            void 경기_매핑() {
                assertEquals("102000", crawler.mapLocationCode("경기"));
            }

            @Test
            @DisplayName("인천을 법정동코드 230000으로 매핑한다")
            void 인천_매핑() {
                assertEquals("230000", crawler.mapLocationCode("인천"));
            }

            @Test
            @DisplayName("부산을 법정동코드 260000으로 매핑한다")
            void 부산_매핑() {
                assertEquals("260000", crawler.mapLocationCode("부산"));
            }

            @Test
            @DisplayName("제주를 법정동코드 500000으로 매핑한다")
            void 제주_매핑() {
                assertEquals("500000", crawler.mapLocationCode("제주"));
            }

            @Test
            @DisplayName("16개 모든 지역이 매핑된다")
            void 전체지역_매핑() {
                assertAll(
                    () -> assertEquals("101000", crawler.mapLocationCode("서울")),
                    () -> assertEquals("102000", crawler.mapLocationCode("경기")),
                    () -> assertEquals("230000", crawler.mapLocationCode("인천")),
                    () -> assertEquals("260000", crawler.mapLocationCode("부산")),
                    () -> assertEquals("270000", crawler.mapLocationCode("대구")),
                    () -> assertEquals("300000", crawler.mapLocationCode("대전")),
                    () -> assertEquals("290000", crawler.mapLocationCode("광주")),
                    () -> assertEquals("360000", crawler.mapLocationCode("세종")),
                    () -> assertEquals("420000", crawler.mapLocationCode("강원")),
                    () -> assertEquals("500000", crawler.mapLocationCode("제주")),
                    () -> assertEquals("440000", crawler.mapLocationCode("충남")),
                    () -> assertEquals("430000", crawler.mapLocationCode("충북")),
                    () -> assertEquals("460000", crawler.mapLocationCode("전남")),
                    () -> assertEquals("450000", crawler.mapLocationCode("전북")),
                    () -> assertEquals("480000", crawler.mapLocationCode("경남")),
                    () -> assertEquals("470000", crawler.mapLocationCode("경북"))
                );
            }

            @Test
            @DisplayName("알 수 없는 지역은 빈 문자열을 반환한다")
            void 알수없는지역_빈문자열() {
                assertEquals("", crawler.mapLocationCode("알수없는지역"));
            }

            @Test
            @DisplayName("복수 지역은 빈 문자열을 반환한다 (단일 loc_mcd만 지원)")
            void 복수지역_빈문자열() {
                assertEquals("", crawler.mapLocationCode("서울,경기"));
            }
        }
    }

    @Nested
    @DisplayName("JobkoreaCrawler 매핑")
    class JobkoreaMapping {

        private final JobkoreaCrawler crawler = new JobkoreaCrawler(mockMapper, mock(JobkoreaAreaMapper.class));

        @Nested
        @DisplayName("mapCareerType 메서드")
        class MapCareerType {

            @Test
            @DisplayName("신입을 코드 1로 매핑한다")
            void 신입_매핑() {
                assertEquals("1", crawler.mapCareerType("신입"));
            }

            @Test
            @DisplayName("경력을 코드 2로 매핑한다")
            void 경력_매핑() {
                assertEquals("2", crawler.mapCareerType("경력"));
            }

            @Test
            @DisplayName("1~3년을 코드 2로 매핑한다")
            void 경력_1_3년_매핑() {
                assertEquals("2", crawler.mapCareerType("1~3년"));
            }

            @Test
            @DisplayName("3~5년을 코드 2로 매핑한다")
            void 경력_3_5년_매핑() {
                assertEquals("2", crawler.mapCareerType("3~5년"));
            }

            @Test
            @DisplayName("5~10년을 코드 2로 매핑한다")
            void 경력_5_10년_매핑() {
                assertEquals("2", crawler.mapCareerType("5~10년"));
            }

            @Test
            @DisplayName("10년이상을 코드 2로 매핑한다")
            void 경력_10년이상_매핑() {
                assertEquals("2", crawler.mapCareerType("10년이상"));
            }

            @Test
            @DisplayName("알 수 없는 값은 빈 문자열을 반환한다")
            void 알수없는값_빈문자열() {
                assertEquals("", crawler.mapCareerType("알수없음"));
            }
        }

        @Nested
        @DisplayName("mapLocationCode 메서드")
        class MapLocationCode {

            @Test
            @DisplayName("서울을 I000으로 매핑한다")
            void 서울_매핑() {
                assertEquals("I000", crawler.mapLocationCode("서울"));
            }

            @Test
            @DisplayName("경기를 B000으로 매핑한다")
            void 경기_매핑() {
                assertEquals("B000", crawler.mapLocationCode("경기"));
            }

            @Test
            @DisplayName("인천을 K000으로 매핑한다")
            void 인천_매핑() {
                assertEquals("K000", crawler.mapLocationCode("인천"));
            }

            @Test
            @DisplayName("부산을 H000으로 매핑한다")
            void 부산_매핑() {
                assertEquals("H000", crawler.mapLocationCode("부산"));
            }

            @Test
            @DisplayName("제주를 N000으로 매핑한다")
            void 제주_매핑() {
                assertEquals("N000", crawler.mapLocationCode("제주"));
            }

            @Test
            @DisplayName("17개 모든 지역이 매핑된다")
            void 전체지역_매핑() {
                assertAll(
                    () -> assertEquals("I000", crawler.mapLocationCode("서울")),
                    () -> assertEquals("B000", crawler.mapLocationCode("경기")),
                    () -> assertEquals("K000", crawler.mapLocationCode("인천")),
                    () -> assertEquals("H000", crawler.mapLocationCode("부산")),
                    () -> assertEquals("F000", crawler.mapLocationCode("대구")),
                    () -> assertEquals("G000", crawler.mapLocationCode("대전")),
                    () -> assertEquals("L000", crawler.mapLocationCode("광주")),
                    () -> assertEquals("L000", crawler.mapLocationCode("전남")),
                    () -> assertEquals("1000", crawler.mapLocationCode("세종")),
                    () -> assertEquals("A000", crawler.mapLocationCode("강원")),
                    () -> assertEquals("N000", crawler.mapLocationCode("제주")),
                    () -> assertEquals("O000", crawler.mapLocationCode("충남")),
                    () -> assertEquals("P000", crawler.mapLocationCode("충북")),
                    () -> assertEquals("M000", crawler.mapLocationCode("전북")),
                    () -> assertEquals("C000", crawler.mapLocationCode("경남")),
                    () -> assertEquals("D000", crawler.mapLocationCode("경북")),
                    () -> assertEquals("J000", crawler.mapLocationCode("울산"))
                );
            }

            @Test
            @DisplayName("알 수 없는 지역은 빈 문자열을 반환한다")
            void 알수없는지역_빈문자열() {
                assertEquals("", crawler.mapLocationCode("알수없는지역"));
            }
        }

        @Nested
        @DisplayName("careerText 메서드")
        class CareerText {

            @Test
            @DisplayName("신입(careerType 1)을 신입으로 변환한다")
            void 신입_변환() {
                assertEquals("신입", JobkoreaCrawler.careerText("1", 0));
            }

            @Test
            @DisplayName("경력(careerType 2) 연수 미지정은 경력으로 변환한다")
            void 경력_미지정_변환() {
                assertEquals("경력", JobkoreaCrawler.careerText("2", 100));
            }

            @Test
            @DisplayName("경력(careerType 2) 연수 지정은 경력N년↑으로 변환한다")
            void 경력_연수_변환() {
                assertEquals("경력3년↑", JobkoreaCrawler.careerText("2", 3));
            }

            @Test
            @DisplayName("신입·경력(careerType 3) 연수 미지정은 신입·경력으로 변환한다")
            void 신입경력_미지정_변환() {
                assertEquals("신입·경력", JobkoreaCrawler.careerText("3", 0));
            }

            @Test
            @DisplayName("신입·경력(careerType 3) 연수 지정은 신입·경력N년↑으로 변환한다")
            void 신입경력_연수_변환() {
                assertEquals("신입·경력4년↑", JobkoreaCrawler.careerText("3", 4));
            }

            @Test
            @DisplayName("경력무관(careerType 4)은 경력무관으로 변환한다")
            void 경력무관_변환() {
                assertEquals("경력무관", JobkoreaCrawler.careerText("4", 0));
            }

            @Test
            @DisplayName("알 수 없는 careerType은 경력무관으로 변환한다")
            void 알수없는타입_변환() {
                assertEquals("경력무관", JobkoreaCrawler.careerText("5", 0));
            }
        }
    }

    @Nested
    @DisplayName("WantedCrawler 매핑")
    class WantedMapping {

        private final WantedCrawler crawler = new WantedCrawler(mockMapper);

        @Nested
        @DisplayName("mapCareerToYears 메서드")
        class MapCareerToYears {

            @Test
            @DisplayName("신입을 0으로 매핑한다")
            void 신입_매핑() {
                assertEquals("0", crawler.mapCareerToYears("신입"));
            }

            @Test
            @DisplayName("1~3년을 1로 매핑한다")
            void 경력_1_3년_매핑() {
                assertEquals("1", crawler.mapCareerToYears("1~3년"));
            }

            @Test
            @DisplayName("3~5년을 3으로 매핑한다")
            void 경력_3_5년_매핑() {
                assertEquals("3", crawler.mapCareerToYears("3~5년"));
            }

            @Test
            @DisplayName("5~10년을 5로 매핑한다")
            void 경력_5_10년_매핑() {
                assertEquals("5", crawler.mapCareerToYears("5~10년"));
            }

            @Test
            @DisplayName("10년이상을 10으로 매핑한다")
            void 경력_10년이상_매핑() {
                assertEquals("10", crawler.mapCareerToYears("10년이상"));
            }

            @Test
            @DisplayName("알 수 없는 값은 빈 문자열을 반환한다")
            void 알수없는값_빈문자열() {
                assertEquals("", crawler.mapCareerToYears("알수없음"));
            }
        }

        @Nested
        @DisplayName("mapLocationCode 메서드")
        class MapLocationCode {

            @Test
            @DisplayName("서울을 seoul로 매핑한다")
            void 서울_매핑() {
                assertEquals("seoul", crawler.mapLocationCode("서울"));
            }

            @Test
            @DisplayName("경기를 gyeonggi로 매핑한다")
            void 경기_매핑() {
                assertEquals("gyeonggi", crawler.mapLocationCode("경기"));
            }

            @Test
            @DisplayName("인천을 incheon로 매핑한다")
            void 인천_매핑() {
                assertEquals("incheon", crawler.mapLocationCode("인천"));
            }

            @Test
            @DisplayName("부산을 busan로 매핑한다")
            void 부산_매핑() {
                assertEquals("busan", crawler.mapLocationCode("부산"));
            }

            @Test
            @DisplayName("제주를 jeju로 매핑한다")
            void 제주_매핑() {
                assertEquals("jeju", crawler.mapLocationCode("제주"));
            }

            @Test
            @DisplayName("알 수 없는 지역은 빈 문자열을 반환한다")
            void 알수없는지역_빈문자열() {
                assertEquals("", crawler.mapLocationCode("알수없는지역"));
            }
        }
    }

    @Nested
    @DisplayName("RememberCrawler 매핑")
    class RememberMapping {

        private final RememberCrawler crawler = new RememberCrawler(mockMapper);

        @Nested
        @DisplayName("mapCareerToExperience 메서드")
        class MapCareerToExperience {

            @Test
            @DisplayName("신입을 0으로 매핑한다")
            void 신입_매핑() {
                assertEquals("0", crawler.mapCareerToExperience("신입"));
            }

            @Test
            @DisplayName("1~3년을 1로 매핑한다")
            void 경력_1_3년_매핑() {
                assertEquals("1", crawler.mapCareerToExperience("1~3년"));
            }

            @Test
            @DisplayName("3~5년을 3으로 매핑한다")
            void 경력_3_5년_매핑() {
                assertEquals("3", crawler.mapCareerToExperience("3~5년"));
            }

            @Test
            @DisplayName("5~10년을 5로 매핑한다")
            void 경력_5_10년_매핑() {
                assertEquals("5", crawler.mapCareerToExperience("5~10년"));
            }

            @Test
            @DisplayName("10년이상을 10으로 매핑한다")
            void 경력_10년이상_매핑() {
                assertEquals("10", crawler.mapCareerToExperience("10년이상"));
            }

            @Test
            @DisplayName("알 수 없는 값은 빈 문자열을 반환한다")
            void 알수없는값_빈문자열() {
                assertEquals("", crawler.mapCareerToExperience("알수없음"));
            }
        }
    }
}
