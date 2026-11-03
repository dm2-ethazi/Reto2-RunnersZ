package com.runnersz.model;

/** Contenido devuelto por la API; al descifrar ya se comprobó X-Integrity. */
public record FileResult(byte[] bytes) {}
