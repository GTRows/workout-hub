package com.workouthub.twofa.dto;

public record SetupResponse(String secretBase32, String otpAuthUri) {}
