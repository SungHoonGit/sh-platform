package com.shplatform.auth.infrastructure.oauth2;

import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class CustomOAuth2User implements OAuth2User {

    private final OAuth2UserInfo userInfo;
    private final Long userId;
    private final String email;
    private final String role;
    private final Map<String, Object> attributes;

    public CustomOAuth2User(OAuth2UserInfo userInfo, Long userId,
                            String email, String role,
                            Map<String, Object> attributes) {
        this.userInfo = userInfo;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.attributes = attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return userInfo.getName();
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public OAuth2UserInfo getUserInfo() {
        return userInfo;
    }
}
