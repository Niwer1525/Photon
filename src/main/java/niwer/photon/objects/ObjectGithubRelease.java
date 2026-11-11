package niwer.photon.objects;

import java.util.List;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

public class ObjectGithubRelease {
    
    private static final Set<String> EXECUTABLE_EXTENSIONS = Set.of(
        // Windows
        ".exe", ".msi", ".bat", ".cmd", ".ps1", ".vbs",
        // macOS
        ".dmg", ".pkg", ".app",
        // Linux / Unix packages & bundles
        ".appimage", ".deb", ".rpm", ".snap", ".flatpak", ".flatpakref", ".apk",
        // Scripts & byte-code
        ".sh", ".bash", ".zsh", ".jar", ".war", ".pyz", ".wasm"
    );

    private static final Set<String> EXECUTABLE_CONTENT_TYPES = Set.of(
        "application/x-executable",
        "application/x-msdos-program",
        "application/x-msi",
        "application/vnd.android.package-archive",
        "application/java-archive",
        "application/x-sh",
        "application/x-apple-diskimage"
    );

    private static final Set<String> EXECUTABLE_CONTENT_TYPES_LOOSE = Set.of(
        "application/octet-stream"
        // "application/x-binary",
        // "application/x-mach-binary",
        // "application/x-elf",
        // "application/x-dosexec"
    );

    @SerializedName("id") public long id; 
    @SerializedName("tag_name") public String tagName;
    @SerializedName("name") public String name;
    @SerializedName("body") public String body;
    @SerializedName("draft") public boolean draft;
    @SerializedName("prerelease") public boolean prerelease;
    @SerializedName("published_at") public String publishedAt;
    @SerializedName("assets") public List<GithubAsset> assets;

    public boolean containsExecutable() {
        if (assets == null) return false;
        return assets.stream().anyMatch(asset -> asset.isExecutable());
    }

    public void keepOnlyExecutableAssets() {
        if (assets == null) return;
        assets.removeIf(asset -> !asset.isExecutable());
    }

    @Override
    public String toString() {
        return String.format("ObjectGithubRelease{id=%d, tagName='%s', name='%s', body='%s', draft=%b, prerelease=%b, publishedAt='%s'}", id, tagName, name, body, draft, prerelease, publishedAt);
    }

    public class GithubAsset {
        @SerializedName("id") public long id;
        @SerializedName("name") public String name;
        @SerializedName("size") public long size;
        @SerializedName("content_type") public String contentType;
        @SerializedName("download_count") public int downloadCount;

        /**
         * Heuristic to determine if a GitHub release asset is likely an executable file based on its name and content type.
         * 
         * @return True if the asset is likely an executable, false otherwise.
         */
        public boolean isExecutable() {
            if (name != null && !name.isBlank() && contentType != null && !contentType.isBlank()) return isExecutableByNameOrContentType(name, contentType);
            return false;
        }

        private boolean isExecutableByNameOrContentType(final String name, final String contentType) {
            final String NORMALIZED_NAME = name.toLowerCase();
            final String NORMALIZED_CONTENT_TYPE = contentType.toLowerCase();

            if (EXECUTABLE_CONTENT_TYPES.contains(NORMALIZED_CONTENT_TYPE)) return true; // Check if the content type is in the set of known executable content types

            /* Check if the file name ends with any of the known executable extensions */
            for (String ext : EXECUTABLE_EXTENSIONS) {
                if (NORMALIZED_NAME.endsWith(ext)) return true;
            }

            return isLikelyRawUnixBinary(NORMALIZED_NAME, NORMALIZED_CONTENT_TYPE); // If neither check passed, check if the file name suggests it's a raw Unix binary
        }

        /**
         * Heuristic to determine if a file is likely a raw Unix binary based on its name and content type.
         * E.G., "my-cli-linux-x86_64" or "my-tool_darwin_arm64"
         * 
         * @param normalizedName The lowercased name of the asset file.
         * @param normalizedContentType The lowercased content type of the asset file.
         * @return True if the asset is likely a raw Unix binary, false otherwise.
         */
        private boolean isLikelyRawUnixBinary(final String normalizedName, final String normalizedContentType) {
            /* Must not have an extension (no dot, or dot is hidden file prefix) */
            final int LAST_DOT = normalizedName.lastIndexOf('.');
            if (LAST_DOT > 0) return false; // Has a non-executable extension (e.g., .txt, .json, .zip, .sig)

            /* Common patterns in GitHub release assets for raw binaries */
            final boolean IS_UNIX_BINARY = normalizedName.contains("linux")
                || normalizedName.contains("darwin")
                || normalizedName.contains("macos")
                || normalizedName.contains("amd64")
                || normalizedName.contains("x86_64")
                || normalizedName.contains("arm64")
                || normalizedName.contains("aarch64");

            return IS_UNIX_BINARY && EXECUTABLE_CONTENT_TYPES_LOOSE.contains(normalizedContentType);
        }
    }
}
