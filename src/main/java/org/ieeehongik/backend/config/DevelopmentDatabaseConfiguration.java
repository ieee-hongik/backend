package org.ieeehongik.backend.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
@Profile("development")
class DevelopmentDatabaseConfiguration {

  @Bean
  static BeanFactoryPostProcessor validateDevelopmentDatabase(ConfigurableEnvironment environment) {
    // Check before any DataSource or SQL initializer can be instantiated.
    return beanFactory -> {
      validatePassword(environment);
      String port = environment.getProperty("DB_PORT");
      if (port != null && (!port.matches("[0-9]{1,5}") || !isValidPort(Integer.parseInt(port)))) {
        throw new IllegalStateException("DB_PORT must be a port between 1 and 65535");
      }
      if (!StringUtils.hasText(environment.getProperty("DB_USERNAME"))) {
        throw new IllegalStateException("DB_USERNAME must be set and nonblank");
      }
    };
  }

  private static void validatePassword(ConfigurableEnvironment environment) {
    // Use Spring's source order, but inspect the winning value before placeholder expansion.
    for (PropertySource<?> source : environment.getPropertySources()) {
      Object value;
      try {
        value = source.getProperty("DB_PASSWORD");
      } catch (RuntimeException ex) {
        throw new IllegalStateException("DB_PASSWORD");
      }
      if (value != null) {
        if (value instanceof String password
            && StringUtils.hasText(password)
            && !password.contains("${")) {
          return;
        }
        break;
      }
    }
    throw new IllegalStateException("DB_PASSWORD");
  }

  private static boolean isValidPort(int port) {
    return port >= 1 && port <= 65535;
  }
}
