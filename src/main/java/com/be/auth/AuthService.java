package com.be.auth;

import com.be.auth.oauth.OAuthClient;
import com.be.auth.oauth.OAuthUserInfo;
import com.be.common.BusinessException;
import com.be.user.User;
import com.be.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final List<OAuthClient> oauthClients;

    @Transactional
    public Map<String, Object> login(String provider, String accessToken) {
        Map<String, OAuthClient> clientMap = oauthClients.stream()
                .collect(Collectors.toMap(OAuthClient::provider, Function.identity()));

        OAuthClient client = clientMap.get(provider);
        if (client == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "지원하지 않는 provider: " + provider);
        }

        OAuthUserInfo info;
        try {
            info = client.getUserInfo(accessToken);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "소셜 토큰 검증 실패: " + e.getMessage());
        }

        boolean[] isNew = {false};
        User user = userRepository.findByProviderAndProviderId(provider, info.providerId())
                .orElseGet(() -> {
                    isNew[0] = true;
                    return userRepository.save(User.of(provider, info.providerId(), info.nickname()));
                });

        String jwt = jwtProvider.generate(user.getId());
        return Map.of("token", jwt, "isNewUser", isNew[0]);
    }
}
