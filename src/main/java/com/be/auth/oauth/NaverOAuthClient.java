package com.be.auth.oauth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NaverOAuthClient implements OAuthClient {

    private final RestTemplate restTemplate;

    @Value("${oauth.naver.userinfo-url}")
    private String userinfoUrl;

    @Override
    public String provider() { return "naver"; }

    @Override
    @SuppressWarnings("unchecked")
    public OAuthUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<Map> resp = restTemplate.exchange(
                userinfoUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<String, Object> response = (Map<String, Object>) resp.getBody().get("response");
        String providerId = (String) response.get("id");
        String nickname = response.containsKey("nickname")
                ? (String) response.get("nickname")
                : (String) response.get("name");
        return new OAuthUserInfo(providerId, nickname);
    }
}
