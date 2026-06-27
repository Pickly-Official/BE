package com.be.global.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final long TTL_SECONDS = 180;
    private static final Map<String, SavedAuthorizationRequest> REQUESTS = new ConcurrentHashMap<>();

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        cleanupExpiredRequests();
        String state = request.getParameter("state");
        if (!StringUtils.hasText(state)) {
            return null;
        }

        SavedAuthorizationRequest saved = REQUESTS.get(state);
        if (saved == null || saved.isExpired()) {
            REQUESTS.remove(state);
            return null;
        }
        return saved.authorizationRequest();
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            return;
        }

        cleanupExpiredRequests();
        REQUESTS.put(
                authorizationRequest.getState(),
                new SavedAuthorizationRequest(authorizationRequest, Instant.now().plusSeconds(TTL_SECONDS))
        );
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                HttpServletResponse response) {
        String state = request.getParameter("state");
        if (!StringUtils.hasText(state)) {
            return null;
        }
        SavedAuthorizationRequest saved = REQUESTS.remove(state);
        return saved == null || saved.isExpired() ? null : saved.authorizationRequest();
    }

    private void cleanupExpiredRequests() {
        REQUESTS.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private record SavedAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            Instant expiresAt
    ) {

        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
