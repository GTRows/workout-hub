package com.workouthub.health;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class AppleHealthParser {

    public static final String TYPE_BODY_MASS = "HKQuantityTypeIdentifierBodyMass";
    public static final String WORKOUT_PREFIX = "HKWorkoutActivityType";

    private static final DateTimeFormatter APPLE_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");

    public ParsedHealth parse(InputStream raw) throws IOException, XMLStreamException {
        InputStream stream = unwrapZipIfNeeded(raw);
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", Boolean.FALSE);
        XMLStreamReader reader = factory.createXMLStreamReader(stream);

        List<ParsedHealth.BodyMassRecord> bodyMass = new ArrayList<>();
        List<ParsedHealth.WorkoutRecord> workouts = new ArrayList<>();

        while (reader.hasNext()) {
            int event = reader.next();
            if (event != XMLStreamConstants.START_ELEMENT) continue;
            String name = reader.getLocalName();
            if ("Record".equals(name)) {
                String type = reader.getAttributeValue(null, "type");
                if (TYPE_BODY_MASS.equals(type)) {
                    ParsedHealth.BodyMassRecord rec = parseBodyMass(reader);
                    if (rec != null) bodyMass.add(rec);
                }
            } else if ("Workout".equals(name)) {
                ParsedHealth.WorkoutRecord w = parseWorkout(reader);
                if (w != null) workouts.add(w);
            }
        }
        reader.close();
        return new ParsedHealth(bodyMass, workouts);
    }

    private InputStream unwrapZipIfNeeded(InputStream raw) throws IOException {
        if (!raw.markSupported()) raw = new java.io.BufferedInputStream(raw);
        raw.mark(4);
        byte[] header = new byte[4];
        int read = raw.read(header);
        raw.reset();
        if (read == 4 && header[0] == 0x50 && header[1] == 0x4B
                && header[2] == 0x03 && header[3] == 0x04) {
            ZipInputStream zip = new ZipInputStream(raw);
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().endsWith("export.xml")) return zip;
            }
            throw new IOException("ZIP did not contain export.xml");
        }
        return raw;
    }

    private ParsedHealth.BodyMassRecord parseBodyMass(XMLStreamReader r) {
        String unit = r.getAttributeValue(null, "unit");
        String value = r.getAttributeValue(null, "value");
        String startDate = r.getAttributeValue(null, "startDate");
        if (value == null || startDate == null) return null;
        BigDecimal kg = new BigDecimal(value);
        if ("lb".equalsIgnoreCase(unit)) {
            kg = kg.multiply(new BigDecimal("0.45359237")).setScale(2, java.math.RoundingMode.HALF_UP);
        }
        Instant when = parseAppleInstant(startDate);
        if (when == null) return null;
        LocalDate date = when.atZone(ZoneOffset.UTC).toLocalDate();
        return new ParsedHealth.BodyMassRecord(date, kg);
    }

    private ParsedHealth.WorkoutRecord parseWorkout(XMLStreamReader r) {
        String activityType = r.getAttributeValue(null, "workoutActivityType");
        String startDate = r.getAttributeValue(null, "startDate");
        String endDate = r.getAttributeValue(null, "endDate");
        if (activityType == null || startDate == null) return null;
        Instant start = parseAppleInstant(startDate);
        Instant end = endDate == null ? null : parseAppleInstant(endDate);
        if (start == null) return null;
        String label = activityType.startsWith(WORKOUT_PREFIX)
                ? activityType.substring(WORKOUT_PREFIX.length())
                : activityType;
        return new ParsedHealth.WorkoutRecord(
                activityType, start, end, "Imported from Apple Health: " + label);
    }

    private Instant parseAppleInstant(String s) {
        try {
            return OffsetDateTime.parse(s, APPLE_DATE).toInstant();
        } catch (Exception ignored) {
            return null;
        }
    }
}
