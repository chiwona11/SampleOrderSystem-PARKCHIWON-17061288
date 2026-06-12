package org.example.repository;

import org.example.model.ProductionItem;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProductionQueueRepository 테스트")
class ProductionQueueRepositoryTest {

    private File tempFile;
    private ProductionQueueRepository repo;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = Files.createTempFile("production_queue_", ".json").toFile();
        repo = new ProductionQueueRepository(tempFile);
    }

    @AfterEach
    void tearDown() {
        tempFile.delete();
    }

    private ProductionItem createItem(String orderId, String sampleId) {
        return new ProductionItem(orderId, sampleId, 10, 13, 100L, "2026-06-12T10:00:00");
    }

    @Test
    @DisplayName("save_새항목저장_파일에영속화됨")
    void save_newItem_persistsToFile() {
        ProductionItem item = createItem("order-001", "sample-A");
        repo.save(item);

        List<ProductionItem> all = repo.findAll();
        assertEquals(1, all.size());
        assertEquals("order-001", all.get(0).getOrderId());
        assertEquals("sample-A", all.get(0).getSampleId());
        assertEquals(10, all.get(0).getRequiredQuantity());
        assertEquals(13, all.get(0).getActualProduction());
    }

    @Test
    @DisplayName("deleteById_존재하는항목삭제_findAll에서제거됨")
    void deleteById_existingItem_removesFromFile() {
        repo.save(createItem("order-001", "sample-A"));
        repo.save(createItem("order-002", "sample-B"));

        repo.deleteById("order-001");

        List<ProductionItem> all = repo.findAll();
        assertEquals(1, all.size());
        assertEquals("order-002", all.get(0).getOrderId());
    }

    @Test
    @DisplayName("findById_존재하는항목조회_해당항목반환됨")
    void findById_existingItem_returnsItem() {
        repo.save(createItem("order-001", "sample-A"));

        ProductionItem found = repo.findById("order-001").orElseThrow();
        assertEquals("order-001", found.getOrderId());
        assertEquals("sample-A", found.getSampleId());
        assertEquals(13, found.getActualProduction());
    }

    @Test
    @DisplayName("재시작후findAll_같은파일로새인스턴스생성_큐데이터복원됨")
    void findAll_afterRestart_restoresQueue() {
        repo.save(createItem("order-001", "sample-A"));
        repo.save(createItem("order-002", "sample-B"));

        // 앱 재시작 시뮬레이션: 같은 파일로 새 Repository 인스턴스 생성
        ProductionQueueRepository restarted = new ProductionQueueRepository(tempFile);
        List<ProductionItem> restored = restarted.findAll();

        assertEquals(2, restored.size());
        assertTrue(restored.stream().anyMatch(p -> p.getOrderId().equals("order-001")));
        assertTrue(restored.stream().anyMatch(p -> p.getOrderId().equals("order-002")));
    }
}
