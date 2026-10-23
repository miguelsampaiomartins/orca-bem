package br.com.bolsoclaro;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Read-only GitHub Release checker and verified downloader for the Windows app. */
public final class UpdateService {
    public static final String RELEASES_API = "https://api.github.com/repos/miguelsampaiomartins/orca-bem/releases/latest";
    private static final long MAX_DOWNLOAD_BYTES = 150L * 1024L * 1024L;
    private static final Pattern STABLE_VERSION = Pattern.compile("(?i)^v?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)$");
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private UpdateService() { }

    public record Release(String tag, String name, String notes, URI pageUri,
                          String assetName, URI assetUri, String sha256, long size) {
        public boolean hasVerifiedWindowsAsset() {
            return assetUri != null && "OrcaBem.exe".equalsIgnoreCase(assetName)
                    && sha256 != null && sha256.matches("(?i)[0-9a-f]{64}");
        }
    }

    /** Returns empty when the repository has no published, non-draft release yet. */
    public static java.util.Optional<Release> fetchLatest() throws IOException, InterruptedException {
        String configuredUrl = System.getProperty("orcabem.update.apiUrl");
        URI apiUri = URI.create(configuredUrl == null || configuredUrl.isBlank() ? RELEASES_API : configuredUrl);
        boolean localTestEndpoint = configuredUrl != null && !configuredUrl.isBlank();
        if (!isAllowedApiUri(apiUri, localTestEndpoint)) throw new IOException("O endereço de atualização não é confiável.");

        HttpRequest request = HttpRequest.newBuilder(apiUri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "OrcaBem-Update-Checker/1.0")
                .GET().build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 404) return java.util.Optional.empty();
        if (response.statusCode() == 403) throw new IOException("O GitHub limitou temporariamente as consultas. Tente novamente mais tarde.");
        if (response.statusCode() != 200) throw new IOException("O GitHub respondeu com HTTP " + response.statusCode() + ".");

        Object parsed = new JsonParser(response.body()).parse();
        Map<?, ?> root = requireObject(parsed, "resposta da Release");
        String tag = string(root.get("tag_name"));
        if (tag == null || tag.isBlank()) throw new IOException("A Release mais recente não possui uma versão identificável.");
        String name = string(root.get("name"));
        String notes = string(root.get("body"));
        URI page = safeGitHubUri(string(root.get("html_url")), false);

        String assetName = null;
        URI assetUri = null;
        String sha256 = null;
        long size = 0;
        Object rawAssets = root.get("assets");
        if (rawAssets instanceof List<?> assets) {
            for (Object rawAsset : assets) {
                if (!(rawAsset instanceof Map<?, ?> asset)) continue;
                String candidate = string(asset.get("name"));
                if (candidate == null || !"OrcaBem.exe".equalsIgnoreCase(candidate)) continue;
                assetName = candidate;
                boolean local = localTestEndpoint;
                assetUri = safeGitHubUri(string(asset.get("browser_download_url")), local);
                sha256 = normalizeDigest(string(asset.get("digest")));
                size = number(asset.get("size"));
                break;
            }
        }
        return java.util.Optional.of(new Release(tag, name == null || name.isBlank() ? tag : name,
                notes == null ? "" : notes, page, assetName, assetUri, sha256, size));
    }

    /** Compares stable semantic versions such as 1.2.3 and v1.2.4. */
    public static int compareVersions(String left, String right) {
        int[] a = parseVersion(left);
        int[] b = parseVersion(right);
        for (int i = 0; i < a.length; i++) {
            int comparison = Integer.compare(a[i], b[i]);
            if (comparison != 0) return comparison;
        }
        return 0;
    }

    public static boolean isStableVersion(String version) {
        return version != null && STABLE_VERSION.matcher(version.strip()).matches();
    }

    /** Downloads to the selected folder and rejects assets whose GitHub SHA-256 digest does not match. */
    public static Path download(Release release, Path destinationDirectory, IntConsumer progress)
            throws IOException, InterruptedException {
        if (release == null || !release.hasVerifiedWindowsAsset())
            throw new IOException("A Release precisa incluir OrcaBem.exe e os dados necessários para conferir sua integridade.");
        if (!isStableVersion(release.tag())) throw new IOException("A versão da Release não está no formato x.y.z.");
        if (release.size() > MAX_DOWNLOAD_BYTES) throw new IOException("O arquivo da atualização ultrapassa o limite de tamanho permitido.");
        boolean localTestEndpoint = System.getProperty("orcabem.update.apiUrl") != null;
        if (!isAllowedDownloadUri(release.assetUri(), localTestEndpoint)) throw new IOException("O endereço do executável não é confiável.");

        Files.createDirectories(destinationDirectory);
        Path partial = Files.createTempFile(destinationDirectory, ".orcabem-update-", ".part");
        try {
            HttpRequest request = HttpRequest.newBuilder(release.assetUri())
                    .timeout(Duration.ofMinutes(3))
                    .header("User-Agent", "OrcaBem-Update-Checker/1.0")
                    .header("Accept", "application/octet-stream")
                    .GET().build();
            HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                try (InputStream ignored = response.body()) { }
                throw new IOException("Não foi possível baixar o executável (HTTP " + response.statusCode() + ").");
            }
            if (!isAllowedDownloadUri(response.uri(), localTestEndpoint)) {
                try (InputStream ignored = response.body()) { }
                throw new IOException("O redirecionamento do download levou a um endereço não autorizado.");
            }

            MessageDigest digest = sha256();
            long total = 0;
            int lastPercent = -1;
            try (InputStream input = response.body(); OutputStream output = Files.newOutputStream(partial)) {
                byte[] buffer = new byte[32 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    total += count;
                    if (total > MAX_DOWNLOAD_BYTES || (release.size() > 0 && total > release.size()))
                        throw new IOException("O download ultrapassou o tamanho informado pelo GitHub.");
                    digest.update(buffer, 0, count);
                    output.write(buffer, 0, count);
                    if (release.size() > 0) {
                        int percent = (int) Math.min(100, total * 100 / release.size());
                        if (percent != lastPercent) { progress.accept(percent); lastPercent = percent; }
                    }
                }
            }
            if (release.size() > 0 && total != release.size())
                throw new IOException("O tamanho baixado não corresponde ao tamanho informado pelo GitHub.");
            String actualDigest = HexFormat.of().formatHex(digest.digest());
            if (!actualDigest.equalsIgnoreCase(release.sha256()))
                throw new IOException("A conferência de integridade falhou. O arquivo foi descartado por segurança.");
            progress.accept(100);

            String version = release.tag().replaceFirst("(?i)^v", "");
            String extension = Boolean.getBoolean("orcabem.update.simulation") ? ".exe.txt" : ".exe";
            Path target = uniqueTarget(destinationDirectory, "OrcaBem-" + version + extension);
            try {
                Files.move(partial, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(partial, target);
            }
            return target;
        } catch (IOException | InterruptedException | RuntimeException ex) {
            Files.deleteIfExists(partial);
            throw ex;
        }
    }

    private static Path uniqueTarget(Path directory, String fileName) {
        Path candidate = directory.resolve(fileName);
        int suffix = 1;
        while (Files.exists(candidate)) {
            int dot = fileName.lastIndexOf('.');
            candidate = directory.resolve(fileName.substring(0, dot) + "-" + suffix++ + fileName.substring(dot));
        }
        return candidate;
    }

    private static boolean isAllowedApiUri(URI uri, boolean localTestEndpoint) {
        if (uri == null || uri.getUserInfo() != null || uri.getHost() == null) return false;
        if (localTestEndpoint && isLoopback(uri.getHost())) return "http".equalsIgnoreCase(uri.getScheme());
        return "https".equalsIgnoreCase(uri.getScheme()) && "api.github.com".equalsIgnoreCase(uri.getHost());
    }

    private static boolean isAllowedDownloadUri(URI uri, boolean localTestEndpoint) {
        if (uri == null || uri.getUserInfo() != null || uri.getHost() == null) return false;
        String host = uri.getHost().toLowerCase(java.util.Locale.ROOT);
        if (localTestEndpoint && isLoopback(host)) return "http".equalsIgnoreCase(uri.getScheme());
        return "https".equalsIgnoreCase(uri.getScheme()) && (host.equals("github.com") || host.endsWith(".githubusercontent.com"));
    }

    private static boolean isLoopback(String host) {
        String value = host.toLowerCase(java.util.Locale.ROOT);
        return value.equals("localhost") || value.equals("127.0.0.1") || value.equals("::1") || value.equals("[::1]");
    }

    private static URI safeGitHubUri(String value, boolean allowLoopback) throws IOException {
        if (value == null || value.isBlank()) return null;
        try {
            URI uri = URI.create(value);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(java.util.Locale.ROOT);
            boolean trusted = allowLoopback && isLoopback(host) && "http".equalsIgnoreCase(uri.getScheme());
            trusted |= "https".equalsIgnoreCase(uri.getScheme()) && (host.equals("github.com") || host.equals("api.github.com") || host.endsWith(".githubusercontent.com"));
            if (!trusted || uri.getUserInfo() != null) throw new IOException("A Release contém um link fora dos domínios autorizados.");
            return uri;
        } catch (IllegalArgumentException ex) {
            throw new IOException("A Release contém um link inválido.", ex);
        }
    }

    private static String normalizeDigest(String digest) {
        if (digest == null) return null;
        String value = digest.strip();
        if (value.regionMatches(true, 0, "sha256:", 0, 7)) value = value.substring(7);
        return value.matches("(?i)[0-9a-f]{64}") ? value.toLowerCase(java.util.Locale.ROOT) : null;
    }

    private static int[] parseVersion(String version) {
        if (version == null) throw new IllegalArgumentException("Versão ausente.");
        Matcher matcher = STABLE_VERSION.matcher(version.strip());
        if (!matcher.matches()) throw new IllegalArgumentException("Use uma versão estável no formato x.y.z, por exemplo v1.2.3.");
        try {
            return new int[] { Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)) };
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Um número da versão é grande demais.", ex);
        }
    }

    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 indisponível nesta Java Runtime.", ex); }
    }

    private static Map<?, ?> requireObject(Object value, String label) throws IOException {
        if (value instanceof Map<?, ?> map) return map;
        throw new IOException("O GitHub retornou um formato JSON inválido para " + label + ".");
    }

    private static String string(Object value) {
        return value instanceof String text ? text : null;
    }

    private static long number(Object value) {
        if (value instanceof BigDecimal decimal) {
            try { return decimal.longValueExact(); }
            catch (ArithmeticException ignored) { return 0; }
        }
        return 0;
    }

    private static final class JsonParser {
        private final String source;
        private int cursor;

        JsonParser(String source) { this.source = source; }

        Object parse() throws IOException {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (cursor != source.length()) fail("Texto extra após o JSON.");
            return value;
        }

        private Object parseValue() throws IOException {
            skipWhitespace();
            if (cursor >= source.length()) return fail("JSON incompleto.");
            return switch (source.charAt(cursor)) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> { literal("true"); yield Boolean.TRUE; }
                case 'f' -> { literal("false"); yield Boolean.FALSE; }
                case 'n' -> { literal("null"); yield null; }
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() throws IOException {
            cursor++;
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (take('}')) return result;
            while (true) {
                skipWhitespace();
                if (cursor >= source.length() || source.charAt(cursor) != '"') return fail("Chave JSON inválida.");
                String key = parseString();
                skipWhitespace(); require(':');
                result.put(key, parseValue());
                skipWhitespace();
                if (take('}')) return result;
                require(',');
            }
        }

        private List<Object> parseArray() throws IOException {
            cursor++;
            List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (take(']')) return result;
            while (true) {
                result.add(parseValue());
                skipWhitespace();
                if (take(']')) return result;
                require(',');
            }
        }

        private String parseString() throws IOException {
            require('"');
            StringBuilder result = new StringBuilder();
            while (cursor < source.length()) {
                char ch = source.charAt(cursor++);
                if (ch == '"') return result.toString();
                if (ch < 0x20) return fail("Caractere de controle em texto JSON.");
                if (ch != '\\') { result.append(ch); continue; }
                if (cursor >= source.length()) return fail("Escape JSON incompleto.");
                char escaped = source.charAt(cursor++);
                switch (escaped) {
                    case '"', '\\', '/' -> result.append(escaped);
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> {
                        if (cursor + 4 > source.length()) return fail("Escape unicode incompleto.");
                        try { result.append((char) Integer.parseInt(source.substring(cursor, cursor + 4), 16)); }
                        catch (NumberFormatException ex) { return fail("Escape unicode inválido."); }
                        cursor += 4;
                    }
                    default -> { return fail("Escape JSON inválido."); }
                }
            }
            return fail("Texto JSON sem fechamento.");
        }

        private BigDecimal parseNumber() throws IOException {
            int start = cursor;
            if (take('-')) { }
            while (cursor < source.length() && Character.isDigit(source.charAt(cursor))) cursor++;
            if (take('.')) while (cursor < source.length() && Character.isDigit(source.charAt(cursor))) cursor++;
            if (cursor < source.length() && (source.charAt(cursor) == 'e' || source.charAt(cursor) == 'E')) {
                cursor++;
                if (cursor < source.length() && (source.charAt(cursor) == '+' || source.charAt(cursor) == '-')) cursor++;
                while (cursor < source.length() && Character.isDigit(source.charAt(cursor))) cursor++;
            }
            if (start == cursor) return fail("Valor JSON inválido.");
            try { return new BigDecimal(source.substring(start, cursor)); }
            catch (NumberFormatException ex) { return fail("Número JSON inválido."); }
        }

        private void literal(String expected) throws IOException {
            if (!source.startsWith(expected, cursor)) fail("Literal JSON inválido.");
            cursor += expected.length();
        }

        private boolean take(char expected) {
            if (cursor < source.length() && source.charAt(cursor) == expected) { cursor++; return true; }
            return false;
        }

        private void require(char expected) throws IOException {
            if (!take(expected)) fail("Esperado '" + expected + "'.");
        }

        private void skipWhitespace() {
            while (cursor < source.length() && Character.isWhitespace(source.charAt(cursor))) cursor++;
        }

        private <T> T fail(String message) throws IOException {
            throw new IOException(message + " Posição " + cursor + ".");
        }
    }
}
