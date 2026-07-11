package com.shplatform.auth.api;

import com.shplatform.auth.api.dto.LoginRequest;
import com.shplatform.auth.api.dto.RefreshRequest;
import com.shplatform.auth.api.dto.SignupRequest;
import com.shplatform.auth.api.dto.TokenResponse;
import com.shplatform.auth.domain.AuthService;
import com.shplatform.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Map<String, Object>>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        var user = authService.signup(request);
        Map<String, Object> data = Map.of(
                "id", user.id(),
                "email", user.email(),
                "name", user.name(),
                "role", user.role().name(),
                "provider", user.provider(),
                "emailVerified", user.emailVerified()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(data));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        var tokens = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", tokens));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshRequest request
    ) {
        var tokens = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(tokens));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody RefreshRequest request
    ) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("로그아웃 되었습니다.", null));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> sendVerificationEmail(
            @RequestBody Map<String, String> body
    ) {
        authService.sendVerificationEmail(body.get("email"), body.get("purpose"));
        return ResponseEntity.ok(ApiResponse.success("인증 메일이 발송되었습니다.", null));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse<Void>> verifyCode(
            @RequestBody Map<String, String> body
    ) {
        authService.verifyCode(body.get("email"), body.get("code"), body.get("purpose"));
        return ResponseEntity.ok(ApiResponse.success("이메일 인증이 완료되었습니다.", null));
    }
}
