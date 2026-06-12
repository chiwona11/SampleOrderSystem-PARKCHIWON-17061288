package org.example.service;

import org.example.model.Inventory;
import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.ProductionItem;
import org.example.repository.InventoryRepository;
import org.example.repository.OrderRepository;
import org.example.repository.ProductionQueueRepository;
import org.example.repository.SampleRepository;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProductionService 테스트")
class ProductionServiceTest {

    private File tempSamplesFile;
    private File tempOrdersFile;
    private File tempInventoryFile;
    private File tempQueueFile;
    private SampleRepository sampleRepo;
    private OrderRepository orderRepo;
    private InventoryRepository inventoryRepo;
    private ProductionQueueRepository productionQueueRepo;
    private ProductionService productionService;

    @BeforeEach
    void setUp() throws IOException {
        tempSamplesFile   = Files.createTempFile("samples_", ".json").toFile();
        tempOrdersFile    = Files.createTempFile("orders_", ".json").toFile();
        tempInventoryFile = Files.createTempFile("inventory_", ".json").toFile();
        tempQueueFile     = Files.createTempFile("production_queue_", ".json").toFile();
        sampleRepo          = new SampleRepository(tempSamplesFile);
        orderRepo           = new OrderRepository(tempOrdersFile);
        inventoryRepo       = new InventoryRepository(tempInventoryFile);
        productionQueueRepo = new ProductionQueueRepository(tempQueueFile);
        productionService   = new ProductionService(productionQueueRepo, orderRepo, inventoryRepo);
    }

    @AfterEach
    void tearDown() {
        tempSamplesFile.delete();
        tempOrdersFile.delete();
        tempInventoryFile.delete();
        tempQueueFile.delete();
    }

    @Test
    @DisplayName("getActiveProductions_PRODUCING주문존재_목록반환")
    void getActiveProductions_hasProducingOrders_returnsList() {
        String now = LocalDateTime.now().toString();
        orderRepo.save(new Order("O001", "S001", "홍길동", 10, OrderStatus.PRODUCING, now));
        orderRepo.save(new Order("O002", "S001", "김철수", 5,  OrderStatus.CONFIRMED, now));
        orderRepo.save(new Order("O003", "S002", "이영희", 20, OrderStatus.PRODUCING, now));

        List<Order> result = productionService.getActiveProductions();

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(o -> o.getStatus() == OrderStatus.PRODUCING));
    }

    @Test
    @DisplayName("getQueueStatus_큐에항목존재_FIFO순서반환")
    void getQueueStatus_queueHasItems_returnsInFifoOrder() {
        String now = LocalDateTime.now().toString();
        ProductionItem item1 = new ProductionItem("O001", "S001", 5,  7,  420L, now);
        ProductionItem item2 = new ProductionItem("O002", "S001", 10, 13, 780L, now);
        ProductionItem item3 = new ProductionItem("O003", "S002", 3,  4,  240L, now);
        productionQueueRepo.save(item1);
        productionQueueRepo.save(item2);
        productionQueueRepo.save(item3);

        List<ProductionItem> result = productionService.getQueueStatus();

        assertEquals(3, result.size());
        assertEquals("O001", result.get(0).getOrderId());
        assertEquals("O002", result.get(1).getOrderId());
        assertEquals("O003", result.get(2).getOrderId());
    }

    @Test
    @DisplayName("completeProduction_생산완료_재고증가및CONFIRMED전환")
    void completeProduction_validOrderId_increasesStockAndSetsConfirmed() {
        // 재고 5, 주문 수량 10, actualProduction=13 → newStock = max(0, 5+13-10) = 8
        String now = LocalDateTime.now().toString();
        inventoryRepo.save(new Inventory("S001", 5));
        orderRepo.save(new Order("O001", "S001", "홍길동", 10, OrderStatus.PRODUCING, now));
        productionQueueRepo.save(new ProductionItem("O001", "S001", 5, 13, 780L, now));

        Order result = productionService.completeProduction("O001");

        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        Inventory inventory = inventoryRepo.findById("S001").orElseThrow();
        assertEquals(8, inventory.getStock());
        assertEquals(0, productionQueueRepo.findAll().size());
    }

    @Test
    @DisplayName("completeProduction_큐에없는주문ID_예외발생")
    void completeProduction_orderNotInQueue_throwsException() {
        orderRepo.save(new Order("O001", "S001", "홍길동", 10, OrderStatus.PRODUCING,
                LocalDateTime.now().toString()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productionService.completeProduction("O001"));
        assertTrue(ex.getMessage().contains("O001"));
    }
}
