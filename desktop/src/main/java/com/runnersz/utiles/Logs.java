package com.runnersz.utiles;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.LoggerFactory;

/**
 * Registro de la aplicación en un fichero que vive fuera del repositorio y nunca se empaqueta.
 *
 * <p>La carpeta se elige en este orden: la propiedad {@code -Drunnersz.logs.dir}, la variable de
 * entorno {@code RUNNERSZ_LOGS_DIR} y, si no hay ninguna, la carpeta de registros del usuario:
 * {@code %LOCALAPPDATA%\RunnersZ\logs} en Windows, {@code ~/Library/Logs/RunnersZ} en macOS y
 * {@code $XDG_STATE_HOME/runnersz/logs} (o {@code ~/.local/state/runnersz/logs}) en Linux. Nunca se
 * escribe en el directorio de trabajo ni junto al JAR: al instalar la aplicación en una carpeta de
 * solo lectura el registro fallaría.
 *
 * <p>El fichero {@code runnersz.log} rota cada 5 MB o cada día ({@code runnersz.2026-11-07.1.log.gz}),
 * conserva una semana y no pasa de 50 MB. El nivel por omisión es {@code INFO} y se sube a
 * {@code DEBUG} con {@code -Drunnersz.log.level=DEBUG}.
 *
 * @author Jairo y Alain
 */
public final class Logs {
  /** Propiedad para elegir la carpeta del registro (pruebas, soporte y demostraciones). */
  public static final String DIR_PROPERTY = "runnersz.logs.dir";
  /** Variable de entorno equivalente a {@link #DIR_PROPERTY}. */
  public static final String DIR_ENV = "RUNNERSZ_LOGS_DIR";
  /** Propiedad con el nivel mínimo que se escribe. */
  public static final String LEVEL_PROPERTY = "runnersz.log.level";
  /** Propiedad para silenciar la copia por consola ({@code -Drunnersz.logs.console=false}). */
  public static final String CONSOLE_PROPERTY = "runnersz.logs.console";
  /** Nombre del fichero activo dentro de la carpeta del registro. */
  public static final String FILE_NAME = "runnersz.log";

  static final String PATTERN =
      "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n";
  /** Líneas que muestra el visor del registro. */
  public static final int VIEWER_LINES = 200;

  private static final String APP = "RunnersZ";
  private static final String APP_LINUX = "runnersz";
  private static final Pattern BEARER =
      Pattern.compile("(?i)(bearer\\s+)[A-Za-z0-9._~+/=-]{4,}");
  private static final Pattern CREDENTIAL =
      Pattern.compile(
          "(?i)\\b(token|password|contrasena|contraseña|clave)\\b(\\s*[=:]\\s*)([^\\s,;]+)");
  private static final Object LOCK = new Object();

  private static Path file;

  private Logs() {}

  /**
   * Configura el registro una sola vez y devuelve el fichero donde se escribe.
   *
   * @return ruta absoluta del {@code runnersz.log} activo
   */
  public static Path start() {
    synchronized (LOCK) {
      if (file == null) file = configureOrFallback();
      return file;
    }
  }

  /**
   * Fichero del registro, aunque todavía no se haya llamado a {@link #start()}.
   *
   * @return ruta del fichero activo o el que se usaría por omisión
   */
  public static Path file() {
    Path current;
    synchronized (LOCK) {
      current = file;
    }
    return current != null ? current : directory().resolve(FILE_NAME);
  }

  /** Carpeta del registro según la propiedad, el entorno o el sistema operativo. */
  public static Path directory() {
    String override = System.getProperty(DIR_PROPERTY);
    if (override == null || override.isBlank()) override = System.getenv(DIR_ENV);
    if (override != null && !override.isBlank()) {
      return Path.of(override).toAbsolutePath().normalize();
    }
    return defaultDirectory(
        System.getProperty("os.name", ""),
        System.getenv(),
        System.getProperty("user.home", "."));
  }

  /**
   * Carpeta del registro propia de cada sistema operativo.
   *
   * @param os nombre del sistema, por ejemplo {@code Windows 11}
   * @param env variables de entorno del proceso
   * @param home carpeta personal del usuario
   * @return carpeta donde se guardará {@code runnersz.log}
   */
  static Path defaultDirectory(String os, Map<String, String> env, String home) {
    String system = os == null ? "" : os.toLowerCase(Locale.ROOT);
    if (system.contains("win")) {
      String local = env.get("LOCALAPPDATA");
      Path base = blank(local) ? Path.of(home, "AppData", "Local") : Path.of(local);
      return base.resolve(APP).resolve("logs");
    }
    if (system.contains("mac")) return Path.of(home, "Library", "Logs", APP);
    String state = env.get("XDG_STATE_HOME");
    Path base = blank(state) ? Path.of(home, ".local", "state") : Path.of(state);
    return base.resolve(APP_LINUX).resolve("logs");
  }

  /**
   * Abre el fichero rotado en la carpeta indicada. No se llama desde fuera de {@link #start()}.
   *
   * @param folder carpeta del registro
   * @return ruta del fichero activo
   * @throws IOException si la carpeta no se puede crear
   */
  static Path install(Path folder) throws IOException {
    Path target = folder.resolve(FILE_NAME);
    configure(folder, target);
    return target;
  }

  /**
   * Últimas líneas del registro para el visor de la aplicación.
   *
   * @param path fichero que se quiere leer
   * @param max número máximo de líneas
   * @return las últimas líneas unidas por saltos de línea, o cadena vacía si no hay fichero
   */
  public static String lastLines(Path path, int max) {
    if (path == null || max <= 0) return "";
    Deque<String> lines = new ArrayDeque<>(max);
    try (Stream<String> stream = Files.lines(path, StandardCharsets.UTF_8)) {
      stream.forEach(
          line -> {
            if (lines.size() == max) lines.removeFirst();
            lines.addLast(line);
          });
    } catch (IOException | RuntimeException ex) {
      Log.of(Logs.class).warn("No se pudo leer el registro " + path, ex);
      return "";
    }
    return String.join(System.lineSeparator(), lines);
  }

  /**
   * Enmascara credenciales antes de escribir una línea. El token nunca llega al fichero.
   *
   * @param message texto tal cual lo compone la aplicación
   * @return el texto con {@code ***} en lugar de tokens y contraseñas
   */
  static String redact(String message) {
    if (message == null) return "";
    String withoutBearer = BEARER.matcher(message).replaceAll("$1***");
    return CREDENTIAL.matcher(withoutBearer).replaceAll("$1$2***");
  }

  /** Olvida la configuración anterior; solo para pruebas. */
  static void reset() {
    synchronized (LOCK) {
      file = null;
    }
  }

  /**
   * Cierra el fichero y olvida la configuración. Las pruebas lo necesitan para poder borrar la
   * carpeta temporal: Windows no elimina un fichero que sigue abierto.
   */
  static void stop() {
    try {
      if (LoggerFactory.getILoggerFactory() instanceof LoggerContext context) context.reset();
    } catch (RuntimeException ignored) {
      // Sin Logback no hay nada abierto que cerrar.
    }
    reset();
  }

  private static Path configureOrFallback() {
    Path folder = directory();
    Path target = folder.resolve(FILE_NAME);
    try {
      configure(folder, target);
    } catch (IOException | RuntimeException ex) {
      target = emergency(folder, ex);
    }
    Thread.setDefaultUncaughtExceptionHandler(
        (thread, error) ->
            Log.of(Logs.class)
                .error("Excepción no capturada en el hilo " + thread.getName(), error));
    return target;
  }

  private static Path emergency(Path folder, Throwable cause) {
    try {
      Path temp = Files.createTempDirectory("runnersz-logs");
      Path target = temp.resolve(FILE_NAME);
      configure(temp, target);
      Log.of(Logs.class).warn("No se pudo escribir en " + folder + "; se usa " + temp, cause);
      return target;
    } catch (IOException | RuntimeException ex) {
      consoleOnly();
      Log.of(Logs.class).error("No se pudo abrir el registro en " + folder, ex);
      return folder.resolve(FILE_NAME);
    }
  }

  private static void configure(Path folder, Path target) throws IOException {
    Files.createDirectories(folder);
    LoggerContext context = context();
    context.reset();

    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(context);
    encoder.setPattern(PATTERN);
    encoder.setCharset(StandardCharsets.UTF_8);
    encoder.start();

    RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
    appender.setContext(context);
    appender.setName("FILE");
    appender.setFile(target.toString());

    SizeAndTimeBasedRollingPolicy<ILoggingEvent> rolling =
        new SizeAndTimeBasedRollingPolicy<>();
    rolling.setContext(context);
    rolling.setParent(appender);
    rolling.setFileNamePattern(
        folder.resolve("runnersz.%d{yyyy-MM-dd}.%i.log.gz").toString());
    rolling.setMaxFileSize(FileSize.valueOf("5MB"));
    rolling.setMaxHistory(7);
    rolling.setTotalSizeCap(FileSize.valueOf("50MB"));
    rolling.start();

    appender.setRollingPolicy(rolling);
    appender.setEncoder(encoder);
    appender.start();

    Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
    root.detachAndStopAllAppenders();
    root.setLevel(level());
    root.addAppender(appender);
    if (consoleEnabled()) root.addAppender(console(context));
    context.getLogger("ch.qos.logback").setLevel(Level.WARN);
  }

  private static void consoleOnly() {
    try {
      Logger root = context().getLogger(Logger.ROOT_LOGGER_NAME);
      root.detachAndStopAllAppenders();
      root.setLevel(level());
      root.addAppender(console(context()));
    } catch (RuntimeException ignored) {
      // Sin registro no hay más que hacer: la aplicación sigue funcionando.
    }
  }

  private static ConsoleAppender<ILoggingEvent> console(LoggerContext context) {
    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(context);
    encoder.setPattern(PATTERN);
    encoder.setCharset(StandardCharsets.UTF_8);
    encoder.start();
    ConsoleAppender<ILoggingEvent> console = new ConsoleAppender<>();
    console.setContext(context);
    console.setName("CONSOLE");
    console.setEncoder(encoder);
    console.start();
    return console;
  }

  private static LoggerContext context() {
    if (LoggerFactory.getILoggerFactory() instanceof LoggerContext found) return found;
    throw new IllegalStateException("SLF4J no está respaldado por Logback");
  }

  private static Level level() {
    return Level.toLevel(System.getProperty(LEVEL_PROPERTY, "INFO"), Level.INFO);
  }

  private static boolean consoleEnabled() {
    return !"false".equalsIgnoreCase(System.getProperty(CONSOLE_PROPERTY, "true"));
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
