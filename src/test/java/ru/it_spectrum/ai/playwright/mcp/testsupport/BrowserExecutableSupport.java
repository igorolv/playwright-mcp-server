package ru.it_spectrum.ai.playwright.mcp.testsupport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

public final class BrowserExecutableSupport {

    private BrowserExecutableSupport() {
    }

    public static String chromiumExecutableOrNull() {
        String configured = firstEnv(
                "PLAYWRIGHT_MCP_EXECUTABLE",
                "PLAYWRIGHT_CHROMIUM_EXECUTABLE",
                "CHROME_PATH",
                "CHROMIUM_PATH",
                "EDGE_PATH");
        if (configured != null) {
            return configured;
        }
        if (isWindows()) {
            for (String candidate : new String[]{
                    "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                    "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
                    "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
                    "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe"}) {
                if (Files.isRegularFile(Path.of(candidate))) {
                    return candidate;
                }
            }
        }
        String pathExecutable = firstOnPath("google-chrome", "google-chrome-stable", "chromium", "chromium-browser", "chrome", "msedge");
        if (pathExecutable != null) {
            return pathExecutable;
        }
        return playwrightCachedChromiumOrNull();
    }

    private static String firstEnv(String... names) {
        for (String name : names) {
            String value = System.getenv(name);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String firstOnPath(String... names) {
        String path = System.getenv("PATH");
        if (path == null || path.isBlank()) {
            return null;
        }
        for (String dir : path.split(java.io.File.pathSeparator)) {
            for (String name : names) {
                Path candidate = Path.of(dir, isWindows() && !name.endsWith(".exe") ? name + ".exe" : name);
                if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                    return candidate.toString();
                }
            }
        }
        return null;
    }

    private static String playwrightCachedChromiumOrNull() {
        for (Path root : playwrightCacheRoots()) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> paths = Files.find(root, 7, (path, attrs) ->
                    attrs.isRegularFile()
                            && path.toString().toLowerCase(Locale.ROOT).contains("chrom")
                            && isChromiumExecutableName(path.getFileName().toString()))) {
                return paths.findFirst().map(Path::toString).orElse(null);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static Path[] playwrightCacheRoots() {
        String localAppData = System.getenv("LOCALAPPDATA");
        String home = System.getProperty("user.home");
        if (isWindows() && localAppData != null && !localAppData.isBlank()) {
            return new Path[]{Path.of(localAppData, "ms-playwright")};
        }
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac")) {
            return new Path[]{Path.of(home, "Library", "Caches", "ms-playwright")};
        }
        return new Path[]{Path.of(home, ".cache", "ms-playwright")};
    }

    private static boolean isChromiumExecutableName(String fileName) {
        String value = fileName.toLowerCase(Locale.ROOT);
        return value.equals("chrome.exe")
                || value.equals("chrome")
                || value.equals("chromium")
                || value.equals("chromium.exe")
                || value.equals("headless_shell")
                || value.equals("headless_shell.exe");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
