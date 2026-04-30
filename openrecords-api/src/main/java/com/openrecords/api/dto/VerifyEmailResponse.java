package com.openrecords.api.dto;

public record VerifyEmailResponse(
    String email,
    String message
) {}