package com.nexoia.auth.user.dto;

import com.nexoia.auth.user.model.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull UserStatus status) {
}
