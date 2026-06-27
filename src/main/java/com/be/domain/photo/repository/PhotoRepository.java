package com.be.domain.photo.repository;

import com.be.domain.photo.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findByVoteIdOrderBySequenceAsc(Long voteId);
}
