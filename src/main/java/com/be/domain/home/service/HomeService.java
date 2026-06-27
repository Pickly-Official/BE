package com.be.domain.home.service;

import com.be.domain.home.dto.response.HomePopularSpotsResponse;
import com.be.domain.home.dto.response.HomeStatsResponse;
import com.be.domain.participant.repository.ParticipantRepository;
import com.be.domain.participant.repository.SwipeRepository;
import com.be.domain.photo.entity.SpotKeyword;
import com.be.domain.photo.repository.PhotoSpotRepository;
import com.be.domain.photo.repository.PopularSpotProjection;
import com.be.domain.vote.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final ParticipantRepository participantRepository;
    private final VoteRepository voteRepository;
    private final SwipeRepository swipeRepository;
    private final PhotoSpotRepository photoSpotRepository;

    public HomeStatsResponse getStats() {
        long totalParticipants = participantRepository.count();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        long todayVoteCount = voteRepository.countByCreatedAtBetween(todayStart, todayEnd);

        return new HomeStatsResponse(totalParticipants, todayVoteCount);
    }

    public HomePopularSpotsResponse getPopularSpots() {
        List<PopularSpotProjection> top3 = swipeRepository.findTop3PopularSpots();

        List<Long> spotIds = top3.stream().map(PopularSpotProjection::getSpotId).toList();
        Map<Long, List<String>> keywordsMap = photoSpotRepository.findAllById(spotIds).stream()
                .collect(Collectors.toMap(
                        spot -> spot.getId(),
                        spot -> spot.getKeywords().stream().map(SpotKeyword::getKeyword).toList()
                ));

        List<HomePopularSpotsResponse.SpotItem> items = new ArrayList<>();
        for (int i = 0; i < top3.size(); i++) {
            PopularSpotProjection p = top3.get(i);
            items.add(new HomePopularSpotsResponse.SpotItem(
                    i + 1,
                    p.getName(),
                    p.getRecommendRatio(),
                    keywordsMap.getOrDefault(p.getSpotId(), List.of())
            ));
        }

        return new HomePopularSpotsResponse(items);
    }
}
