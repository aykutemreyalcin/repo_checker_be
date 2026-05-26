package repoChecker.repo_checker_be.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

	@Bean
	WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/api/**")
						.allowedOriginPatterns(
								"http://localhost:5173",
								"http://127.0.0.1:5173",
								"https://security-2.codewithpeter.com",
								"https://*.codewithpeter.com")
						.allowedMethods("GET", "POST", "OPTIONS")
						.allowedHeaders("*");
			}
		};
	}
}
