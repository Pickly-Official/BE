package com.be.domain.mypage.service;

import com.be.domain.mypage.dto.response.MypageStatsResponse;
import com.be.domain.participant.repository.SwipeRepository;
import com.be.domain.user.entity.User;
import com.be.domain.user.repository.UserRepository;
import com.be.domain.vote.repository.VoteRepository;
import com.be.global.exception.CustomException;
import com.be.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MypageService {

    private final UserRepository userRepository;
    private final VoteRepository voteRepository;
    private final SwipeRepository swipeRepository;

    public MypageStatsResponse getStats(Long userId) {
        if (userId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        long voteCount = voteRepository.countByUserId(userId);
        long receivedCount = swipeRepository.countReceivedSwipesByUserId(userId);

        return new MypageStatsResponse(
                user.getNickname(),
                user.getProvider().name(),
                voteCount,
                receivedCount
        );
    }
}
