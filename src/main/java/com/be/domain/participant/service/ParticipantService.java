package com.be.domain.participant.service;

import com.be.domain.participant.dto.request.SwipeSubmitRequest;
import com.be.domain.participant.dto.response.SwipeSubmitResponse;
import com.be.domain.participant.entity.Participant;
import com.be.domain.participant.entity.Swipe;
import com.be.domain.participant.entity.SwipeChoice;
import com.be.domain.participant.repository.ParticipantRepository;
import com.be.domain.participant.repository.SwipeRepository;
import com.be.domain.photo.entity.Photo;
import com.be.domain.photo.repository.PhotoRepository;
import com.be.domain.vote.entity.Vote;
import com.be.domain.vote.repository.VoteRepository;
import com.be.global.exception.CustomException;
import com.be.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final SwipeRepository swipeRepository;
    private final VoteRepository voteRepository;
    private final PhotoRepository photoRepository;

    @Transactional
    public SwipeSubmitResponse submitResults(Long voteId, SwipeSubmitRequest request, Long userId) {
        log.info("[submitResults] 진입 - voteId={}, voterUuid={}, userId={}, resultCount={}",
                voteId, request.voterUuid(), userId, request.results() == null ? null : request.results().size());

        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));
        log.info("[submitResults] 투표 조회 성공 - title={}, closed={}", vote.getTitle(), vote.isClosed());

        if (vote.isClosed()) {
            throw new CustomException(ErrorCode.VOTE_ALREADY_CLOSED);
        }

        List<Photo> photos = photoRepository.findByVoteIdOrderBySequenceAsc(vote.getId());
        log.info("[submitResults] 사진 목록 조회 - count={}", photos.size());

        Map<Long, Photo> photoMap = photos.stream()
                .collect(Collectors.toMap(Photo::getId, p -> p));

        if (request.results().size() != photoMap.size()) {
            log.warn("[submitResults] 결과 개수 불일치 - request={}, photos={}", request.results().size(), photoMap.size());
            throw new CustomException(ErrorCode.INVALID_RESULT_COUNT);
        }

        for (SwipeSubmitRequest.ResultItem item : request.results()) {
            if (!photoMap.containsKey(item.photoId())) {
                log.warn("[submitResults] 유효하지 않은 photoId={}", item.photoId());
                throw new CustomException(ErrorCode.PHOTO_NOT_IN_VOTE);
            }
        }

        log.info("[submitResults] participant 조회/생성 시작");
        Participant participant = participantRepository
                .findByVoteIdAndVoterUuid(vote.getId(), request.voterUuid())
                .orElseGet(() -> participantRepository.save(Participant.of(vote, request.voterUuid())));
        log.info("[submitResults] participant 확보 - id={}", participant.getId());

        swipeRepository.deleteAllByParticipant(participant);
        log.info("[submitResults] 기존 swipe 삭제 완료");

        participant.updateUserId(userId);
        participant.complete();
        participant = participantRepository.save(participant);
        log.info("[submitResults] participant 저장 완료 - completedAt={}", participant.getCompletedAt());

        for (SwipeSubmitRequest.ResultItem item : request.results()) {
            SwipeChoice choice = SwipeChoice.from(item.choice());
            swipeRepository.save(Swipe.of(participant, photoMap.get(item.photoId()), choice));
            log.info("[submitResults] swipe 저장 - photoId={}, choice={}", item.photoId(), choice);
        }

        log.info("[submitResults] 완료 - submissionId={}", participant.getId());
        return new SwipeSubmitResponse(participant.getId());
    }
}
