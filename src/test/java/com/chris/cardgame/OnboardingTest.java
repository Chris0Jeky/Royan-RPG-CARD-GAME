package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.chris.cardgame.web.GameSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OnboardingTest {
    @Test
    void corruptSaveReturnsFriendlyRecoveryDto(@TempDir Path temp) throws Exception {
        Path save = temp.resolve("save.json");
        Files.writeString(save, "{nope, this is not json", StandardCharsets.UTF_8);

        Map<String, Object> dto = new GameSession(save).continueRun();

        assertThat(dto.get("phase")).isEqualTo("select");
        assertThat(dto.get("hasSave")).isEqualTo(false);
        assertThat(dto.get("canStartFresh")).isEqualTo(true);
        assertThat(dto.get("heroes")).asList().hasSize(3);
        String error = (String) dto.get("error");
        assertThat(error).contains("water-damaged");
        assertThat(error).contains("fresh voyage");
        assertThat(error).doesNotContain(
                "at com.chris", "Exception", "Caused by", ".java:", "\n", "\tat ");
    }

    @Test
    void wrongVersionSaveReturnsFriendlyRecoveryDto(@TempDir Path temp) throws Exception {
        Path save = temp.resolve("save.json");
        Files.writeString(save, "{\"version\":999}", StandardCharsets.UTF_8);

        Map<String, Object> dto = new GameSession(save).continueRun();

        assertThat(dto.get("phase")).isEqualTo("select");
        assertThat(dto.get("hasSave")).isEqualTo(false);
        assertThat(dto.get("canStartFresh")).isEqualTo(true);
        assertThat((String) dto.get("error")).contains("water-damaged");
    }

    @Test
    void missingSaveKeepsNoSavedCampaignContract(@TempDir Path temp) {
        Path save = temp.resolve("save.json");

        assertThatThrownBy(() -> new GameSession(save).continueRun())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No saved campaign");
    }

    @Test
    void webResourcesHaveNoExternalReferences() throws Exception {
        Path web = Paths.get("src/main/resources/web");
        assertThat(web).isDirectory();

        List<String> violations = new ArrayList<>();
        int scanned = 0;
        int xmlnsAllowed = 0;
        List<Path> files;
        try (Stream<Path> walk = Files.walk(web)) {
            files = walk.filter(Files::isRegularFile).sorted().toList();
        }
        for (Path file : files) {
            scanned++;
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains("http://www.w3.org/2000/svg")) {
                    // XML namespace identifier: never dereferenced by browsers.
                    xmlnsAllowed++;
                    line = line.replace("http://www.w3.org/2000/svg", "");
                }
                if (line.contains("http://") || line.contains("https://")) {
                    violations.add(web.relativize(file) + ":" + (i + 1));
                }
            }
        }

        assertThat(scanned).isGreaterThan(30);
        assertThat(xmlnsAllowed).isGreaterThan(30);
        assertThat(violations).as("external http(s) references in web/**").isEmpty();
    }
}
