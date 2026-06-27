package com.be.domain.participant.repository;

import com.be.domain.participant.entity.Participant;
import com.be.domain.participant.entity.Swipe;
import com.be.domain.participant.entity.SwipeChoice;
import com.be.domain.photo.repository.PopularSpotProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SwipeRepository extends JpaRepository<Swipe, Long> {
    void deleteAllByParticipant(Participant participant);

    long countByPhotoIdAndChoice(Long photoId, SwipeChoice choice);

   @Query(value = """
            SELECT ps.id AS spotId, ps.name AS name,
                   CAST(ROUND(SUM(CASE WHEN s.choice = 'LIKE' THEN 1 ELSE 0 END) * 100.0 / COUNT(s.id)) AS UNSIGNED) AS recommendRatio
            FROM swipes s
            JOIN photos p ON s.photo_id = p.id
            JOIN photo_spots ps ON p.photo_spot_id = ps.id
            GROUP BY ps.id, ps.name
            ORDER BY recommendRatio DESC, ps.id ASC
            LIMIT 3
            """, nativeQuery = true)
    List<PopularSpotProjection> findTop3PopularSpots();

}