package org.example.service;

import org.example.model.Inventory;
import org.example.model.InventoryStatus;
import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.Sample;
import org.example.repository.InventoryRepository;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MonitorService 테스트")
class MonitorServiceTest {

    private File tempSamplesFile;
    private File tempOrdersFile;
    private File tempInventoryFile;
    private SampleRepository sampleRepo;
    private OrderRepository orderRepo;
    private InventoryRepository inventoryRepo;
    private MonitorService monitorService;

    @BeforeEach
    void setUp() throws IOException {
        tempSamplesFile   = Files.createTempFile("samples_", ".json").toFile();
        tempOrdersFile    = Files.createTempFile("orders_", ".json").toFile();
        tempInventoryFile = Files.createTempFile("inventory_", ".json").toFile();
        sampleRepo    = new SampleRepository(tempSamplesFile);
        orderRepo     = new OrderRepository(tempOrdersFile);
        inventoryRepo = new InventoryRepository(tempInventoryFile);
        monitorService = new MonitorService(orderRepo, inventoryRepo, sampleRepo);
    }

    @AfterEach
    void tearDown() {
        tempSamplesFile.delete();
        tempOrdersFile.delete();
        tempInventoryFile.delete();
    }

    @Test
    @DisplayName("getOrderCountByStatus_REJECTED제외_상태별건수반환")
    void getOrderCountByStatus_excludesRejected_returnsCountByStatus() {
        String now = LocalDateTime.now().toString();
        orderRepo.save(new Order("O001", "S001", "홍길동", 10, OrderStatus.RESERVED,  now));
        orderRepo.save(new Order("O002", "S001", "김철수", 5,  OrderStatus.CONFIRMED, now));
        orderRepo.save(new Order("O003", "S001", "이영희", 20, OrderStatus.REJECTED,  now));
        orderRepo.save(new Order("O004", "S002", "박민준", 15, OrderStatus.PRODUCING, now));
        orderRepo.save(new Order("O005", "S002", "최지수", 8,  OrderStatus.RESERVED,  now));

        Map<OrderStatus, Long> result = monitorService.getOrderCountByStatus();

        assertFalse(result.containsKey(OrderStatus.REJECTED));
        assertEquals(2L, result.get(OrderStatus.RESERVED));
        assertEquals(1L, result.get(OrderStatus.CONFIRMED));
        assertEquals(1L, result.get(OrderStatus.PRODUCING));
    }

    @Test
    @DisplayName("getInventoryStatus_재고0인시료_DEPLETED반환")
    void getInventoryStatus_zeroStock_returnsDepleted() {
        sampleRepo.save(new Sample("S001", "Alpha-7", 3600L, 0.95));
        inventoryRepo.save(new Inventory("S001", 0));
        // pendingDemand가 있어도 stock==0이면 DEPLETED 우선
        orderRepo.save(new Order("O001", "S001", "홍길동", 5, OrderStatus.RESERVED,
                LocalDateTime.now().toString()));

        Map<String, InventoryStatus> result = monitorService.getInventoryStatus();

        assertEquals(InventoryStatus.DEPLETED, result.get("S001"));
    }

    @Test
    @DisplayName("getInventoryStatus_재고부족_SHORTAGE반환")
    void getInventoryStatus_partialStock_returnsShortage() {
        sampleRepo.save(new Sample("S001", "Alpha-7", 3600L, 0.95));
        inventoryRepo.save(new Inventory("S001", 3));
        // pendingDemand = 5 + 5 = 10, stock(3) < pendingDemand(10)
        String now = LocalDateTime.now().toString();
        orderRepo.save(new Order("O001", "S001", "홍길동", 5, OrderStatus.RESERVED,  now));
        orderRepo.save(new Order("O002", "S001", "김철수", 5, OrderStatus.PRODUCING, now));

        Map<String, InventoryStatus> result = monitorService.getInventoryStatus();

        assertEquals(InventoryStatus.SHORTAGE, result.get("S001"));
    }

    @Test
    @DisplayName("getInventoryStatus_재고충분_SUFFICIENT반환")
    void getInventoryStatus_sufficientStock_returnsSufficient() {
        sampleRepo.save(new Sample("S001", "Alpha-7", 3600L, 0.95));
        inventoryRepo.save(new Inventory("S001", 100));
        // pendingDemand = 10, stock(100) >= pendingDemand(10)
        orderRepo.save(new Order("O001", "S001", "홍길동", 10, OrderStatus.RESERVED,
                LocalDateTime.now().toString()));

        Map<String, InventoryStatus> result = monitorService.getInventoryStatus();

        assertEquals(InventoryStatus.SUFFICIENT, result.get("S001"));
    }
}
