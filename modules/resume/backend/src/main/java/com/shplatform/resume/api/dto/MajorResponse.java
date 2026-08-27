package com.shplatform.resume.api.dto;

/**
 * 전공 마스터 응답.
 *
 * @param id   전공 ID
 * @param name 전공명
 */
public record MajorResponse(Long id, String name) {
}
