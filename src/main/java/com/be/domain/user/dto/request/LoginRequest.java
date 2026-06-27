package com.be.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 일반 이메일/비밀번호 로그인 요청.
 * (기존 소셜 로그인 record 는 미사용 상태였으므로 교체함)
 */
public record LoginRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {}
