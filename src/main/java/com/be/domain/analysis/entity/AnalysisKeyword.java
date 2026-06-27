package com.be.domain.analysis.entity;

import com.be.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "analysis_keywords")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisKeyword extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private VoteAnalysis analysis;

    @Column(nullable = false, length = 50)
    private String keyword;

    public static AnalysisKeyword of(VoteAnalysis analysis, String keyword) {
        AnalysisKeyword ak = new AnalysisKeyword();
        ak.analysis = analysis;
        ak.keyword = keyword;
        return ak;
    }
}
