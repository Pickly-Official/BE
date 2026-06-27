package com.be.domain.vote.dto.request;

import com.be.domain.vote.entity.DeadlineType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VoteCreateRequest(
        @NotBlank String title,
        @NotNull DeadlineType deadlineType,
        boolean useLocation
) {}
