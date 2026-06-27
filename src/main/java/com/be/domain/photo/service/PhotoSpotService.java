package com.be.domain.photo.service;

import com.be.domain.photo.repository.PhotoSpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhotoSpotService {
    private final PhotoSpotRepository photoSpotRepository;
    // TODO: implement
}
