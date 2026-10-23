package br.com.bolsoclaro;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Stages a verified Windows update and replaces/relaunches the EXE after all running instances exit. */
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

    /** Starts a detached helper; caller should exit immediately after this method succeeds. */
    public static Path startAfterExit(Path downloadedExecutable, Path installedExecutable, long processId) throws IOException {
        Path source = requireWindowsExecutable(downloadedExecutable, "arquivo baixado");
        Path target = requireWindowsExecutable(installedExecutable, "aplicativo instalado");
        if (processId <= 0) throw new IllegalArgumentException("Identificador de processo inválido.");
        if (source.equals(target)) throw new IOException("O arquivo baixado e o aplicativo aberto são o mesmo arquivo.");

        String scriptText = createPowerShellScript(source, target, processId);
        Path script = Files.createTempFile("orcabem-update-install-", ".ps1");
        try {
            byte[] body = scriptText.getBytes(StandardCharsets.UTF_8);
            byte[] withBom = new byte[body.length + 3];
            withBom[0] = (byte) 0xEF;
            withBom[1] = (byte) 0xBB;
            withBom[2] = (byte) 0xBF;
            System.arraycopy(body, 0, withBom, 3, body.length);
            Files.write(script, withBom);
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
                .append("$scriptPath = $PSCommandPath\r\n")
                .append("$log = [IO.Path]::ChangeExtension($scriptPath, '.log')\r\n")
                .append("$stage = $target + '.' + [guid]::NewGuid().ToString('N') + '.new.exe'\r\n")
                .append("$backup = $target + '.' + [guid]::NewGuid().ToString('N') + '.backup'\r\n")
                .append("function Write-UpdateLog([string]$message) { Add-Content -LiteralPath $log -Value ('[{0}] {1}' -f [DateTime]::Now.ToString('s'), $message) -Encoding UTF8 }\r\n")
                .append("function Test-OrcaBemRunning {\r\n")
                .append("  $pidAlive = $null -ne (Get-Process -Id $processIdToWait -ErrorAction SilentlyContinue)\r\n")
                .append("  $targetAlive = $false\r\n")
                .append("  try {\r\n")
                .append("    $processes = @(Get-CimInstance -ClassName Win32_Process -ErrorAction Stop | Where-Object { $_.ExecutablePath -and [string]::Equals($_.ExecutablePath, $target, [StringComparison]::OrdinalIgnoreCase) })\r\n")
                .append("    $targetAlive = $processes.Count -gt 0\r\n")
                .append("  } catch { Write-UpdateLog ('Não foi possível enumerar processos pelo caminho: ' + $_.Exception.Message) }\r\n")
                .append("  return ($pidAlive -or $targetAlive)\r\n")
                .append("}\r\n")
                .append("try {\r\n")
                .append("  Write-UpdateLog ('Iniciando atualização. EXE alvo: ' + $target)\r\n")
                .append("  $exitDeadline = [DateTime]::UtcNow.AddSeconds(120)\r\n")
                .append("  while (Test-OrcaBemRunning) {\r\n")
                .append("    if ([DateTime]::UtcNow -ge $exitDeadline) { throw 'O processo do OrçaBem continua aberto após 120 segundos.' }\r\n")
                .append("    Start-Sleep -Milliseconds 500\r\n")
                .append("  }\r\n")
                .append("  Write-UpdateLog 'Processos encerrados; preparando a substituição.'\r\n")
                .append("  $copyDeadline = [DateTime]::UtcNow.AddSeconds(45)\r\n")
                .append("  while ($true) {\r\n")
                .append("    try {\r\n")
                .append("      Copy-Item -LiteralPath $source -Destination $stage -Force\r\n")
                .append("      $stream = [IO.File]::OpenRead($stage)\r\n")
                .append("      try { if ($stream.Length -lt 2 -or $stream.ReadByte() -ne 77 -or $stream.ReadByte() -ne 90) { throw 'O executável baixado é inválido.' } } finally { $stream.Dispose() }\r\n")
                .append("      break\r\n")
                .append("    } catch {\r\n")
                .append("      if ([DateTime]::UtcNow -ge $copyDeadline) { throw ('Não foi possível preparar a nova versão: ' + $_.Exception.Message) }\r\n")
                .append("      Start-Sleep -Milliseconds 500\r\n")
                .append("    }\r\n")
                .append("  }\r\n")
                .append("  $replaceDeadline = [DateTime]::UtcNow.AddSeconds(90)\r\n")
                .append("  $replaced = $false\r\n")
                .append("  while (-not $replaced) {\r\n")
                .append("    try { [IO.File]::Replace($stage, $target, $backup, $true); $replaced = $true }\r\n")
                .append("    catch {\r\n")
                .append("      $lastReplaceError = $_.Exception.Message\r\n")
                .append("      if ([DateTime]::UtcNow -ge $replaceDeadline) { throw ('O Windows manteve o executável bloqueado: ' + $lastReplaceError) }\r\n")
                .append("      Start-Sleep -Milliseconds 500\r\n")
                .append("    }\r\n")
                .append("  }\r\n")
                .append("  Write-UpdateLog 'Executável instalado por substituição atômica.'\r\n")
                .append("  Start-Process -FilePath $target -WorkingDirectory ([IO.Path]::GetDirectoryName($target))\r\n")
                .append("  Write-UpdateLog 'Nova versão iniciada.'\r\n")
                .append("  Start-Sleep -Seconds 2\r\n")
                .append("  Remove-Item -LiteralPath $backup -Force -ErrorAction SilentlyContinue\r\n")
                .append("} catch {\r\n")
                .append("  $failure = $_.Exception.Message\r\n")
                .append("  try { Write-UpdateLog ('FALHA: ' + $failure) } catch { }\r\n")
                .append("  Remove-Item -LiteralPath $stage -Force -ErrorAction SilentlyContinue\r\n")
                .append("  try { Add-Type -AssemblyName System.Windows.Forms; [System.Windows.Forms.MessageBox]::Show(('Não foi possível concluir a instalação. A versão baixada permanece em:' + [Environment]::NewLine + $source + [Environment]::NewLine + [Environment]::NewLine + 'Detalhe: ' + $failure + [Environment]::NewLine + 'Registro: ' + $log), 'OrçaBem - atualização', 'OK', 'Warning') | Out-Null } catch { }\r\n")
                .append("} finally {\r\n")
                .append("  Remove-Item -LiteralPath $scriptPath -Force -ErrorAction SilentlyContinue\r\n")
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
