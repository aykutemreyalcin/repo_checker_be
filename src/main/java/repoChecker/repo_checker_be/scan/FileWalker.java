package repoChecker.repo_checker_be.scan;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class FileWalker {

	private static final Set<String> SKIPPED_DIR_NAMES = Set.of(
			".git",
			"node_modules",
			"vendor",
			"dist",
			"build",
			"target",
			".gradle",
			"__pycache__",
			".next",
			"coverage",
			".idea");

	private FileWalker() {
	}

	public static boolean isUnderSkippedDirectory(Path file) {
		for (Path part : file) {
			if (part.getFileName() != null
					&& SKIPPED_DIR_NAMES.contains(part.getFileName().toString())) {
				return true;
			}
		}
		return false;
	}

	public static List<Path> listTextFiles(Path repoRoot, long maxFileSizeBytes) throws IOException {
		List<Path> files = new ArrayList<>();
		Files.walkFileTree(repoRoot, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
				if (dir.getFileName() != null
						&& SKIPPED_DIR_NAMES.contains(dir.getFileName().toString())) {
					return FileVisitResult.SKIP_SUBTREE;
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				if (attrs.isRegularFile()
						&& attrs.size() <= maxFileSizeBytes
						&& !isBinary(file)) {
					files.add(file);
				}
				return FileVisitResult.CONTINUE;
			}
		});
		return files;
	}

	private static boolean isBinary(Path file) {
		try {
			byte[] sample = Files.readAllBytes(file);
			int limit = Math.min(sample.length, 512);
			for (int i = 0; i < limit; i++) {
				if (sample[i] == 0) {
					return true;
				}
			}
			return false;
		}
		catch (IOException ex) {
			return true;
		}
	}
}
