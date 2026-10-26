package com.runnersz.reto2.desktop.model;

public record AesResult(
        String outputPath,
        long inputBytes,
        long outputBytes,
        String sha256,
        Boolean integrityVerified) {}
