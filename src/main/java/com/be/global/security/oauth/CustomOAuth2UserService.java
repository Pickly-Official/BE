package com.be.global.security.oauth;

import com.be.domain.user.entity.Provider;
import com.be.domain.user.entity.User;
import com.be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 구글 사용자 정보 추출
        String providerId = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        User user = userRepository.findByProviderAndProviderId(Provider.GOOGLE, providerId)
                .orElseGet(() -> {
                    log.info("[OAuth] 신규 구글 회원 가입: email={}", email);
                    return userRepository.save(User.of(
                            email,
                            name != null ? name : "사용자",
                            Provider.GOOGLE,
                            providerId
                    ));
                });

        // SuccessHandler에서 JWT 발급에 사용할 userId를 attributes에 추가
        Map<String, Object> customAttributes = new HashMap<>(attributes);
        customAttributes.put("userId", user.getId());

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                customAttributes,
                "sub"
        );
    }
}
