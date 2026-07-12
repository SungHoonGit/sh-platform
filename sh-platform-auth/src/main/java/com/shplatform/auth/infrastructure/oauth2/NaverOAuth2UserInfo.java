package com.shplatform.auth.infrastructure.oauth2;

import java.util.Map;

public class NaverOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() {
        return "NAVER";
    }

    @Override
    public String getProviderId() {
        return String.valueOf(getResponse().get("id"));
    }

    @Override
    public String getEmail() {
        Object email = getResponse().get("email");
        return email != null ? email.toString() : null;
    }

    @Override
    public String getName() {
        Object name = getResponse().get("name");
        return name != null ? name.toString() : null;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getResponse() {
        return (Map<String, Object>) attributes.get("response");
    }
}
