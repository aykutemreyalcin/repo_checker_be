package repoChecker.repo_checker_be.config;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

	@Bean(name = "scanTaskExecutor")
	Executor scanTaskExecutor(@Value("${scan.max-concurrent:5}") int maxConcurrent) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("scan-");
		executor.setCorePoolSize(maxConcurrent);
		executor.setMaxPoolSize(maxConcurrent);
		executor.setQueueCapacity(100);
		executor.initialize();
		return executor;
	}
}
