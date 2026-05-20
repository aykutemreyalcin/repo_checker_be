package repoChecker.repo_checker_be.util;

import java.net.URI;
import java.util.regex.Pattern;

public final class RepoUrlParser {

	private static final Pattern GITHUB_REPO = Pattern.compile("^https?://github\\.com/([\\w.\\-]+)/([\\w.\\-]+?)(?:\\.git)?/?$");

	private RepoUrlParser() {
	}

	public record ParsedRepo(String owner, String name, String cloneUrl) {
	}

	public static ParsedRepo parse(String repoUrl) {
		String normalized = repoUrl.trim();
		if (normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}

		var matcher = GITHUB_REPO.matcher(normalized);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Only public github.com repository URLs are supported");
		}

		URI uri = URI.create(normalized);
		if (!"github.com".equalsIgnoreCase(uri.getHost())) {
			throw new IllegalArgumentException("Only github.com hosts are allowed");
		}

		String owner = matcher.group(1);
		String name = matcher.group(2);
		String cloneUrl = "https://github.com/" + owner + "/" + name + ".git";
		return new ParsedRepo(owner, name, cloneUrl);
	}
}
