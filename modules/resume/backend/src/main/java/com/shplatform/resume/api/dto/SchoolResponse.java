package com.shplatform.resume.api.dto;

/**
 * 학교 마스터 응답.
 *
 * @param id         학교 ID
 * @param name       학교명
 * @param schoolType 학교 유형 (고등학교/대학교/대학원)
 */
public record SchoolResponse(Long id, String name, String schoolType) {
}
