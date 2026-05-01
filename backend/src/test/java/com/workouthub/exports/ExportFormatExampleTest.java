package com.workouthub.exports;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Guards the documented example in docs/examples/full-export-example.json:
 * the file shipped alongside docs/EXPORT_FORMAT.md MUST round-trip through
 * the live import endpoint, so the documentation cannot drift from the
 * actual contract.
 */
@AutoConfigureMockMvc
class ExportFormatExampleTest extends AbstractIntegrationTest {

    private static final String SECRET = "ExampleSecret1!";
    private static final Path EXAMPLE_PATH = Path.of(
            System.getProperty("user.dir"),
            "..",
            "docs",
            "examples",
            "full-export-example.json");

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;

    @Test
    void documentedExampleImportsAndBecomesTheCurrentExportState() throws Exception {
        String example = Files.readString(EXAMPLE_PATH);
        SeededUser u = helpers.seed(
                "ex-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();

        mvc.perform(post("/api/export/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(example))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricsInserted").value(1))
                .andExpect(jsonPath("$.supplementsInserted").value(1));

        // After import, a read-back must surface the example's slices.
        mvc.perform(get("/api/metrics").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].recordedDate").value("2026-04-20"));

        mvc.perform(get("/api/supplements").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Creatine"))
                .andExpect(jsonPath("$[0].timing").value("morning"));
    }
}
