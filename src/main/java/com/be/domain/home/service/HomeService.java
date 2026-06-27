package com.be.domain.home.service;

import com.be.domain.home.dto.response.HomeStatsResponse;
import com.be.domain.participant.repository.ParticipantRepository;
import com.be.domain.vote.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final ParticipantRepository participantRepository;
    private final VoteRepository voteRepository;

    public HomeStatsResponse getStats() {
        long totalParticipants = participantRepository.count();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        long todayVoteCount = voteRepository.countByCreatedAtBetween(todayStart, todayEnd);

        return new HomeStatsResponse(totalParticipants, todayVoteCount);
    }
}
