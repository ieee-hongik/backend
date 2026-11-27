package org.ieeehongik.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(OutputCaptureExtension.class)
class DevelopmentDatabaseConfigurationTests {

  private final Map<String, Object> settings =
      new HashMap<>(Map.of("DB_USERNAME", "testuser", "DB_PASSWORD", "syntheticPassword"));

  private ConfigurableApplicationContext start(String profile, Map<String, Object> overrides) {
    var environment = new MockEnvironment();
    environment.getPropertySources().addFirst(new MapPropertySource("base", settings));
    environment.getPropertySources().addFirst(new MapPropertySource("overrides", overrides));
    return new SpringApplicationBuilder(DevelopmentDatabaseConfiguration.class)
        .environment(environment)
        .web(WebApplicationType.NONE)
        .run("--spring.profiles.active=" + profile);
  }

  @Test
  void usesDefaultPort() {
    try (var context = start("development", Map.of())) {
      assertThat(context.getEnvironment().getRequiredProperty("spring.datasource.url"))
          .isEqualTo("jdbc:mysql://127.0.0.1:3306/ieee_db");
    }
  }

  @Test
  void usesWinningPasswordAndExplicitPort() {
    settings.put("DB_PASSWORD", "unused${MISSING}");
    try (var context =
        start("development", Map.of("DB_PASSWORD", "syntheticOverride", "DB_PORT", "3307"))) {
      var environment = context.getEnvironment();
      assertThat(environment.getRequiredProperty("spring.datasource.url"))
          .isEqualTo("jdbc:mysql://127.0.0.1:3307/ieee_db");
      assertThat(environment.getRequiredProperty("spring.datasource.password"))
          .isEqualTo("syntheticOverride");
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "abc", "0", "65536"})
  void rejectsInvalidPort(String port) {
    assertThat(catchThrowable(() -> start("development", Map.of("DB_PORT", port))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("DB_PORT");
  }

  @ParameterizedTest
  @CsvSource(
      value = {
        "DB_USERNAME,", "DB_USERNAME,''", "DB_USERNAME,'   '",
        "DB_PASSWORD,", "DB_PASSWORD,''", "DB_PASSWORD,'   '"
      })
  void requiresNonblankCredentials(String name, String value) {
    if (value == null) {
      settings.remove(name);
    } else {
      settings.put(name, value);
    }
    assertThat(catchThrowable(() -> start("development", Map.of())))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(name);
  }

  @ParameterizedTest
  @ValueSource(strings = {"${EXISTS}", "${MISSING}", "${MISSING:fallback}", "${unfinished"})
  void rejectsRawPlaceholderWithoutLoggingPassword(String pattern, CapturedOutput output) {
    String password = "syntheticPrefix" + pattern + "syntheticSuffix";
    var overrides = Map.<String, Object>of("DB_PASSWORD", password, "EXISTS", "syntheticTarget");
    assertThat(catchThrowable(() -> start("development", overrides)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("DB_PASSWORD")
        .hasNoCause();
    assertThat(output.getAll())
        .contains("DB_PASSWORD")
        .doesNotContain("syntheticPrefix", "syntheticSuffix", "syntheticTarget");
  }

  @Test
  void skipsValidationForTestProfile() {
    settings.clear();
    try (var context = start("test", Map.of("DB_PORT", "invalid"))) {
      assertThat(context.containsBean("validateDevelopmentDatabase")).isFalse();
    }
  }
}
