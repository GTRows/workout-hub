package com.workouthub.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class GarminFitImportIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "GarminSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;

    @Test
    void importsSessionFromFitFile() throws Exception {
        SeededUser u = helpers.seed(
                "fit-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        byte[] file = buildSessionFile(1_000_000_000L, 3_600_000L, 142);

        mvc.perform(multipart("/api/health/import/fit")
                        .file(new MockMultipartFile(
                                "file", "ride.fit", "application/octet-stream", file))
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workoutsImported").value(1));
    }

    @Test
    void rejectsBogusFitFiles() throws Exception {
        SeededUser u = helpers.seed(
                "fit2-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(multipart("/api/health/import/fit")
                        .file(new MockMultipartFile(
                                "file", "junk.fit", "application/octet-stream",
                                "NOT A FIT FILE.....".getBytes()))
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().is4xxClientError());
    }

    private static byte[] buildSessionFile(long fitTs, long elapsedMs, int avgHr)
            throws Exception {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(0x40);
        body.write(0x00);
        body.write(0x00);
        writeUint16LE(body, 18);
        body.write(0x03);
        body.write(0x02); body.write(0x04); body.write(0x86);
        body.write(0x07); body.write(0x04); body.write(0x86);
        body.write(0x10); body.write(0x01); body.write(0x02);

        body.write(0x00);
        writeUint32LE(body, fitTs);
        writeUint32LE(body, elapsedMs);
        body.write(avgHr & 0xFF);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(12);
        out.write(0x10);
        writeUint16LE(out, 100);
        writeUint32LE(out, body.size());
        out.write('.'); out.write('F'); out.write('I'); out.write('T');
        out.write(body.toByteArray());
        out.write(0x00); out.write(0x00);
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
