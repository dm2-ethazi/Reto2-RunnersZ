package com.runnersz.reto2.desktop.controller;

import static org.junit.jupiter.api.Assertions.*;
import static com.runnersz.reto2.desktop.controller.FxTestSupport.*;
import com.runnersz.reto2.desktop.service.BackendException;
import com.runnersz.reto2.desktop.service.BackendService;
import com.runnersz.reto2.desktop.service.FileTextService;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class VigenereControllerTest {
    @BeforeAll static void startToolkit() throws Exception { initialize(); }
    static class StubBackend extends BackendService {
        boolean failure;
        volatile boolean background;
        @Override public String vigenere(String text, String key, boolean encrypt)
                throws BackendException {
            background = !Platform.isFxApplicationThread();
            if (failure) throw new BackendException("TEST", "Clave no valida");
            return encrypt ? "CIFRADO" : "texto";
        }
    }

    @Test void transformsMessagesOffUiThreadAndReportsErrors() throws Exception {
        StubBackend backend = new StubBackend();
        VigenereController controller = fx(() -> new VigenereController(backend, new FileTextService()));
        Button encrypt = fx(() -> field(controller, "encryptButton", Button.class));
        Button decrypt = fx(() -> field(controller, "decryptButton", Button.class));
        TextArea result = fx(() -> field(controller, "resultArea", TextArea.class));
        fx(() -> {
            controller.getView(); controller.setBackendReady(true);
            field(controller, "keyField", TextField.class).setText("CLAVE");
            field(controller, "inputArea", TextArea.class).setText("texto");
            encrypt.fire(); return null;
        });
        await(() -> !encrypt.isDisabled());
        assertEquals("CIFRADO", fx(result::getText));
        assertTrue(backend.background);
        fx(() -> { decrypt.fire(); return null; });
        await(() -> !decrypt.isDisabled());
        assertEquals("texto", fx(result::getText));
        backend.failure = true;
        fx(() -> { encrypt.fire(); return null; });
        await(() -> !encrypt.isDisabled());
        assertEquals("texto", fx(result::getText));
    }

    @Test void rejectsUnavailableBackendEmptyKeyAndEmptyResult() throws Exception {
        VigenereController controller = fx(() -> new VigenereController(new StubBackend(), new FileTextService()));
        int initial = dialogs.get();
        fx(() -> {
            invoke(controller, "runOperation", new Class<?>[]{boolean.class}, true);
            controller.setBackendReady(true);
            field(controller, "encryptButton", Button.class).fire();
            field(controller, "saveButton", Button.class).fire();
            return null;
        });
        assertTrue(dialogs.get() >= initial + 3);
    }

    @Test void unwrapsMessagesAndLetsUserChooseRetry() throws Exception {
        assertEquals("root", UiAlerts.messageOf(new RuntimeException((String)null, new Exception("root"))));
        assertTrue(UiAlerts.messageOf(new Exception()).contains("Exception"));
        retryDialogs = true;
        try { assertTrue(fx(() -> UiAlerts.retry(null, "Reintento", "Prueba"))); }
        finally { retryDialogs = false; }
        assertFalse(fx(() -> UiAlerts.retry(null, "Cerrar", "Prueba")));
    }
}
