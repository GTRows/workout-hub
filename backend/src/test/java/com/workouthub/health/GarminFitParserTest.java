package com.workouthub.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class GarminFitParserTest {

    private static final long FIT_EPOCH_OFFSET = 631065600L;

    @Test
    void parsesSessionMessageWithStartElapsedAndAvgHr() throws Exception {
        long fitTs = 1_000_000_000L;
        long elapsedMs = 3_600_000L;
        int avgHr = 142;
        byte[] file = buildSessionFile(fitTs, elapsedMs, avgHr);

        GarminFitParser.Session s = new GarminFitParser()
                .parse(new ByteArrayInputStream(file));

        assertThat(s.startedAt().getEpochSecond()).isEqualTo(fitTs + FIT_EPOCH_OFFSET);
        assertThat(s.endedAt().toEpochMilli() - s.startedAt().toEpochMilli())
                .isEqualTo(elapsedMs);
        assertThat(s.avgHeartRateBpm()).isEqualTo((short) avgHr);
    }

    @Test
    void rejectsNonFitInputs() {
        byte[] junk = "NOT A FIT FILE.....".getBytes();
        assertThatThrownBy(() -> new GarminFitParser().parse(new ByteArrayInputStream(junk)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsFitFileWithoutSession() throws Exception {
        byte[] file = buildEmptyFitFile();
        assertThatThrownBy(() -> new GarminFitParser().parse(new ByteArrayInputStream(file)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Session");
    }

    private static byte[] buildSessionFile(long fitTs, long elapsedMs, int avgHr)
            throws Exception {
        ByteArrayOutputStream body = new ByteArrayOutputStream();

        // Definition record for session (global #18) at local message type 0.
        // Header byte: definition + local 0 -> 0x40
        body.write(0x40);
        body.write(0x00);                 // reserved
        body.write(0x00);                 // little-endian
        writeUint16LE(body, 18);          // global message number = session
        body.write(0x03);                 // 3 fields
        // field def 2: start_time, size 4, base type 0x86 (uint32)
        body.write(0x02); body.write(0x04); body.write(0x86);
        // field def 7: total_elapsed_time, size 4, base type 0x86
        body.write(0x07); body.write(0x04); body.write(0x86);
        // field def 16: avg_heart_rate, size 1, base type 0x02 (uint8)
        body.write(0x10); body.write(0x01); body.write(0x02);

        // Data record for local 0
        body.write(0x00);
        writeUint32LE(body, fitTs);
        writeUint32LE(body, elapsedMs);
        body.write(avgHr & 0xFF);

        return wrapWithHeaderAndCrc(body.toByteArray());
    }

    private static byte[] buildEmptyFitFile() throws Exception {
        return wrapWithHeaderAndCrc(new byte[0]);
    }

    private static byte[] wrapWithHeaderAndCrc(byte[] body) throws Exception {
        int dataSize = body.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(12);                 // header_size
        out.write(0x10);               // protocol_version
        writeUint16LE(out, 100);       // profile_version
        writeUint32LE(out, dataSize);
        out.write('.'); out.write('F'); out.write('I'); out.write('T');
        out.write(body);
        out.write(0x00); out.write(0x00); // trailing CRC (we don't validate)
        return out.toByteArray();
    }

    private static void writeUint16LE(ByteArrayOutputStream out, int v) {
        ByteBuffer b = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
        b.putShort((short) v);
        out.writeBytes(b.array());
    }

    private static void writeUint32LE(ByteArrayOutputStream out, long v) {
        ByteBuffer b = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt((int) v);
        out.writeBytes(b.array());
    }
}
