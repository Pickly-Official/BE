package com.be.auth.oauth;

public interface OAuthClient {
    String provider();
    OAuthUserInfo getUserInfo(String accessToken);
}
