package com.runnersz.controller;

/** Estado del formulario que se conserva al recargar la vista al cambiar de idioma. */
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
