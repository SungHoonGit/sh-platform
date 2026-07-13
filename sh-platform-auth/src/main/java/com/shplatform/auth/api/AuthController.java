package com.shplatform.auth.api;

import com.shplatform.auth.api.dto.*;
import com.shplatform.auth.domain.AccountLinkService;
import com.shplatform.auth.domain.AuthService;
import com.shplatform.auth.domain.User;
import com.shplatform.auth.infrastructure.TokenProvider;
import com.shplatform.auth.infrastructure.oauth2.CustomOAuth2User;
import com.shplatform.shared.dto.ApiResponse;
import com.shplatform.shared.exception.BusinessException;
import com.shplatform.shared.exception.ErrorCode;
import jakarta.validation.Valid;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AccountLinkService accountLinkService;

    public AuthController(AuthService authService, AccountLinkService accountLinkService) {
        this.authService = authService;
        this.accountLinkService = accountLinkService;
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

    @PostMapping("/oauth2/link-check")
    public ResponseEntity<ApiResponse<LinkCheckResponse>> checkLink(
            @RequestBody LinkCheckRequest request,
            @AuthenticationPrincipal OAuth2User principal
    ) {
        Long userId = ((CustomOAuth2User) principal).getUserId();
        List<String> linked = accountLinkService.getLinkedProviders(userId);
        boolean alreadyLinked = linked.contains(request.provider());
        return ResponseEntity.ok(ApiResponse.success(new LinkCheckResponse(alreadyLinked, linked)));
    }

    @PostMapping("/oauth2/link")
    public ResponseEntity<ApiResponse<Void>> linkProvider(
            @RequestBody ProviderLinkRequest request,
            @AuthenticationPrincipal OAuth2User principal
    ) {
        Long userId = ((CustomOAuth2User) principal).getUserId();
        accountLinkService.linkProvider(userId, request.provider(), request.providerId(), request.providerEmail());
        return ResponseEntity.ok(ApiResponse.success("계정이 연결되었습니다.", null));
    }

    @GetMapping("/oauth2/providers")
    public ResponseEntity<ApiResponse<ProviderListResponse>> listProviders(
            @AuthenticationPrincipal OAuth2User principal
    ) {
        Long userId = ((CustomOAuth2User) principal).getUserId();
        List<String> providers = accountLinkService.getLinkedProviders(userId);
        var providerInfos = providers.stream()
                .map(p -> new ProviderListResponse.ProviderInfo(p, null))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(new ProviderListResponse(providerInfos)));
    }

    @DeleteMapping("/oauth2/providers/{provider}")
    public ResponseEntity<ApiResponse<Void>> unlinkProvider(
            @PathVariable String provider,
            @AuthenticationPrincipal OAuth2User principal
    ) {
        Long userId = ((CustomOAuth2User) principal).getUserId();
        accountLinkService.unlinkProvider(userId, provider);
        return ResponseEntity.ok(ApiResponse.success("프로바이더 연결이 해제되었습니다.", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @AuthenticationPrincipal Object principal
    ) {
        var user = authService.getUser(getCurrentUserId(principal));
        var response = toUserResponse(user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal Object principal
    ) {
        var user = authService.updateProfile(getCurrentUserId(principal), request);
        var response = toUserResponse(user);
        return ResponseEntity.ok(ApiResponse.success("프로필이 수정되었습니다.", response));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal Object principal
    ) {
        authService.changePassword(getCurrentUserId(principal), request);
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다.", null));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal Object principal
    ) {
        String password = body != null ? body.get("password") : null;
        authService.deleteAccount(getCurrentUserId(principal), password);
        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다.", null));
    }

    private Long getCurrentUserId(Object principal) {
        if (principal instanceof TokenProvider.Claims claims) {
            return claims.userId();
        }
        if (principal instanceof CustomOAuth2User oauth2User) {
            return oauth2User.getUserId();
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    private UserResponse toUserResponse(User user) {
        var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return new UserResponse(
                user.id(),
                user.email(),
                user.name(),
                user.role().name(),
                user.provider(),
                user.emailVerified(),
                user.locale(),
                user.createdAt() != null ? user.createdAt().format(dtf) : null,
                user.updatedAt() != null ? user.updatedAt().format(dtf) : null
        );
    }
}
