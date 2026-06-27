package com.be.auth.oauth;

import org.springframework.stereotype.Component;

@Component
public class MockOAuthClient implements OAuthClient {

    @Override
    public String provider() {
        return "mock";
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        String nickname = accessToken.length() > 20 ? accessToken.substring(0, 20) : accessToken;
        return new OAuthUserInfo("mock_" + accessToken, nickname);
    }
}
