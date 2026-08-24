package com.shplatform.resume.domain;

/**
 * 지원 진행 상태 코드.
 * DB에는 문자열로 저장되며 파이프라인 순서를 정의한다.
 */
public enum ApplicationStatus {

    PREPARING("준비 중"),
    APPLIED("지원 완료"),
    SCREEN_PASSED("서류 통과"),
    INTERVIEW("면접"),
    OFFER("오퍼"),
    REJECTED("불합격");

    private final String label;

    ApplicationStatus(String label) {
        this.label = label;
    }

    /** 한글 라벨을 반환한다. */
    public String label() {
        return label;
    }

    /**
     * 문자열 코드를 안전하게 변환한다.
     *
     * @param code 상태 코드 (null 허용 → 기본값 PREPARING)
     * @return 유효한 코드면 해당 값, 아니면 PREPARING
     */
    public static ApplicationStatus fromCode(String code) {
        if (code == null) {
            return PREPARING;
        }
        try {
            return valueOf(code);
        } catch (IllegalArgumentException e) {
            return PREPARING;
        }
    }
}
