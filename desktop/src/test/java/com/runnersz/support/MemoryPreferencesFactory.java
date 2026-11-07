package com.runnersz.support;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * Preferencias en memoria para que las pruebas no toquen el registro del usuario.
 *
 * @author Jairo y Alain
 */
public final class MemoryPreferencesFactory implements PreferencesFactory {
  private static final MemoryNode USER = new MemoryNode(null, "");
  private static final MemoryNode SYSTEM = new MemoryNode(null, "");

  @Override
  public Preferences userRoot() {
    return USER;
  }

  @Override
  public Preferences systemRoot() {
    return SYSTEM;
  }

  private static final class MemoryNode extends AbstractPreferences {
    private final Map<String, String> values = new HashMap<>();

    MemoryNode(AbstractPreferences parent, String name) {
      super(parent, name);
    }

    @Override
    protected void putSpi(String key, String value) {
      values.put(key, value);
    }

    @Override
    protected String getSpi(String key) {
      return values.get(key);
    }

    @Override
    protected void removeSpi(String key) {
      values.remove(key);
    }

    @Override
    protected void removeNodeSpi() {
      values.clear();
    }

    @Override
    protected String[] keysSpi() {
      return values.keySet().toArray(new String[0]);
    }

    @Override
    protected String[] childrenNamesSpi() {
      return new String[0];
    }

    @Override
    protected AbstractPreferences childSpi(String name) {
      return new MemoryNode(this, name);
    }

    @Override
    protected void flushSpi() {
      // En memoria no hay nada que volcar.
    }

    @Override
    protected void syncSpi() throws BackingStoreException {
      // En memoria no hay origen externo que sincronizar.
    }
  }
}
