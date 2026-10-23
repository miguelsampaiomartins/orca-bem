package br.com.bolsoclaro;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Stages a verified Windows update and starts a detached PowerShell helper to replace/relaunch the EXE after exit. */
public final class UpdateInstaller {
    private UpdateInstaller() { }

    public static Path currentExecutable() {
        String value = System.getProperty("launch4j.exefile", "").strip();
        if (!value.isEmpty()) {
            Path configured = existingExecutable(value);
            if (configured != null) return configured;
        }
        ProcessHandle process = ProcessHandle.current();
        for (int depth = 0; depth < 6 && process != null; depth++) {
            Path candidate = existingExecutable(process.info().command().orElse(""));
            if (candidate != null && isOrcaBemExecutable(candidate)) return candidate;
            process = process.parent().orElse(null);
        }
        return null;
    }

    /** Launches a detached helper; caller should exit immediately after this method succeeds. */
    public static Path startAfterExit(Path downloadedExecutable, Path installedExecutable, long processId) throws IOException {
        Path source = requireWindowsExecutable(downloadedExecutable, "arquivo baixado");
        Path target = requireWindowsExecutable(installedExecutable, "aplicativo instalado");
        if (processId <= 0) throw new IllegalArgumentException("Identificador de processo inválido.");
        if (source.equals(target)) throw new IOException("O arquivo baixado e o aplicativo aberto são o mesmo arquivo.");

        String scriptText = createPowerShellScript(source, target, processId);
        Path script = Files.createTempFile("orcabem-update-install-", ".ps1");
        try {
            Files.writeString(script, scriptText, StandardCharsets.UTF_8);
            ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                    "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden", "-File", script.toString());
            builder.redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD);
            builder.start();
            return script;
        } catch (IOException | RuntimeException ex) {
            Files.deleteIfExists(script);
            if (ex instanceof IOException io) throw io;
            throw new IOException("Não foi possível iniciar o instalador do Windows.", ex);
        }
    }

    static String createPowerShellScript(Path source, Path target, long processId) {
        String sourceLiteral = psLiteral(source.toAbsolutePath().normalize().toString());
        String targetLiteral = psLiteral(target.toAbsolutePath().normalize().toString());
        StringBuilder script = new StringBuilder();
        script.append("$ErrorActionPreference = 'Stop'\r\n")
                .append("$source = ").append(sourceLiteral).append("\r\n")
                .append("$target = ").append(targetLiteral).append("\r\n")
                .append("$processIdToWait = ").append(processId).append("\r\n")
                .append("$stage = $target + '.' + [guid]::NewGuid().ToString('N') + '.new.exe'\r\n")
                .append("$backup = $target + '.' + [guid]::NewGuid().ToString('N') + '.backup'\r\n")
                .append("try {\r\n")
                .append("  $deadline = [DateTime]::UtcNow.AddSeconds(90)\r\n")
                .append("  while ($true) {\r\n")
                .append("    $running = Get-Process -Id $processIdToWait -ErrorAction SilentlyContinue\r\n")
                .append("    $unlocked = $false\r\n")
                .append("    if (-not $running) {\r\n")
                .append("      try { $probe = [IO.File]::Open($target, [IO.FileMode]::Open, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None); $probe.Dispose(); $unlocked = $true } catch { }\r\n")
                .append("    }\r\n")
                .append("    if ($unlocked) { break }\r\n")
                .append("    if ([DateTime]::UtcNow -ge $deadline) { throw 'O aplicativo ainda está em uso. Tente novamente.' }\r\n")
                .append("    Start-Sleep -Milliseconds 400\r\n")
                .append("  }\r\n")
                .append("  Copy-Item -LiteralPath $source -Destination $stage\r\n")
                .append("  $stream = [IO.File]::OpenRead($stage)\r\n")
                .append("  try { if ($stream.Length -lt 2 -or $stream.ReadByte() -ne 77 -or $stream.ReadByte() -ne 90) { throw 'O executável baixado é inválido.' } } finally { $stream.Dispose() }\r\n")
                .append("  [IO.File]::Replace($stage, $target, $backup, $true)\r\n")
                .append("  Start-Process -FilePath $target\r\n")
                .append("  Start-Sleep -Milliseconds 1200\r\n")
                .append("  Remove-Item -LiteralPath $backup -Force -ErrorAction SilentlyContinue\r\n")
                .append("} catch {\r\n")
                .append("  Remove-Item -LiteralPath $stage -Force -ErrorAction SilentlyContinue\r\n")
                .append("  try { Start-Process -FilePath $source } catch { }\r\n")
                .append("  try { Add-Type -AssemblyName System.Windows.Forms; [System.Windows.Forms.MessageBox]::Show(('Não foi possível substituir o OrçaBem. Uma cópia da versão baixada será aberta.\r\n\r\n' + $_.Exception.Message), 'OrçaBem - atualização', 'OK', 'Warning') | Out-Null } catch { }\r\n")
                .append("} finally {\r\n")
                .append("  Remove-Item -LiteralPath $PSCommandPath -Force -ErrorAction SilentlyContinue\r\n")
                .append("}\r\n");
        return script.toString();
    }

    private static Path existingExecutable(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            Path executable = Path.of(value).toAbsolutePath().normalize();
            return Files.isRegularFile(executable) ? executable : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static boolean isOrcaBemExecutable(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.startsWith("orcabem") && name.endsWith(".exe");
    }

    private static String psLiteral(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private static Path requireWindowsExecutable(Path path, String label) throws IOException {
        if (path == null) throw new IOException("Caminho do " + label + " ausente.");
        Path normalized = path.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized) || !normalized.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".exe"))
            throw new IOException("O " + label + " precisa ser um arquivo .exe existente.");
        if (!hasWindowsSignature(normalized)) throw new IOException("O " + label + " não é um executável Windows válido.");
        return normalized;
    }

    private static boolean hasWindowsSignature(Path path) throws IOException {
        try (var input = Files.newInputStream(path)) {
            return input.read() == 'M' && input.read() == 'Z';
        }
    }
}
