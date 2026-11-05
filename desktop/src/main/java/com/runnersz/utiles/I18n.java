package com.runnersz.utiles;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.file.AccessDeniedException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/** Catálogos UTF-8 y preferencia de idioma. No depende de JavaFX ni traduce datos del usuario. */
public final class I18n {
  private static final String BUNDLE = "com.runnersz.i18n.messages";
  private static final String NODE = "/com/runnersz";
  private static final Preferences PREFS = Preferences.userRoot().node(NODE);
  private static volatile Locale locale = supported(read());

  private I18n() {}

  public static Locale locale() {
    return locale;
  }

  public static void setLanguage(String language) {
    locale = supported(language);
    PREFS.put("language", locale.getLanguage());
    try {
      PREFS.flush();
    } catch (BackingStoreException ignored) {
      // La preferencia se guarda igualmente al cerrar la aplicación.
    }
  }

  /** Catálogo del idioma activo; lo usa el cargador de FXML. */
  public static ResourceBundle bundle() {
    return ResourceBundle.getBundle(BUNDLE, locale);
  }

  public static String text(String key, Object... arguments) {
    Locale current = locale;
    String pattern = ResourceBundle.getBundle(BUNDLE, current).getString(key);
    return new MessageFormat(pattern, current).format(arguments);
  }

  /** La clave se conserva hasta mostrar el fallo; cambiar de idioma no deja errores antiguos. */
  public static IOException failure(String key, Object... arguments) {
    return new TranslatedFailure(key, arguments);
  }

  public static IOException httpFailure(int status, String messageKey) {
    return failure("error.http", status, new Detail(messageKey));
  }

  private record Detail(String key) {
    @Override
    public String toString() {
      return text(key);
    }
  }

  public static IllegalArgumentException invalid(String key) {
    return new TranslatedArgument(key);
  }

  public static String errorMessage(Throwable error) {
    for (Throwable cause = error; cause != null; cause = cause.getCause()) {
      if (cause instanceof TranslatedFailure || cause instanceof TranslatedArgument) {
        return cause.getMessage();
      }
      if (cause instanceof AccessDeniedException) return text("error.access");
      if (cause instanceof NoSuchFileException) return text("error.notFound");
      if (cause instanceof InvalidPathException) return text("error.path");
      if (cause instanceof CharacterCodingException) return text("error.encoding");
    }
    // Los mensajes nativos dependen del SO y pueden contener datos sensibles: no se muestran.
    return text("error.generic");
  }

  private static String read() {
    String value = PREFS.get("language", null);
    if (value != null) return value;
    try {
      Preferences legacy = Preferences.userRoot().node("/org/example");
      return legacy.get("language", "es");
    } catch (IllegalStateException ex) {
      return "es";
    }
  }

  private static Locale supported(String language) {
    return Locale.forLanguageTag(
        switch (language == null ? "" : language) {
          case "eu", "en" -> language;
          default -> "es";
        });
  }

  private static final class TranslatedFailure extends IOException {
    private static final long serialVersionUID = 1L;

    private final String key;
    private final transient Object[] arguments;

    private TranslatedFailure(String key, Object[] arguments) {
      this.key = key;
      this.arguments = arguments.clone();
    }

    @Override
    public String getMessage() {
      return text(key, arguments);
    }
  }

  private static final class TranslatedArgument extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    private final String key;

    private TranslatedArgument(String key) {
      this.key = key;
    }

    @Override
    public String getMessage() {
      return text(key);
    }
  }
}
