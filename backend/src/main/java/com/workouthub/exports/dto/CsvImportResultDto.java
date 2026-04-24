package com.workouthub.exports.dto;

import java.util.List;

public record CsvImportResultDto(
        int sessionsInserted,
        int setsInserted,
        List<String> unmatchedExerciseNames,
        List<String> warnings) {}
