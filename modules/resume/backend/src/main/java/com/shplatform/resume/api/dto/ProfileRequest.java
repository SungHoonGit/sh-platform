package com.shplatform.resume.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 인적사항 등록/수정 요청.
 *
 * @param name      이름
 * @param email     이메일
 * @param phone     전화번호
 * @param address   주소
 * @param birthDate 생년월일
 * @param photoUrl  프로필 사진 URL
 * @param headline  한 줄 소개
 */
public record ProfileRequest(
        @Size(max = 50) String name,
        @Email @Size(max = 100) String email,
        @Size(max = 30) String phone,
        @Size(max = 200) String address,
        LocalDate birthDate,
        @Size(max = 300) String photoUrl,
        @Size(max = 100) String headline
) {
}
