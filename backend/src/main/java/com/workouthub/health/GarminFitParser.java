package com.workouthub.health;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hand-rolled .fit binary parser. Recognizes only the file header plus
 * Session messages (global #18) — enough to seed a WorkoutSession with
 * start, end, and average HR. Anything else is skipped via field-size
 * arithmetic from the matching definition record.
 */
public class GarminFitParser {

    private static final long FIT_EPOCH_OFFSET = 631065600L;
    private static final int GLOBAL_MSG_SESSION = 18;

    public record Session(Instant startedAt, Instant endedAt, Short avgHeartRateBpm) {}

    public Session parse(InputStream stream) throws IOException {
        byte[] all = stream.readAllBytes();
        if (all.length < 14 || all[8] != '.' || all[9] != 'F'
                || all[10] != 'I' || all[11] != 'T') {
            throw new IllegalArgumentException("Not a FIT file");
        }
        int headerSize = all[0] & 0xFF;
        if (headerSize != 12 && headerSize != 14) {
            throw new IllegalArgumentException("Unexpected FIT header size: " + headerSize);
        }
        ByteBuffer headerBuf = ByteBuffer.wrap(all, 0, headerSize)
                .order(ByteOrder.LITTLE_ENDIAN);
        long dataSize = headerBuf.getInt(4) & 0xFFFFFFFFL;
        int dataEnd = headerSize + (int) dataSize;
        if (dataEnd > all.length) {
            throw new IllegalArgumentException("FIT data_size exceeds file length");
        }

        Map<Integer, Definition> defs = new HashMap<>();
        Session session = null;
        int pos = headerSize;
        while (pos < dataEnd) {
            int rh = all[pos++] & 0xFF;
            boolean compressed = (rh & 0x80) != 0;
            if (compressed) {
                int localMsg = (rh >> 5) & 0x03;
                Definition d = defs.get(localMsg);
                if (d == null) throw new IllegalArgumentException("Compressed without def");
                pos += d.totalSize;
                continue;
            }
            boolean isDefinition = (rh & 0x40) != 0;
            int localMsg = rh & 0x0F;
            if (isDefinition) {
                Definition d = readDefinition(all, pos);
                pos += d.headerBytes;
                defs.put(localMsg, d);
            } else {
                Definition d = defs.get(localMsg);
                if (d == null) throw new IllegalArgumentException("Data without def");
                if (d.globalMsg == GLOBAL_MSG_SESSION && session == null) {
                    session = parseSession(all, pos, d);
                }
                pos += d.totalSize;
            }
        }
        if (session == null) {
            throw new IllegalArgumentException("FIT file did not contain a Session message");
        }
        return session;
    }

    private static Definition readDefinition(byte[] all, int pos) {
        int reserved = all[pos] & 0xFF;
        int arch = all[pos + 1] & 0xFF;
        ByteOrder order = arch == 0 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        ByteBuffer mb = ByteBuffer.wrap(all, pos + 2, 2).order(order);
        int globalMsg = mb.getShort() & 0xFFFF;
        int numFields = all[pos + 4] & 0xFF;
        List<Field> fields = new ArrayList<>(numFields);
        int totalSize = 0;
        for (int i = 0; i < numFields; i++) {
            int fpos = pos + 5 + i * 3;
            int defNum = all[fpos] & 0xFF;
            int size = all[fpos + 1] & 0xFF;
            int baseType = all[fpos + 2] & 0xFF;
            fields.add(new Field(defNum, size, baseType, totalSize));
            totalSize += size;
        }
        int headerBytes = 5 + numFields * 3;
        if (reserved != 0) {
            // Some FIT writers stuff a developer-fields count after the standard
            // fields. We don't need their values; just skip past them.
            int devCount = all[pos + headerBytes] & 0xFF;
            headerBytes += 1 + devCount * 3;
            for (int i = 0; i < devCount; i++) {
                int devSize = all[pos + 5 + numFields * 3 + 1 + i * 3 + 1] & 0xFF;
                totalSize += devSize;
            }
        }
        return new Definition(globalMsg, order, fields, totalSize, headerBytes);
    }

    private static Session parseSession(byte[] all, int pos, Definition d) {
        Long fitStart = null;
        Long elapsedMs = null;
        Long timerMs = null;
        Short avgHr = null;
        for (Field f : d.fields) {
            int fpos = pos + f.offset;
            switch (f.defNum) {
                case 2 -> fitStart = readU32(all, fpos, d.order);
                case 7 -> elapsedMs = readU32(all, fpos, d.order);
                case 8 -> timerMs = readU32(all, fpos, d.order);
                case 16 -> {
                    int hr = all[fpos] & 0xFF;
                    if (hr != 0xFF) avgHr = (short) hr;
                }
                default -> {
                    // ignore unrecognized field, size already accounted for
                }
            }
        }
        if (fitStart == null) return null;
        Instant start = Instant.ofEpochSecond(fitStart + FIT_EPOCH_OFFSET);
        long durMs = elapsedMs != null ? elapsedMs : (timerMs != null ? timerMs : 0L);
        Instant end = durMs > 0 ? start.plusMillis(durMs) : null;
        return new Session(start, end, avgHr);
    }

    private static long readU32(byte[] all, int pos, ByteOrder order) {
        ByteBuffer b = ByteBuffer.wrap(all, pos, 4).order(order);
        return b.getInt() & 0xFFFFFFFFL;
    }

    private record Field(int defNum, int size, int baseType, int offset) {}

    private record Definition(
            int globalMsg,
            ByteOrder order,
            List<Field> fields,
            int totalSize,
            int headerBytes) {}
}
