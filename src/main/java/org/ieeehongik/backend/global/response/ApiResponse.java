package org.ieeehongik.backend.global.response;

import org.ieeehongik.backend.global.exception.ErrorCode;

public record ApiResponse<T>(boolean success, T data, ErrorResponse error) {

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, data, null);
  }

  public static ApiResponse<Void> failure(ErrorCode errorCode) {
    return new ApiResponse<>(
        false, null, new ErrorResponse(errorCode.getCode(), errorCode.getMessage()));
  }
}
