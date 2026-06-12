package org.example.service;

import org.example.model.Inventory;
import org.example.model.Sample;
import org.example.repository.InventoryRepository;
import org.example.repository.SampleRepository;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SampleService 테스트")
class SampleServiceTest {

    private File tempSamplesFile;
    private File tempInventoryFile;
    private SampleRepository sampleRepo;
    private InventoryRepository inventoryRepo;
    private SampleService sampleService;

    @BeforeEach
    void setUp() throws IOException {
        tempSamplesFile = Files.createTempFile("samples_", ".json").toFile();
        tempInventoryFile = Files.createTempFile("inventory_", ".json").toFile();
        sampleRepo = new SampleRepository(tempSamplesFile);
        inventoryRepo = new InventoryRepository(tempInventoryFile);
        sampleService = new SampleService(sampleRepo, inventoryRepo);
    }

    @AfterEach
    void tearDown() {
        tempSamplesFile.delete();
        tempInventoryFile.delete();
    }

    @Test
    @DisplayName("register_새로운시료_저장소에추가됨")
    void register_newSample_addedToRepository() {
        Sample result = sampleService.register("S001", "Alpha-7", 3600L, 0.95);

        assertNotNull(result);
        assertEquals("S001", result.getId());
        assertEquals("Alpha-7", result.getName());
        assertEquals(3600L, result.getAvgProductionTime());
        assertEquals(0.95, result.getYield());
        assertTrue(sampleRepo.existsById("S001"));
    }

    @Test
    @DisplayName("register_중복ID_IllegalArgumentException발생")
    void register_duplicateId_throwsIllegalArgumentException() {
        sampleService.register("S001", "Alpha-7", 3600L, 0.95);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sampleService.register("S001", "Beta-3", 1200L, 0.88));
        assertTrue(ex.getMessage().contains("S001"));
    }

    @Test
    @DisplayName("register_시료등록시_재고0으로초기화됨")
    void register_newSample_inventoryInitializedWithZero() {
        sampleService.register("S001", "Alpha-7", 3600L, 0.95);

        Inventory inventory = inventoryRepo.findById("S001").orElseThrow();
        assertEquals("S001", inventory.getSampleId());
        assertEquals(0, inventory.getStock());
    }

    @Test
    @DisplayName("search_부분문자열_일치하는시료반환")
    void search_partialKeyword_returnsMatchingSamples() {
        sampleService.register("S001", "Alpha-7 시료", 3600L, 0.95);
        sampleService.register("S002", "Beta-3 시료", 1200L, 0.88);
        sampleService.register("S003", "Gamma-1 시료", 2400L, 0.91);

        List<Sample> result = sampleService.search("Alpha");

        assertEquals(1, result.size());
        assertEquals("S001", result.get(0).getId());
    }

    @Test
    @DisplayName("search_대소문자무시_일치하는시료반환")
    void search_caseInsensitive_returnsMatchingSamples() {
        sampleService.register("S001", "Alpha-7 시료", 3600L, 0.95);
        sampleService.register("S002", "Beta-3 시료", 1200L, 0.88);

        List<Sample> result = sampleService.search("alpha");

        assertEquals(1, result.size());
        assertEquals("S001", result.get(0).getId());
    }

    @Test
    @DisplayName("search_빈키워드_전체목록반환")
    void search_emptyKeyword_returnsAllSamples() {
        sampleService.register("S001", "Alpha-7 시료", 3600L, 0.95);
        sampleService.register("S002", "Beta-3 시료", 1200L, 0.88);

        List<Sample> resultEmpty = sampleService.search("");
        List<Sample> resultNull  = sampleService.search(null);
        List<Sample> resultBlank = sampleService.search("   ");

        assertEquals(2, resultEmpty.size());
        assertEquals(2, resultNull.size());
        assertEquals(2, resultBlank.size());
    }
}
