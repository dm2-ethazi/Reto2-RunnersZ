package com.runnersz.controller;

/**
 * Estado del formulario que se conserva al recargar la vista al cambiar de idioma.
 *
 * @param inputPath ruta de entrada seleccionada
 * @param outputPath ruta de salida propuesta
 * @param password contraseña escrita, que nunca se guarda en disco
 * @param text texto de la pestaña Vigenère
 * @param key clave de Vigenère
 * @param result resultado de Vigenère
 * @param caesarText texto de la pestaña César
 * @param caesarResult resultado de César
 * @param shift desplazamiento de César seleccionado
 * @param tab pestaña activa
 * @author RunnersZ
 */
public record UiState(
    String inputPath,
    String outputPath,
    String password,
    String text,
    String key,
    String result,
    String caesarText,
    String caesarResult,
    Integer shift,
    int tab) {}
