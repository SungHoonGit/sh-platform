package com.shplatform.auth.domain;

import com.shplatform.auth.api.dto.*;

public interface AuthService {
    User signup(SignupRequest request);
    void sendVerificationEmail(String email, String purpose);
    void verifyCode(String email, String code, String purpose);
    TokenResponse login(LoginRequest request);
    TokenResponse refresh(String refreshToken);
    void logout(String refreshToken);
    User getUser(Long userId);
    User updateProfile(Long userId, UpdateProfileRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
    void deleteAccount(Long userId, String password);
}
