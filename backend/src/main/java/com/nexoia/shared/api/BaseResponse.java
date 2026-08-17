package com.nexoia.shared.api;

import java.util.Collection;
import java.util.List;

public record BaseResponse<T>(int code, String message, List<T> data) {

    public static <T> BaseResponse<T> success(int code, String message, T data) {
        return new BaseResponse<>(code, message, List.of(data));
    }

    public static <T> BaseResponse<T> success(int code, String message, Collection<T> data) {
        return new BaseResponse<>(code, message, List.copyOf(data));
    }

    public static BaseResponse<Void> success(int code, String message) {
        return new BaseResponse<>(code, message, List.of());
    }

    public static BaseResponse<Void> error(int code, String message) {
        return new BaseResponse<>(code, message, null);
    }
}
