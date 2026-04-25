package com.workouthub.health;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class GoogleFitParser {

    public static final String TYPE_WEIGHT = "com.google.weight";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public ParsedHealth parse(InputStream stream) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(stream);
        List<ParsedHealth.BodyMassRecord> bodyMass = new ArrayList<>();
        List<ParsedHealth.WorkoutRecord> workouts = new ArrayList<>();

        if (root.isArray()) {
            for (JsonNode session : root) collectSession(session, workouts);
        } else if (root.isObject()) {
            JsonNode points = root.path("dataPoint");
            if (points.isArray()) {
                for (JsonNode p : points) collectPoint(p, bodyMass);
            }
            JsonNode sessions = root.path("session");
            if (sessions.isArray()) {
                for (JsonNode s : sessions) collectSession(s, workouts);
            } else if (root.has("startTimeMillis") && root.has("endTimeMillis")) {
                collectSession(root, workouts);
            }
        }
        return new ParsedHealth(bodyMass, workouts);
    }

    private void collectPoint(JsonNode point, List<ParsedHealth.BodyMassRecord> out) {
        String type = point.path("dataTypeName").asText("");
        if (!TYPE_WEIGHT.equals(type)) return;
        JsonNode values = point.path("fitValue");
        if (!values.isArray() || values.isEmpty()) values = point.path("value");
        if (!values.isArray() || values.isEmpty()) return;
        JsonNode first = values.get(0);
        JsonNode fp = first.path("value").path("fpVal");
        if (fp.isMissingNode()) fp = first.path("fpVal");
        if (!fp.isNumber()) return;
        BigDecimal kg = BigDecimal.valueOf(fp.asDouble())
                .setScale(2, java.math.RoundingMode.HALF_UP);
        long startNanos = point.path("startTimeNanos").asLong();
        if (startNanos == 0) return;
        Instant when = Instant.ofEpochSecond(startNanos / 1_000_000_000L,
                startNanos % 1_000_000_000L);
        out.add(new ParsedHealth.BodyMassRecord(
                when.atZone(ZoneOffset.UTC).toLocalDate(), kg));
    }

    private void collectSession(JsonNode session, List<ParsedHealth.WorkoutRecord> out) {
        long startMillis = session.path("startTimeMillis").asLong();
        long endMillis = session.path("endTimeMillis").asLong();
        if (startMillis == 0) return;
        Instant start = Instant.ofEpochMilli(startMillis);
        Instant end = endMillis == 0 ? null : Instant.ofEpochMilli(endMillis);
        String name = session.path("name").asText("");
        String activityType = session.path("activityType").asText("0");
        String label = name.isBlank() ? "activityType=" + activityType : name;
        out.add(new ParsedHealth.WorkoutRecord(
                "google.fit." + activityType, start, end,
                "Imported from Google Fit: " + label));
    }
}
