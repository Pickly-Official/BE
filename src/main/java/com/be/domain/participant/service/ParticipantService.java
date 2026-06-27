package com.be.domain.participant.service;

import com.be.domain.participant.dto.request.ParticipantCreateRequest;
import com.be.domain.participant.dto.request.SwipeRequest;
import com.be.domain.participant.dto.response.ParticipantResponse;
import com.be.domain.participant.dto.response.SwipeResponse;
import com.be.domain.participant.entity.Participant;
import com.be.domain.participant.entity.Swipe;
import com.be.domain.participant.repository.ParticipantRepository;
import com.be.domain.participant.repository.SwipeRepository;
import com.be.domain.photo.entity.Photo;
import com.be.domain.photo.repository.PhotoRepository;
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
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final SwipeRepository swipeRepository;
    private final VoteRepository voteRepository;
    private final PhotoRepository photoRepository;

    @Transactional
    public ParticipantResponse register(Long voteId, ParticipantCreateRequest request) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));

        return participantRepository.findByVoteIdAndVoterUuid(voteId, request.deviceToken())
                .map(p -> new ParticipantResponse(p.getId(), true))
                .orElseGet(() -> {
                    Participant saved = participantRepository.save(
                            Participant.of(vote, request.deviceToken()));
                    return new ParticipantResponse(saved.getId(), false);
                });
    }

    @Transactional
    public SwipeResponse saveSwipe(Long participantId, SwipeRequest request) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new CustomException(ErrorCode.PARTICIPANT_NOT_FOUND));

        Photo photo = photoRepository.findById(request.photoId())
                .orElseThrow(() -> new CustomException(ErrorCode.PHOTO_NOT_FOUND));

        Swipe swipe = swipeRepository.findByParticipantIdAndPhotoId(participantId, photo.getId())
                .orElseGet(() -> swipeRepository.save(
                        Swipe.of(participant, photo, request.choice())));

        return new SwipeResponse(swipe.getId(), photo.getId(), swipe.getChoice().name());
    }
}
