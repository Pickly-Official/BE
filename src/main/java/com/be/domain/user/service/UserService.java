package com.be.domain.user.service;

import com.be.domain.user.dto.response.UserProfileResponse;
import com.be.domain.user.dto.response.UserStatsResponse;
import com.be.domain.user.entity.User;
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
        User user = findById(userId);
        return new UserProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getProvider().name().toLowerCase(),
                user.getProfileImage()
        );
    }

    public UserStatsResponse getStats(Long userId) {
        findById(userId);
        return new UserStatsResponse(userId, 0, 0);
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = findById(userId);
        user.withdraw();
    }

    private User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
