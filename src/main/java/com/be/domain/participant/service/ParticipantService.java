package com.be.domain.participant.service;

import com.be.domain.participant.repository.ParticipantRepository;
import com.be.domain.participant.repository.SwipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipantService {
    private final ParticipantRepository participantRepository;
    private final SwipeRepository swipeRepository;
    // TODO: implement
}
