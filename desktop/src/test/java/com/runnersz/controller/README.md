# Regresión del selector de idioma

## Ejecución

Desde desktop, con JDK 21 o superior:

```powershell
mvn clean verify
java -cp "target/test-classes;target/runnersz-desktop.jar" com.runnersz.controller.LanguageSwitchRegression
```

En Linux usa `:` en lugar de `;`. El fat JAR debe generarse para el mismo sistema.
JavaFX necesita un entorno gráfico accesible; la prueba no muestra ventanas.
Maven compila el comprobador, pero su ejecución es explícita, con la segunda orden.

## Funcionamiento

Construye las vistas sin llamar a Stage.show y simula 12 selecciones consecutivas
de euskera, inglés y español. Comprueba botones, pestañas, prompts, accesibilidad,
Aceptar/Cancelar, menú de edición, preferencias y conservación de mensajes, rutas
y contraseña. Usa un proceso Java independiente con PreferencesFactory en memoria.
No lee la propiedad interna languageCode: esa lectura ocultaría la regresión.

## Dependencias y motivo

JavaFX y Java estándar del proyecto, sin añadir bibliotecas. La prueba falla en
el segundo cambio con el binding anterior y pasa con la revalidación corregida.
Protege el comportamiento multidioma y la accesibilidad del contrato §9.3 sin
modificar el reparto de responsabilidades de §5.4. No acredita cobertura JUnit/JaCoCo.
