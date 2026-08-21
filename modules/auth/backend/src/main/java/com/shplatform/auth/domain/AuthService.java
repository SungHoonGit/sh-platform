package com.shplatform.auth.domain;

import com.shplatform.auth.api.dto.*;

public interface AuthService {
    User signup(SignupRequest request);

    /**
     * (명령형) 이메일 인증 코드를 발송한다.
     *
     * @param email   수신자 이메일
     * @param purpose 인증 목적 (SIGNUP, CHANGE_EMAIL 등)
     * @throws BusinessException DUPLICATE_EMAIL - SIGNUP 목적인데 이미 가입된 이메일인 경우
     */
    void sendVerificationEmail(String email, String purpose);

    /**
     * (명령형) 발송된 인증 코드를 검증한다.
     *
     * @param email   수신자 이메일
     * @param code    6자리 인증 코드
     * @param purpose 인증 목적 (SIGNUP, CHANGE_EMAIL 등)
     * @throws BusinessException INVALID_CODE - 코드가 없거나 일치하지 않는 경우
     * @throws BusinessException CODE_EXPIRED - 이미 사용되었거나 만료된 경우
     */
    void verifyCode(String email, String code, String purpose);
    TokenResponse login(LoginRequest request);
    TokenResponse refresh(String refreshToken);
    void logout(String refreshToken);
    User getUser(Long userId);
    User updateProfile(Long userId, UpdateProfileRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
    void setPassword(Long userId, SetPasswordRequest request);
    void deleteAccount(Long userId, String password);
}
