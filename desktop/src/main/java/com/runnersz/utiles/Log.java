package com.runnersz.utiles;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fachada mínima de SLF4J que enmascara credenciales antes de escribir. Toda la aplicación registra
 * a través de esta clase para que ni el token ni la contraseña AES lleguen nunca al fichero.
 *
 * @author Jairo y Alain
 */
public final class Log {
  private final Logger delegate;

  private Log(Class<?> type) {
    delegate = LoggerFactory.getLogger(type);
  }

  /**
   * Registro asociado a una clase.
   *
   * @param type clase que firma las líneas
   * @return la fachada correspondiente
   */
  public static Log of(Class<?> type) {
    return new Log(type);
  }

  /**
   * Traza de depuración, solo visible con {@code -Drunnersz.log.level=DEBUG}.
   *
   * @param format mensaje con parámetros al estilo de {@link String#format}
   * @param args valores del mensaje
   */
  public void debug(String format, Object... args) {
    if (delegate.isDebugEnabled()) delegate.debug(message(format, args));
  }

  /**
   * Información de funcionamiento normal.
   *
   * @param format mensaje con parámetros al estilo de {@link String#format}
   * @param args valores del mensaje
   */
  public void info(String format, Object... args) {
    if (delegate.isInfoEnabled()) delegate.info(message(format, args));
  }

  /**
   * Situación recuperable que conviene revisar.
   *
   * @param format mensaje con parámetros al estilo de {@link String#format}
   * @param args valores del mensaje
   */
  public void warn(String format, Object... args) {
    if (delegate.isWarnEnabled()) delegate.warn(message(format, args));
  }

  /**
   * Fallo con su traza completa, que es lo que el diálogo de la interfaz no muestra.
   *
   * @param format mensaje con parámetros al estilo de {@link String#format}
   * @param failure error que se registra con su pila
   * @param args valores del mensaje
   */
  public void error(String format, Throwable failure, Object... args) {
    if (delegate.isErrorEnabled()) delegate.error(message(format, args), failure);
  }

  private static String message(String format, Object... args) {
    String text = format == null ? "" : format;
    if (args != null && args.length > 0) text = String.format(Locale.ROOT, text, args);
    return Logs.redact(text);
  }
}
