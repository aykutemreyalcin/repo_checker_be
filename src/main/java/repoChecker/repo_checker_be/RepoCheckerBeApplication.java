package repoChecker.repo_checker_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class RepoCheckerBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(RepoCheckerBeApplication.class, args);
	}

}
