package com.runnersz.reto2.desktop;

import static org.junit.jupiter.api.Assertions.*;
import static com.runnersz.reto2.desktop.controller.FxTestSupport.*;
import com.runnersz.reto2.desktop.controller.MainController;
import com.runnersz.reto2.desktop.service.BackendService;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AppLifecycleTest {
    @BeforeAll static void startToolkit() throws Exception { initialize(); }
    @Test void startsActualBackendAndClosesWindowSafely() throws Exception {
        App application = new App();
        Stage stage = fx(() -> { Stage window = new Stage(); application.start(window); return window; });
        try {
            MainController controller = fx(() -> field(application, "controller", MainController.class));
            BackendService backend = fx(() -> field(controller, "backend", BackendService.class));
            await(() -> backend.getBaseUrl() != null);
            assertTrue(fx(stage::isShowing));
            assertNotNull(fx(stage::getScene));
            fx(() -> { stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST)); return null; });
        } finally {
            fx(() -> { application.stop(); stage.close(); new App().stop(); return null; });
        }
    }
}
