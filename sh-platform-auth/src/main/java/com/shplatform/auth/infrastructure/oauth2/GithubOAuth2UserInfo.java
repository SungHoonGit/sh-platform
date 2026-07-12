package com.shplatform.auth.infrastructure.oauth2;

import java.util.Map;

public class GithubOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GithubOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() {
        return "GITHUB";
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        var name = (String) attributes.get("name");
        if (name == null || name.isBlank()) {
            return (String) attributes.get("login");
        }
        return name;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
