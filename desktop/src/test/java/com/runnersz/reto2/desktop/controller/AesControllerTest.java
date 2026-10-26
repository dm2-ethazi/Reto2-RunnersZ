package com.runnersz.reto2.desktop.controller;

import static org.junit.jupiter.api.Assertions.*;
import static com.runnersz.reto2.desktop.controller.FxTestSupport.*;

import java.nio.file.Path;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import com.runnersz.reto2.desktop.model.AesResult;
import com.runnersz.reto2.desktop.service.BackendException;
import com.runnersz.reto2.desktop.service.BackendService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AesControllerTest {
    @BeforeAll static void startToolkit() throws Exception { initialize(); }

    static class StubBackend extends BackendService {
        boolean failure;
        Boolean integrity = true;
        volatile boolean background;
        @Override public AesResult encryptFile(String input, String output, String password)
                throws BackendException {
            background = !Platform.isFxApplicationThread();
            if (failure) throw new BackendException("TEST", "Clave incorrecta");
            return new AesResult(output, 12, 12, "abc123", integrity);
        }
        @Override public AesResult decryptFile(String input, String output, String password)
                throws BackendException { return encryptFile(input, output, password); }
    }

    private AesController prepared(StubBackend backend) throws Exception {
        return fx(() -> {
            AesController controller = new AesController(backend);
            new Scene(controller.getView());
            controller.setBackendReady(true);
            field(controller, "inputField", TextField.class).setText("entrada.txt");
            field(controller, "outputField", TextField.class).setText("salida.enc");
            field(controller, "passwordField", TextField.class).setText("password");
            return controller;
        });
    }

    @Test void encryptsDecryptsAndHandlesAuthenticationErrors() throws Exception {
        StubBackend backend = new StubBackend();
        AesController controller = prepared(backend);
        Button encrypt = fx(() -> field(controller, "encryptButton", Button.class));
        Button decrypt = fx(() -> field(controller, "decryptButton", Button.class));
        Label status = fx(() -> field(controller, "statusLabel", Label.class));
        fx(() -> { encrypt.fire(); return null; });
        await(() -> !encrypt.isDisabled());
        assertTrue(backend.background);
        assertTrue(fx(() -> status.getText().startsWith("Cifrado completado")));
        fx(() -> { decrypt.fire(); return null; });
        await(() -> !decrypt.isDisabled());
        assertTrue(fx(() -> status.getText().startsWith("Integridad verificada")));
        backend.integrity = false;
        fx(() -> { decrypt.fire(); return null; });
        await(() -> !decrypt.isDisabled());
        assertTrue(fx(() -> status.getText().startsWith("Descifrado completado")));
        backend.failure = true;
        fx(() -> { decrypt.fire(); return null; });
        await(() -> !decrypt.isDisabled());
        assertTrue(fx(() -> status.getText().startsWith("Error:")));
    }

    @Test void rejectsIncompleteInputsAndProtectsOriginal() throws Exception {
        AesController controller = prepared(new StubBackend());
        int initial = dialogs.get();
        fx(() -> {
            controller.setBackendReady(false);
            invoke(controller, "runOperation", new Class<?>[]{boolean.class}, true);
            controller.setBackendReady(true);
            TextField input = field(controller, "inputField", TextField.class);
            TextField output = field(controller, "outputField", TextField.class);
            TextField password = field(controller, "passwordField", TextField.class);
            input.clear();
            field(controller, "encryptButton", Button.class).fire();
            input.setText("entrada.txt"); password.clear();
            field(controller, "encryptButton", Button.class).fire();
            password.setText("secret"); output.setText("entrada.txt");
            field(controller, "encryptButton", Button.class).fire();
            assertEquals("a.enc", invoke(controller, "suggestOutput",
                new Class<?>[]{Path.class, boolean.class}, Path.of("a"), true));
            assertEquals("a", invoke(controller, "suggestOutput",
                new Class<?>[]{Path.class, boolean.class}, Path.of("a.enc"), false));
            assertEquals("a.recuperado", invoke(controller, "suggestOutput",
                new Class<?>[]{Path.class, boolean.class}, Path.of("a"), false));
            return null;
        });
        assertTrue(dialogs.get() >= initial + 4);
    }
}
