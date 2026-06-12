package org.example.service;

import org.example.model.Inventory;
import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.ProductionItem;
import org.example.model.Sample;
import org.example.repository.InventoryRepository;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrderService 테스트")
class OrderServiceTest {

    private File tempSamplesFile;
    private File tempOrdersFile;
    private File tempInventoryFile;
    private SampleRepository sampleRepo;
    private OrderRepository orderRepo;
    private InventoryRepository inventoryRepo;
    private List<ProductionItem> productionQueue;
    private OrderService orderService;

    @BeforeEach
    void setUp() throws IOException {
        tempSamplesFile   = Files.createTempFile("samples_", ".json").toFile();
        tempOrdersFile    = Files.createTempFile("orders_", ".json").toFile();
        tempInventoryFile = Files.createTempFile("inventory_", ".json").toFile();
        sampleRepo    = new SampleRepository(tempSamplesFile);
        orderRepo     = new OrderRepository(tempOrdersFile);
        inventoryRepo = new InventoryRepository(tempInventoryFile);
        productionQueue = new ArrayList<>();
        orderService  = new OrderService(orderRepo, sampleRepo, inventoryRepo, productionQueue);
    }

    @AfterEach
    void tearDown() {
        tempSamplesFile.delete();
        tempOrdersFile.delete();
        tempInventoryFile.delete();
    }

    private void registerSample(String id, String name, long avgProductionTime, double yield) {
        sampleRepo.save(new Sample(id, name, avgProductionTime, yield));
        inventoryRepo.save(new Inventory(id, 0));
    }

    private void setStock(String sampleId, int stock) {
        Inventory existing = inventoryRepo.findById(sampleId).orElse(null);
        if (existing != null) {
            inventoryRepo.update(existing.withStock(stock));
        } else {
            inventoryRepo.save(new Inventory(sampleId, stock));
        }
    }

    @Test
    @DisplayName("placeOrder_유효한시료ID_RESERVED상태주문생성")
    void placeOrder_validSampleId_createsReservedOrder() {
        registerSample("S001", "Alpha-7", 3600L, 0.95);

        Order order = orderService.placeOrder("S001", "홍길동", 10);

        assertNotNull(order);
        assertNotNull(order.getId());
        assertEquals("S001", order.getSampleId());
        assertEquals("홍길동", order.getCustomerName());
        assertEquals(10, order.getQuantity());
        assertEquals(OrderStatus.RESERVED, order.getStatus());
        assertNotNull(order.getCreatedAt());
    }

    @Test
    @DisplayName("placeOrder_존재하지않는시료ID_예외발생")
    void placeOrder_invalidSampleId_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.placeOrder("NOTEXIST", "홍길동", 10));
        assertTrue(ex.getMessage().contains("NOTEXIST"));
    }

    @Test
    @DisplayName("approve_재고충분_CONFIRMED상태로전환")
    void approve_sufficientStock_setsConfirmed() {
        registerSample("S001", "Alpha-7", 3600L, 0.95);
        setStock("S001", 100);
        Order order = orderService.placeOrder("S001", "홍길동", 10);

        Order approved = orderService.approve(order.getId());

        assertEquals(OrderStatus.CONFIRMED, approved.getStatus());
        Inventory inventory = inventoryRepo.findById("S001").orElseThrow();
        assertEquals(90, inventory.getStock());
    }

    @Test
    @DisplayName("approve_재고부족_PRODUCING상태로전환및생산큐등록")
    void approve_insufficientStock_setsProducingAndEnqueuesItem() {
        registerSample("S001", "Alpha-7", 3600L, 0.95);
        setStock("S001", 0);
        Order order = orderService.placeOrder("S001", "홍길동", 10);

        Order approved = orderService.approve(order.getId());

        assertEquals(OrderStatus.PRODUCING, approved.getStatus());
        assertEquals(1, productionQueue.size());
        assertEquals(order.getId(), productionQueue.get(0).getOrderId());
    }

    @Test
    @DisplayName("approve_재고부족_생산량계산식검증")
    void approve_insufficientStock_calculatesActualProductionCorrectly() {
        // shortage=10, yield=0.9 → actualProduction = ceil(10 / (0.9 * 0.9)) = ceil(12.345...) = 13
        registerSample("S001", "Alpha-7", 60L, 0.9);
        setStock("S001", 0);
        Order order = orderService.placeOrder("S001", "홍길동", 10);

        orderService.approve(order.getId());

        assertEquals(1, productionQueue.size());
        ProductionItem item = productionQueue.get(0);
        assertEquals(10, item.getRequiredQuantity());
        assertEquals(13, item.getActualProduction());
        assertEquals(60L * 13, item.getTotalProductionTime());
    }

    @Test
    @DisplayName("approve_RESERVED아닌상태_예외발생")
    void approve_nonReservedStatus_throwsIllegalStateException() {
        registerSample("S001", "Alpha-7", 3600L, 0.95);
        setStock("S001", 100);
        Order order = orderService.placeOrder("S001", "홍길동", 10);
        orderService.approve(order.getId());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.approve(order.getId()));
        assertTrue(ex.getMessage().contains("RESERVED"));
    }

    @Test
    @DisplayName("reject_RESERVED상태_REJECTED로전환")
    void reject_reservedOrder_setsRejected() {
        registerSample("S001", "Alpha-7", 3600L, 0.95);
        Order order = orderService.placeOrder("S001", "홍길동", 10);

        Order rejected = orderService.reject(order.getId());

        assertEquals(OrderStatus.REJECTED, rejected.getStatus());
    }

    @Test
    @DisplayName("release_CONFIRMED상태_RELEASE로전환")
    void release_confirmedOrder_setsRelease() {
        registerSample("S001", "Alpha-7", 3600L, 0.95);
        setStock("S001", 100);
        Order order = orderService.placeOrder("S001", "홍길동", 10);
        orderService.approve(order.getId());

        Order released = orderService.release(order.getId());

        assertEquals(OrderStatus.RELEASE, released.getStatus());
    }

    @Test
    @DisplayName("release_CONFIRMED아닌상태_예외발생")
    void release_nonConfirmedStatus_throwsIllegalStateException() {
        registerSample("S001", "Alpha-7", 3600L, 0.95);
        Order order = orderService.placeOrder("S001", "홍길동", 10);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.release(order.getId()));
        assertTrue(ex.getMessage().contains("CONFIRMED"));
    }
}
