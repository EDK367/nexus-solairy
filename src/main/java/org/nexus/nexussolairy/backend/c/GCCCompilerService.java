package org.nexus.nexussolairy.backend.c;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.function.Consumer;

public class GCCCompilerService {

    public static class CompilationResult {
        private final boolean success;
        private final String outputLog;
        private final File executableFile;

        public CompilationResult(boolean success, String outputLog, File executableFile) {
            this.success = success;
            this.outputLog = outputLog;
            this.executableFile = executableFile;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getOutputLog() {
            return outputLog;
        }

        public File getExecutableFile() {
            return executableFile;
        }
    }

    public CompilationResult compileAndRun(String cSourceCode, Consumer<String> logConsumer) {
        StringBuilder log = new StringBuilder();
        try {
            File tempCFile = File.createTempFile("solairy_c3d_", ".c");
            File tempExeFile = File.createTempFile("solairy_c3d_bin_", ".out");
            tempCFile.deleteOnExit();
            tempExeFile.deleteOnExit();

            Files.writeString(tempCFile.toPath(), cSourceCode);
            appendLog(log, logConsumer, "[GCC] Guardado archivo C fuente en: " + tempCFile.getAbsolutePath());

            ProcessBuilder pbCompile = new ProcessBuilder("gcc", "-O0", tempCFile.getAbsolutePath(), "-o", tempExeFile.getAbsolutePath(), "-lm");
            pbCompile.redirectErrorStream(true);
            Process processCompile = pbCompile.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(processCompile.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    appendLog(log, logConsumer, "[GCC] " + line);
                }
            }

            int exitCode = processCompile.waitFor();
            if (exitCode != 0) {
                appendLog(log, logConsumer, "[GCC] ERROR: Falló la compilación con GCC (código de salida " + exitCode + ").");
                return new CompilationResult(false, log.toString(), null);
            }

            appendLog(log, logConsumer, "[GCC] Compilación exitosa. Ejecutando binario generado...\n------------------------------------------------");

            ProcessBuilder pbRun = new ProcessBuilder(tempExeFile.getAbsolutePath());
            pbRun.redirectErrorStream(true);
            Process processRun = pbRun.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(processRun.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    appendLog(log, logConsumer, line);
                }
            }

            int runExitCode = processRun.waitFor();
            appendLog(log, logConsumer, "------------------------------------------------\n[GCC] Ejecución finalizada con código de salida: " + runExitCode);

            return new CompilationResult(true, log.toString(), tempExeFile);
        } catch (Exception e) {
            String errStr = "[GCC] Excepción durante compilación/ejecución: " + e.getMessage();
            appendLog(log, logConsumer, errStr);
            return new CompilationResult(false, log.toString(), null);
        }
    }

    private void appendLog(StringBuilder log, Consumer<String> logConsumer, String text) {
        log.append(text).append("\n");
        if (logConsumer != null) {
            logConsumer.accept(text);
        }
    }
}
