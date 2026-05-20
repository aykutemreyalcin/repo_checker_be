package repoChecker.repo_checker_be.util;

public final class SnippetMasker {

	private SnippetMasker() {
	}

	public static String mask(String value) {
		if (value == null || value.isBlank()) {
			return value;
		}
		String trimmed = value.trim();
		if (trimmed.length() <= 8) {
			return "****";
		}
		return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 4);
	}
}
