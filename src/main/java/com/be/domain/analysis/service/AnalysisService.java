package com.be.domain.analysis.service;

import com.be.domain.analysis.repository.PhotoAnalysisRepository;
import com.be.domain.analysis.repository.VoteAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisService {
    private final VoteAnalysisRepository voteAnalysisRepository;
    private final PhotoAnalysisRepository photoAnalysisRepository;
    // TODO: implement
}
