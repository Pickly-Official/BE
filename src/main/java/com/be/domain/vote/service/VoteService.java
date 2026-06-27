package com.be.domain.vote.service;

import com.be.domain.photo.entity.Photo;
import com.be.domain.photo.repository.PhotoRepository;
import com.be.domain.user.entity.User;
import com.be.domain.user.repository.UserRepository;
import com.be.domain.vote.dto.response.VoteDetailResponse;
import com.be.domain.vote.dto.request.VoteCreateRequest;
import com.be.domain.vote.dto.response.VoteCreateResponse;
import com.be.domain.vote.entity.Vote;
import com.be.domain.vote.entity.DeadlineType;
import com.be.domain.vote.repository.VoteRepository;
import com.be.global.exception.CustomException;
import com.be.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteService {

    private final VoteRepository voteRepository;
    private final UserRepository userRepository;
    private final PhotoRepository photoRepository;

    @Transactional
    public VoteCreateResponse createVote(VoteCreateRequest request) {
        // TODO: JWT 연동 후 SecurityContext에서 userId 추출로 교체
        User user = userRepository.findById(1L)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Vote vote = Vote.of(user, request.title(), request.useLocation(), request.deadlineType());
        Vote saved = voteRepository.save(vote);

        return new VoteCreateResponse(saved.getId(), saved.getTitle(), saved.getClosedAt());
    }

    public VoteDetailResponse getVoteDetail(Long voteId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));

        if (vote.isClosed()) {
            throw new CustomException(ErrorCode.VOTE_ALREADY_CLOSED);
        }

        List<Photo> photos = photoRepository.findByVoteIdOrderBySequenceAsc(voteId);

        List<VoteDetailResponse.PhotoItem> photoItems = IntStream.range(0, photos.size())
                .mapToObj(i -> new VoteDetailResponse.PhotoItem(
                        photos.get(i).getId(),
                        photos.get(i).getImageUrl(),
                        i))
                .toList();

        return new VoteDetailResponse(vote.getId(), vote.getTitle(), photoItems);
    }
}
