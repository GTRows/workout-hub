package com.workouthub.exports;

import java.util.ArrayList;
import java.util.List;

/**
 * Tiny RFC 4180 reader. Handles quoted fields, escaped double-quotes
 * inside quoted fields, and CRLF / LF line endings. Good enough for the
 * Strong/Hevy CSV shape; not a general-purpose parser.
 */
final class CsvRowReader {

    private CsvRowReader() {}

    static List<String[]> readAll(String csv) {
        List<String[]> rows = new ArrayList<>();
        if (csv == null || csv.isEmpty()) return rows;

        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;
        while (i < csv.length()) {
            char c = csv.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else {
                if (c == ',') {
                    current.add(field.toString());
                    field.setLength(0);
                } else if (c == '\n' || c == '\r') {
                    if (c == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') {
                        i++;
                    }
                    current.add(field.toString());
                    field.setLength(0);
                    if (!(current.size() == 1 && current.get(0).isEmpty())) {
                        rows.add(current.toArray(new String[0]));
                    }
                    current = new ArrayList<>();
                } else if (c == '"' && field.length() == 0) {
                    inQuotes = true;
                } else {
                    field.append(c);
                }
            }
            i++;
        }
        if (field.length() > 0 || !current.isEmpty()) {
            current.add(field.toString());
            if (!(current.size() == 1 && current.get(0).isEmpty())) {
                rows.add(current.toArray(new String[0]));
            }
        }
        return rows;
    }
}
