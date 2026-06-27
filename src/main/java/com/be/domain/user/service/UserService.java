package com.be.domain.user.service;

import com.be.domain.user.dto.response.UserProfileResponse;
import com.be.domain.user.entity.User;
import com.be.domain.user.entity.UserStatus;
import com.be.domain.user.repository.UserRepository;
import com.be.global.exception.CustomException;
import com.be.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    public UserProfileResponse getProfile(Long userId) {
        if (userId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new CustomException(ErrorCode.USER_ALREADY_WITHDRAWN);
        }

        return new UserProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getProvider().name(),
                user.getProfileImage()
        );
    }
}
