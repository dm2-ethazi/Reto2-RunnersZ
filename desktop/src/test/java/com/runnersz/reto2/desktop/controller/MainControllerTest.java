package com.runnersz.reto2.desktop.controller;

import static org.junit.jupiter.api.Assertions.*;
import static com.runnersz.reto2.desktop.controller.FxTestSupport.*;
import com.runnersz.reto2.desktop.service.BackendException;
import com.runnersz.reto2.desktop.service.BackendLauncher;
import com.runnersz.reto2.desktop.service.BackendService;
import com.runnersz.reto2.desktop.service.FileTextService;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MainControllerTest {
    @BeforeAll static void startToolkit() throws Exception { initialize(); }
    static class StubLauncher extends BackendLauncher {
        final AtomicInteger starts = new AtomicInteger();
        final AtomicInteger stops = new AtomicInteger();
        boolean failOnce;
        @Override public String start() throws BackendException {
            int attempt = starts.incrementAndGet();
            if (failOnce && attempt == 1) throw new BackendException("TEST", "Arranque fallido");
            return "http://127.0.0.1:8765";
        }
        @Override public void stop() { stops.incrementAndGet(); }
    }

    @Test void connectsRestartsAndShutsDownOnlyOnce() throws Exception {
        StubLauncher launcher = new StubLauncher();
        BackendService backend = new BackendService();
        MainController controller = fx(() -> {
            MainController value = new MainController(launcher, backend, new FileTextService());
            new Scene(value.getView());
            assertEquals(2, ((TabPane)((BorderPane)value.getView()).getCenter()).getTabs().size());
            value.connectBackend(); return value;
        });
        await(() -> backend.getBaseUrl() != null);
        fx(() -> { field(controller, "reconnectButton", Button.class).fire(); return null; });
        await(() -> launcher.starts.get() >= 2 && backend.getBaseUrl() != null);
        int previous = launcher.stops.get();
        fx(() -> { controller.shutdown(); controller.shutdown(); controller.connectBackend(); return null; });
        assertEquals(previous + 1, launcher.stops.get());
    }

    @Test void retriesAStartupFailureWithoutExitingApplication() throws Exception {
        StubLauncher launcher = new StubLauncher(); launcher.failOnce = true;
        BackendService backend = new BackendService();
        retryDialogs = true;
        MainController controller = fx(() -> new MainController(launcher, backend, new FileTextService()));
        try {
            fx(() -> { controller.connectBackend(); return null; });
            await(() -> backend.getBaseUrl() != null);
            assertEquals(2, launcher.starts.get());
        } finally {
            retryDialogs = false;
            fx(() -> { controller.shutdown(); return null; });
        }
    }
}
