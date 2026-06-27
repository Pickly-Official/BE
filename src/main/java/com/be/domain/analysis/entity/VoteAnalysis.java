package com.be.domain.analysis.entity;

import com.be.domain.vote.entity.Vote;
import com.be.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "vote_analyses",
       uniqueConstraints = @UniqueConstraint(columnNames = {"vote_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id", nullable = false)
    private Vote vote;

    @Column(nullable = false, length = 50)
    private String modelName;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalysisStatus status;

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnalysisKeyword> keywords = new ArrayList<>();

    public static VoteAnalysis of(Vote vote) {
        VoteAnalysis va = new VoteAnalysis();
        va.vote = vote;
        va.modelName = "GPT-4o Vision";
        va.status = AnalysisStatus.PENDING;
        return va;
    }

    public void complete(String summary) {
        this.summary = summary;
        this.status = AnalysisStatus.DONE;
    }

    public void fail() {
        this.status = AnalysisStatus.FAILED;
    }

    public void addKeyword(String keyword) {
        this.keywords.add(AnalysisKeyword.of(this, keyword));
    }
}
