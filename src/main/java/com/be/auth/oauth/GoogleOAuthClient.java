package com.be.auth.oauth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class GoogleOAuthClient implements OAuthClient {

    private final RestTemplate restTemplate;

    @Value("${oauth.google.userinfo-url}")
    private String userinfoUrl;

    @Override
    public String provider() { return "google"; }

    @Override
    @SuppressWarnings("unchecked")
    public OAuthUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<Map> resp = restTemplate.exchange(
                userinfoUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<String, Object> body = resp.getBody();
        String providerId = (String) body.get("sub");
        String nickname = (String) body.get("name");
        return new OAuthUserInfo(providerId, nickname);
    }
}
