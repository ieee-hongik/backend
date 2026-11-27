package org.ieeehongik.backend.global.exception;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.ieeehongik.backend.global.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestController.class})
class GlobalExceptionHandlerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("정상 요청은 공통 성공 응답을 반환한다")
  void successResponse() throws Exception {

    mockMvc
        .perform(get("/test/success"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(1))
        .andExpect(jsonPath("$.error").value(nullValue()));
  }

  @Test
  @DisplayName("반환 데이터가 없는 성공 응답을 처리한다")
  void successWithoutData() throws Exception {

    mockMvc
        .perform(get("/test/no-data"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").value(nullValue()))
        .andExpect(jsonPath("$.error").value(nullValue()));
  }

  @Test
  @DisplayName("비즈니스 예외는 정의된 HTTP 상태와 에러 코드를 반환한다")
  void businessException() throws Exception {

    mockMvc
        .perform(get("/test/business"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.data").value(nullValue()))
        .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
        .andExpect(jsonPath("$.error.message").value("접속 권한이 없습니다."));
  }

  @Test
  @DisplayName("@Valid 검증 실패는 INVALID_INPUT을 반환한다")
  void validationException() throws Exception {

    mockMvc
        .perform(
            post("/test/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
  }

  @Test
  @DisplayName("잘못된 JSON 요청은 BAD_REQUEST를 반환한다")
  void invalidJsonException() throws Exception {

    mockMvc
        .perform(
            post("/test/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
  }

  @Test
  @DisplayName("존재하지 않는 API는 NOT_FOUND_END_POINT를 반환한다")
  void notFoundException() throws Exception {

    mockMvc
        .perform(get("/not-existing-endpoint"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("NOT_FOUND_END_POINT"));
  }

  @Test
  @DisplayName("허용되지 않은 HTTP 메서드는 405를 반환한다")
  void methodNotAllowedException() throws Exception {

    mockMvc
        .perform(post("/test/success"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
  }

  @Test
  @DisplayName("지원하지 않는 Content-Type은 415를 반환한다")
  void unsupportedMediaTypeException() throws Exception {

    mockMvc
        .perform(post("/test/validate").contentType(MediaType.TEXT_PLAIN).content("name=test"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_MEDIA_TYPE"));
  }

  @Test
  @DisplayName("예상하지 못한 예외는 내부 정보를 노출하지 않는다")
  void unexpectedException() throws Exception {

    mockMvc
        .perform(get("/test/unexpected"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.data").value(nullValue()))
        .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.error.message").value("서버 내부 오류입니다."));
  }

  @RestController
  @RequestMapping("/test")
  public static class TestController {

    @GetMapping("/success")
    public ApiResponse<Map<String, Integer>> success() {
      return ApiResponse.success(Map.of("id", 1));
    }

    @GetMapping("/no-data")
    public ApiResponse<Void> noData() {
      return ApiResponse.success(null);
    }

    @GetMapping("/business")
    public ApiResponse<Void> business() {
      throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }

    @PostMapping("/validate")
    public ApiResponse<String> validate(@Valid @RequestBody TestRequest request) {
      return ApiResponse.success(request.name());
    }

    @GetMapping("/unexpected")
    public ApiResponse<Void> unexpected() {
      throw new IllegalStateException("Internal sensitive information");
    }
  }

  public record TestRequest(@NotBlank String name) {}
}
