package org.example.repository;

import org.example.model.Inventory;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InventoryRepository 테스트")
class InventoryRepositoryTest {

    private File tempFile;
    private InventoryRepository repo;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = Files.createTempFile("inventory_", ".json").toFile();
        repo = new InventoryRepository(tempFile);
    }

    @AfterEach
    void tearDown() {
        tempFile.delete();
    }

    @Test
    @DisplayName("findOrCreate_존재하지않는시료_재고0으로생성")
    void findOrCreate_nonExistingSampleId_createsWithZeroStock() {
        Inventory inv = repo.findOrCreate("S001");

        assertEquals("S001", inv.getSampleId());
        assertEquals(0, inv.getStock());
        // 파일에도 저장되었는지 확인
        assertTrue(repo.findById("S001").isPresent());
    }

    @Test
    @DisplayName("findOrCreate_이미존재하는시료_기존재고반환")
    void findOrCreate_existingSampleId_returnsExistingInventory() {
        repo.save(new Inventory("S001", 100));

        Inventory inv = repo.findOrCreate("S001");

        assertEquals("S001", inv.getSampleId());
        assertEquals(100, inv.getStock());
        // 중복 저장되지 않았는지 확인
        assertEquals(1, repo.findAll().size());
    }

    @Test
    @DisplayName("update_재고변경후업데이트_변경된재고반환")
    void update_stockChanged_updatedStockReflected() {
        repo.save(new Inventory("S001", 50));

        Inventory updated = new Inventory("S001", 50).withStock(30);
        repo.update(updated);

        Inventory found = repo.findById("S001").orElseThrow();
        assertEquals(30, found.getStock());
    }

    @Test
    @DisplayName("재시작후findAll_같은파일로새인스턴스생성_데이터복원됨")
    void findAll_afterRestartWithSameFile_restoresData() {
        repo.save(new Inventory("S001", 100));
        repo.save(new Inventory("S002", 250));

        // 앱 재시작 시뮬레이션: 같은 파일로 새 Repository 인스턴스 생성
        InventoryRepository restarted = new InventoryRepository(tempFile);
        java.util.List<Inventory> restored = restarted.findAll();

        assertEquals(2, restored.size());
        assertTrue(restored.stream().anyMatch(inv -> inv.getSampleId().equals("S001") && inv.getStock() == 100));
        assertTrue(restored.stream().anyMatch(inv -> inv.getSampleId().equals("S002") && inv.getStock() == 250));
    }
}
