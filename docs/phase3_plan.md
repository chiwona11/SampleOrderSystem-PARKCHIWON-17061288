# Phase 3 상세 설계 — 서비스 레이어

> 이 문서는 PLAN.md Phase 3의 상세 버전이다. 완료 기준 및 구현 명세는 이 문서를 정본으로 삼으며, PLAN.md와 내용이 다른 경우 이 문서를 우선한다.

---

## Phase 목표

핵심 비즈니스 로직(시료 관리, 주문 처리, 생산 라인, 모니터링)을 4개의 Service 클래스(`SampleService`, `OrderService`, `ProductionService`, `MonitorService`)에 구현하고, 각 Service에 대한 단위 테스트를 작성하여 `./gradlew test --tests "org.example.service.*"` 전체 GREEN을 달성한다. 모든 Service는 생성자 주입 방식으로 Repository를 받으며, `OrderService`와 `ProductionService`는 `List<ProductionItem>` 생산 큐를 공유 참조로 주입받아 런타임 상태를 공유한다.

---

## FR-N / NFR-N 매핑

| 요구사항 | 설명 |
|---------|------|
| FR-1-1 | `SampleService.register()` — 중복 ID 시 `IllegalArgumentException`, 등록 성공 시 재고 0 초기화 |
| FR-1-2 | `SampleService.findAll()` — 전체 시료 목록 반환 |
| FR-1-3 | `SampleService.search()` — 이름 부분 문자열 검색 (대소문자 무시), 빈 키워드 시 전체 반환 |
| FR-2-1 | `OrderService.placeOrder()` — RESERVED 상태로 주문 생성, 미등록 시료 ID 시 `IllegalArgumentException` |
| FR-2-2 | `OrderService.approve()` — 재고 충분: CONFIRMED + 재고 차감, 재고 부족: PRODUCING + ProductionItem 큐 등록 |
| FR-2-3 | `OrderService.reject()` — RESERVED → REJECTED |
| FR-3-1 | `MonitorService.getOrderCountByStatus()` — REJECTED 제외 상태별 건수 집계 |
| FR-3-2 | `MonitorService.getInventoryStatus()` — pendingDemand 기반 SUFFICIENT/SHORTAGE/DEPLETED 판정 |
| FR-4-1 | `OrderService.release()` — CONFIRMED → RELEASE |
| FR-5-1 | `ProductionService.getActiveProductions()` — PRODUCING 상태 주문 목록 반환 |
| FR-5-2 | `ProductionService.getQueueStatus()` — 생산 대기 큐 FIFO 순서 반환 |
| FR-5-3 | `ProductionService.completeProduction()` — 재고 증가 후 주문 수량 차감, CONFIRMED 전환, 큐에서 제거 |
| NFR-2 | 각 Service 핵심 메서드 단위 테스트 (Mock 금지, 실제 임시 파일 기반 Repository 사용) |

---

## 변경 대상 파일 목록

```
src/main/java/org/example/service/SampleService.java              (생성)
src/main/java/org/example/service/OrderService.java               (생성)
src/main/java/org/example/service/ProductionService.java          (생성)
src/main/java/org/example/service/MonitorService.java             (생성)
src/test/java/org/example/service/SampleServiceTest.java          (생성)
src/test/java/org/example/service/OrderServiceTest.java           (생성)
src/test/java/org/example/service/ProductionServiceTest.java      (생성)
src/test/java/org/example/service/MonitorServiceTest.java         (생성)
```

---

## 구현 상세

### `src/main/java/org/example/service/SampleService.java`

```java
package org.example.service;

import org.example.model.Inventory;
import org.example.model.Sample;
import org.example.repository.InventoryRepository;
import org.example.repository.SampleRepository;

import java.util.List;

public class SampleService {

    private final SampleRepository sampleRepo;
    private final InventoryRepository inventoryRepo;

    public SampleService(SampleRepository sampleRepo, InventoryRepository inventoryRepo) {
        this.sampleRepo = sampleRepo;
        this.inventoryRepo = inventoryRepo;
    }

    public Sample register(String id, String name, long avgProductionTime, double yield) {
        if (sampleRepo.existsById(id)) {
            throw new IllegalArgumentException("이미 존재하는 시료 ID입니다: " + id);
        }
        Sample sample = new Sample(id, name, avgProductionTime, yield);
        sampleRepo.save(sample);
        inventoryRepo.save(new Inventory(id, 0));
        return sample;
    }

    public List<Sample> findAll() {
        return sampleRepo.findAll();
    }

    public List<Sample> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        return sampleRepo.findAll().stream()
                .filter(s -> s.getName().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
    }
}
```

---

### `src/main/java/org/example/service/OrderService.java`

```java
package org.example.service;

import org.example.model.Inventory;
import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.ProductionItem;
import org.example.model.Sample;
import org.example.repository.InventoryRepository;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class OrderService {

    private final OrderRepository orderRepo;
    private final SampleRepository sampleRepo;
    private final InventoryRepository inventoryRepo;
    private final List<ProductionItem> productionQueue;

    public OrderService(OrderRepository orderRepo, SampleRepository sampleRepo,
                        InventoryRepository inventoryRepo, List<ProductionItem> productionQueue) {
        this.orderRepo = orderRepo;
        this.sampleRepo = sampleRepo;
        this.inventoryRepo = inventoryRepo;
        this.productionQueue = productionQueue;
    }

    public Order placeOrder(String sampleId, String customerName, int quantity) {
        if (!sampleRepo.existsById(sampleId)) {
            throw new IllegalArgumentException("등록되지 않은 시료 ID입니다: " + sampleId);
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("주문 수량은 1 이상이어야 합니다.");
        }
        Order order = new Order(
                UUID.randomUUID().toString(),
                sampleId,
                customerName,
                quantity,
                OrderStatus.RESERVED,
                LocalDateTime.now().toString()
        );
        orderRepo.save(order);
        return order;
    }

    public Order approve(String orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문 ID입니다: " + orderId));
        if (order.getStatus() != OrderStatus.RESERVED) {
            throw new IllegalStateException("RESERVED 상태인 주문만 승인할 수 있습니다. 현재 상태: " + order.getStatus());
        }
        Sample sample = sampleRepo.findById(order.getSampleId())
                .orElseThrow(() -> new IllegalArgumentException("시료를 찾을 수 없습니다: " + order.getSampleId()));
        Inventory inventory = inventoryRepo.findOrCreate(order.getSampleId());

        if (inventory.getStock() >= order.getQuantity()) {
            int newStock = inventory.getStock() - order.getQuantity();
            inventoryRepo.update(inventory.withStock(newStock));
            Order confirmed = order.withStatus(OrderStatus.CONFIRMED);
            orderRepo.update(confirmed);
            return confirmed;
        } else {
            int shortage = order.getQuantity() - inventory.getStock();
            int actualProduction = (int) Math.ceil(shortage / (sample.getYield() * 0.9));
            long totalProductionTime = sample.getAvgProductionTime() * actualProduction;
            ProductionItem item = new ProductionItem(
                    order.getId(),
                    order.getSampleId(),
                    shortage,
                    actualProduction,
                    totalProductionTime,
                    LocalDateTime.now().toString()
            );
            productionQueue.add(item);
            Order producing = order.withStatus(OrderStatus.PRODUCING);
            orderRepo.update(producing);
            return producing;
        }
    }

    public Order reject(String orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문 ID입니다: " + orderId));
        if (order.getStatus() != OrderStatus.RESERVED) {
            throw new IllegalStateException("RESERVED 상태인 주문만 거절할 수 있습니다. 현재 상태: " + order.getStatus());
        }
        Order rejected = order.withStatus(OrderStatus.REJECTED);
        orderRepo.update(rejected);
        return rejected;
    }

    public Order release(String orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문 ID입니다: " + orderId));
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("CONFIRMED 상태인 주문만 출고할 수 있습니다. 현재 상태: " + order.getStatus());
        }
        Order released = order.withStatus(OrderStatus.RELEASE);
        orderRepo.update(released);
        return released;
    }

    public List<Order> findAll() {
        return orderRepo.findAll();
    }
}
```

---

### `src/main/java/org/example/service/ProductionService.java`

```java
package org.example.service;

import org.example.model.Inventory;
import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.ProductionItem;
import org.example.repository.InventoryRepository;
import org.example.repository.OrderRepository;

import java.util.List;

public class ProductionService {

    private final List<ProductionItem> productionQueue;
    private final OrderRepository orderRepo;
    private final InventoryRepository inventoryRepo;

    public ProductionService(List<ProductionItem> productionQueue,
                             OrderRepository orderRepo, InventoryRepository inventoryRepo) {
        this.productionQueue = productionQueue;
        this.orderRepo = orderRepo;
        this.inventoryRepo = inventoryRepo;
    }

    public List<Order> getActiveProductions() {
        return orderRepo.findByStatus(OrderStatus.PRODUCING);
    }

    public List<ProductionItem> getQueueStatus() {
        return List.copyOf(productionQueue);
    }

    public Order completeProduction(String orderId) {
        ProductionItem item = productionQueue.stream()
                .filter(p -> p.getOrderId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("생산 큐에 등록되지 않은 주문 ID입니다: " + orderId));

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문 ID입니다: " + orderId));

        Inventory inventory = inventoryRepo.findOrCreate(order.getSampleId());
        int newStock = Math.max(0, inventory.getStock() + item.getActualProduction() - order.getQuantity());
        inventoryRepo.update(inventory.withStock(newStock));

        Order confirmed = order.withStatus(OrderStatus.CONFIRMED);
        orderRepo.update(confirmed);

        productionQueue.remove(item);
        return confirmed;
    }
}
```

---

### `src/main/java/org/example/service/MonitorService.java`

```java
package org.example.service;

import org.example.model.Inventory;
import org.example.model.InventoryStatus;
import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.Sample;
import org.example.repository.InventoryRepository;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MonitorService {

    private final OrderRepository orderRepo;
    private final InventoryRepository inventoryRepo;
    private final SampleRepository sampleRepo;

    public MonitorService(OrderRepository orderRepo, InventoryRepository inventoryRepo,
                          SampleRepository sampleRepo) {
        this.orderRepo = orderRepo;
        this.inventoryRepo = inventoryRepo;
        this.sampleRepo = sampleRepo;
    }

    public Map<OrderStatus, Long> getOrderCountByStatus() {
        return orderRepo.findAll().stream()
                .filter(o -> o.getStatus() != OrderStatus.REJECTED)
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
    }

    public Map<String, InventoryStatus> getInventoryStatus() {
        List<Sample> samples = sampleRepo.findAll();
        List<Order> allOrders = orderRepo.findAll();

        return samples.stream().collect(Collectors.toMap(
                Sample::getId,
                sample -> {
                    String sampleId = sample.getId();
                    Inventory inventory = inventoryRepo.findOrCreate(sampleId);
                    int stock = inventory.getStock();

                    int pendingDemand = allOrders.stream()
                            .filter(o -> o.getSampleId().equals(sampleId))
                            .filter(o -> o.getStatus() == OrderStatus.RESERVED
                                    || o.getStatus() == OrderStatus.PRODUCING)
                            .mapToInt(Order::getQuantity)
                            .sum();

                    if (stock == 0) {
                        return InventoryStatus.DEPLETED;
                    } else if (stock < pendingDemand) {
                        return InventoryStatus.SHORTAGE;
                    } else {
                        return InventoryStatus.SUFFICIENT;
                    }
                }
        ));
    }
}
```

---

## 테스트 상세

### 테스트 공통 원칙

- `Files.createTempFile()`로 임시 파일 생성 — `data/` 실제 경로 사용 금지
- `@BeforeEach`: 임시 파일 생성 + Repository 인스턴스 생성 + Service 인스턴스 생성
- `@AfterEach`: 임시 파일 삭제 (사용한 파일 전체)
- Mockito 사용 금지 — 실제 파일 기반 Repository 인스턴스 사용
- 테스트 메서드 명명: `메서드명_상황_기대결과` (영문 camelCase)
- `@DisplayName`: 한국어 필수

---

### `src/test/java/org/example/service/SampleServiceTest.java`

```java
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
```

---

### `src/test/java/org/example/service/OrderServiceTest.java`

```java
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
```

---

### `src/test/java/org/example/service/ProductionServiceTest.java`

```java
package org.example.service;

import org.example.model.Inventory;
import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.ProductionItem;
import org.example.repository.InventoryRepository;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProductionService 테스트")
class ProductionServiceTest {

    private File tempSamplesFile;
    private File tempOrdersFile;
    private File tempInventoryFile;
    private SampleRepository sampleRepo;
    private OrderRepository orderRepo;
    private InventoryRepository inventoryRepo;
    private List<ProductionItem> productionQueue;
    private ProductionService productionService;

    @BeforeEach
    void setUp() throws IOException {
        tempSamplesFile   = Files.createTempFile("samples_", ".json").toFile();
        tempOrdersFile    = Files.createTempFile("orders_", ".json").toFile();
        tempInventoryFile = Files.createTempFile("inventory_", ".json").toFile();
        sampleRepo    = new SampleRepository(tempSamplesFile);
        orderRepo     = new OrderRepository(tempOrdersFile);
        inventoryRepo = new InventoryRepository(tempInventoryFile);
        productionQueue = new ArrayList<>();
        productionService = new ProductionService(productionQueue, orderRepo, inventoryRepo);
    }

    @AfterEach
    void tearDown() {
        tempSamplesFile.delete();
        tempOrdersFile.delete();
        tempInventoryFile.delete();
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
        productionQueue.add(item1);
        productionQueue.add(item2);
        productionQueue.add(item3);

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
        productionQueue.add(new ProductionItem("O001", "S001", 5, 13, 780L, now));

        Order result = productionService.completeProduction("O001");

        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        Inventory inventory = inventoryRepo.findById("S001").orElseThrow();
        assertEquals(8, inventory.getStock());
        assertEquals(0, productionQueue.size());
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
```

---

### `src/test/java/org/example/service/MonitorServiceTest.java`

```java
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
```

---

## 제약 조건

| 항목 | 규칙 |
|------|------|
| `System.out` | Service 클래스 내부에서 사용 금지 |
| Spring / Quarkus / Mockito | 사용 금지 |
| 테스트 격리 | `Files.createTempFile()`로 임시 파일 생성, `@AfterEach`에서 반드시 삭제 |
| 생산량 계산식 | `(int) Math.ceil(shortage / (sample.getYield() * 0.9))` 정확히 적용 |
| DEPLETED 판정 | `stock == 0` 조건 우선 (pendingDemand가 0이어도 stock==0이면 DEPLETED) |
| 불변 객체 패턴 | 상태 변경 시 `Order.withStatus()`, `Inventory.withStock()` 사용 |
| 생산 큐 공유 | `OrderService`와 `ProductionService`는 동일한 `List<ProductionItem>` 참조를 주입받아야 함 |
| `completeProduction` 재고 계산 | `Math.max(0, stock + actualProduction - quantity)` 적용 (음수 방지) |

---

## 완료 기준

1. `./gradlew test --tests "org.example.service.*"` GREEN (테스트 케이스 총 23개 통과)
   - `SampleServiceTest`: 6개
   - `OrderServiceTest`: 9개
   - `ProductionServiceTest`: 4개
   - `MonitorServiceTest`: 4개
2. `shortage=10, yield=0.9` → `actualProduction=13` 검증 통과
3. REJECTED 주문이 `getOrderCountByStatus()` 결과에 미포함 검증 통과
4. `stock==0` 시료에 pendingDemand가 있어도 DEPLETED 반환 검증 통과
5. `completeProduction` 후 재고가 `max(0, stock + actualProduction - quantity)` 값으로 업데이트됨 검증 통과
