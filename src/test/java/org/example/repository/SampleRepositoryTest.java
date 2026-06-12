package org.example.repository;

import org.example.model.Sample;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SampleRepository 테스트")
class SampleRepositoryTest {

    private File tempFile;
    private SampleRepository repo;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = Files.createTempFile("samples_", ".json").toFile();
        repo = new SampleRepository(tempFile);
    }

    @AfterEach
    void tearDown() {
        tempFile.delete();
    }

    @Test
    @DisplayName("save_새로운시료_파일에저장됨")
    void save_newSample_savedToFile() {
        Sample sample = new Sample("S001", "Alpha-7", 3600L, 0.95);
        repo.save(sample);

        List<Sample> all = repo.findAll();
        assertEquals(1, all.size());
        assertEquals("S001", all.get(0).getId());
    }

    @Test
    @DisplayName("findById_존재하는ID_시료반환")
    void findById_existingId_returnsSample() {
        Sample sample = new Sample("S002", "Beta-3", 7200L, 0.88);
        repo.save(sample);

        Optional<Sample> result = repo.findById("S002");
        assertTrue(result.isPresent());
        assertEquals("Beta-3", result.get().getName());
    }

    @Test
    @DisplayName("findById_존재하지않는ID_빈Optional반환")
    void findById_nonExistingId_returnsEmpty() {
        Optional<Sample> result = repo.findById("NOT_EXIST");
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("findAll_여러시료저장후_전체목록반환")
    void findAll_multipleSamples_returnsAll() {
        repo.save(new Sample("S001", "Alpha-7", 3600L, 0.95));
        repo.save(new Sample("S002", "Beta-3", 7200L, 0.88));
        repo.save(new Sample("S003", "Gamma-1", 1800L, 0.91));

        List<Sample> all = repo.findAll();
        assertEquals(3, all.size());
    }

    @Test
    @DisplayName("update_기존시료수정_변경사항반영됨")
    void update_existingSample_updatesFile() {
        repo.save(new Sample("S001", "Alpha-7", 3600L, 0.95));

        Sample updated = new Sample("S001", "Alpha-7-Updated", 4000L, 0.97);
        repo.update(updated);

        Sample found = repo.findById("S001").orElseThrow();
        assertEquals("Alpha-7-Updated", found.getName());
        assertEquals(4000L, found.getAvgProductionTime());
        assertEquals(0.97, found.getYield());
    }

    @Test
    @DisplayName("deleteById_존재하는ID_삭제됨")
    void deleteById_existingId_removed() {
        repo.save(new Sample("S001", "Alpha-7", 3600L, 0.95));
        repo.save(new Sample("S002", "Beta-3", 7200L, 0.88));

        repo.deleteById("S001");

        assertFalse(repo.findById("S001").isPresent());
        assertEquals(1, repo.findAll().size());
    }

    @Test
    @DisplayName("existsById_저장된시료ID_true반환")
    void existsById_savedId_returnsTrue() {
        repo.save(new Sample("S001", "Alpha-7", 3600L, 0.95));

        assertTrue(repo.existsById("S001"));
        assertFalse(repo.existsById("NOT_EXIST"));
    }

    @Test
    @DisplayName("재시작후findAll_같은파일로새인스턴스생성_데이터복원됨")
    void findAll_afterRestartWithSameFile_restoresData() {
        repo.save(new Sample("S001", "Alpha-7", 3600L, 0.95));
        repo.save(new Sample("S002", "Beta-3", 7200L, 0.88));

        // 앱 재시작 시뮬레이션: 같은 파일로 새 Repository 인스턴스 생성
        SampleRepository restarted = new SampleRepository(tempFile);
        List<Sample> restored = restarted.findAll();

        assertEquals(2, restored.size());
        assertTrue(restored.stream().anyMatch(s -> s.getId().equals("S001")));
        assertTrue(restored.stream().anyMatch(s -> s.getId().equals("S002")));
    }
}
