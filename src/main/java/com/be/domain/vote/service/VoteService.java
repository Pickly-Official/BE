package com.be.domain.vote.service;

import com.be.domain.user.entity.User;
import com.be.domain.user.repository.UserRepository;
import com.be.domain.vote.dto.request.VoteCreateRequest;
import com.be.domain.vote.dto.response.VoteCreateResponse;
import com.be.domain.vote.entity.Vote;
import com.be.domain.vote.repository.VoteRepository;
import com.be.global.exception.CustomException;
import com.be.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteService {

    private final VoteRepository voteRepository;
    private final UserRepository userRepository;

    @Transactional
    public VoteCreateResponse createVote(VoteCreateRequest request) {
        // TODO: JWT 연동 후 SecurityContext에서 userId 추출로 교체
        User user = userRepository.findById(1L)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Vote vote = Vote.of(user, request.title(), request.useLocation(), request.deadlineType());
        Vote saved = voteRepository.save(vote);

        return new VoteCreateResponse(saved.getId(), saved.getTitle(), saved.getClosedAt());
    }
}
