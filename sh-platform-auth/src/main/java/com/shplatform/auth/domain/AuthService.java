package com.shplatform.auth.domain;

import com.shplatform.auth.api.dto.LoginRequest;
import com.shplatform.auth.api.dto.SignupRequest;
import com.shplatform.auth.api.dto.TokenResponse;

public interface AuthService {
    User signup(SignupRequest request);
    void sendVerificationEmail(String email, String purpose);
    void verifyCode(String email, String code, String purpose);
    TokenResponse login(LoginRequest request);
    TokenResponse refresh(String refreshToken);
    void logout(String refreshToken);
}
