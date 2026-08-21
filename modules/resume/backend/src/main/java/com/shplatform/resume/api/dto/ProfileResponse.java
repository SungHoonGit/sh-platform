package com.shplatform.resume.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 인적사항 응답.
 *
 * @param id        인적사항 ID
 * @param name      이름
 * @param email     이메일
 * @param phone     전화번호
 * @param address   주소
 * @param birthDate 생년월일
 * @param photoUrl  프로필 사진 URL
 * @param headline  한 줄 소개
 * @param createdAt 등록 시각
 * @param updatedAt 수정 시각
 */
public record ProfileResponse(
        Long id,
        String name,
        String email,
        String phone,
        String address,
        LocalDate birthDate,
        String photoUrl,
        String headline,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
