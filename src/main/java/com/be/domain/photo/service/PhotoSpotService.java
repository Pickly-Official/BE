package com.be.domain.photo.service;

import com.be.domain.photo.entity.PhotoSpot;
import com.be.domain.photo.repository.PhotoSpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhotoSpotService {

    private final PhotoSpotRepository photoSpotRepository;

    @Transactional
    public PhotoSpot findOrCreate(String name, BigDecimal latitude, BigDecimal longitude) {
        return photoSpotRepository.findByName(name)
                .orElseGet(() -> photoSpotRepository.save(PhotoSpot.of(name, latitude, longitude)));
    }
}
