package com.shplatform.resume.domain;

/**
 * 지원 경로 코드.
 */
public enum ApplyChannel {

    PLATFORM("플랫폼"),
    LINK("링크"),
    EMAIL("이메일"),
    ETC("기타");

    private final String label;

    ApplyChannel(String label) {
        this.label = label;
    }

    /** 한글 라벨을 반환한다. */
    public String label() {
        return label;
    }

    /**
     * 문자열 코드를 안전하게 변환한다.
     *
     * @param code 경로 코드 (null 허용 → 기본값 LINK)
     * @return 유효한 코드면 해당 값, 아니면 LINK
     */
    public static ApplyChannel fromCode(String code) {
        if (code == null) {
            return LINK;
        }
        try {
            return valueOf(code);
        } catch (IllegalArgumentException e) {
            return LINK;
        }
    }
}
