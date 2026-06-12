# Phase 2 상세 설계 — Repository 레이어

> 이 문서는 PLAN.md Phase 2의 상세 버전이다. 완료 기준 및 구현 명세는 이 문서를 정본으로 삼으며, PLAN.md와 내용이 다른 경우 이 문서를 우선한다.

---

## Phase 목표

`CrudRepository<T, ID>` 인터페이스를 구현하는 3개의 JSON 파일 기반 저장소를 작성하고, 각각에 대한 단위 테스트를 작성한다.  
모든 저장소는 **read-modify-write** 패턴을 사용하며, 생성자 주입으로 File을 받아 테스트 격리를 보장한다.

---

## FR-N / NFR-N 매핑

| 요구사항 | 설명 |
|---------|------|
| NFR-1   | 각 저장소가 `data/` 경로에 JSON 파일로 데이터를 영속화 |
| NFR-2   | 각 Repository 클래스에 대한 CRUD 단위 테스트 필수 |
| FR-1-1  | `SampleRepository.save()` — 시료 등록 후 즉시 파일에 저장 |
| FR-2-1  | `OrderRepository.save()` — 주문 접수 후 즉시 파일에 저장 |

---

## 변경 대상 파일 목록

```
src/main/java/org/example/repository/SampleRepository.java         (생성)
src/main/java/org/example/repository/OrderRepository.java          (생성)
src/main/java/org/example/repository/InventoryRepository.java      (생성)
src/test/java/org/example/repository/SampleRepositoryTest.java     (생성)
src/test/java/org/example/repository/OrderRepositoryTest.java      (생성)
src/test/java/org/example/repository/InventoryRepositoryTest.java  (생성)
```

---

## 구현 상세

### `src/main/java/org/example/repository/SampleRepository.java`

```java
package org.example.repository;

import org.example.model.Sample;
import org.example.util.JsonFileUtil;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class SampleRepository implements CrudRepository<Sample, String> {

    private final File file;

    public SampleRepository(File file) {
        this.file = file;
    }

    @Override
    public void save(Sample entity) {
        List<Sample> list = JsonFileUtil.readList(file, Sample.class);
        list.add(entity);
        JsonFileUtil.writeList(file, list);
    }

    @Override
    public Optional<Sample> findById(String id) {
        return JsonFileUtil.readList(file, Sample.class).stream()
                .filter(s -> s.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<Sample> findAll() {
        return JsonFileUtil.readList(file, Sample.class);
    }

    @Override
    public void update(Sample entity) {
        List<Sample> list = JsonFileUtil.readList(file, Sample.class);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(entity.getId())) {
                list.set(i, entity);
                break;
            }
        }
        JsonFileUtil.writeList(file, list);
    }

    @Override
    public void deleteById(String id) {
        List<Sample> list = JsonFileUtil.readList(file, Sample.class);
        list.removeIf(s -> s.getId().equals(id));
        JsonFileUtil.writeList(file, list);
    }

    public boolean existsById(String id) {
        return findById(id).isPresent();
    }
}
```

### `src/main/java/org/example/repository/OrderRepository.java`

```java
package org.example.repository;

import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.util.JsonFileUtil;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OrderRepository implements CrudRepository<Order, String> {

    private final File file;

    public OrderRepository(File file) {
        this.file = file;
    }

    @Override
    public void save(Order entity) {
        List<Order> list = JsonFileUtil.readList(file, Order.class);
        list.add(entity);
        JsonFileUtil.writeList(file, list);
    }

    @Override
    public Optional<Order> findById(String id) {
        return JsonFileUtil.readList(file, Order.class).stream()
                .filter(o -> o.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<Order> findAll() {
        return JsonFileUtil.readList(file, Order.class);
    }

    @Override
    public void update(Order entity) {
        List<Order> list = JsonFileUtil.readList(file, Order.class);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(entity.getId())) {
                list.set(i, entity);
                break;
            }
        }
        JsonFileUtil.writeList(file, list);
    }

    @Override
    public void deleteById(String id) {
        List<Order> list = JsonFileUtil.readList(file, Order.class);
        list.removeIf(o -> o.getId().equals(id));
        JsonFileUtil.writeList(file, list);
    }

    public List<Order> findByStatus(OrderStatus status) {
        return JsonFileUtil.readList(file, Order.class).stream()
                .filter(o -> o.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<Order> findBySampleId(String sampleId) {
        return JsonFileUtil.readList(file, Order.class).stream()
                .filter(o -> o.getSampleId().equals(sampleId))
                .collect(Collectors.toList());
    }
}
```

### `src/main/java/org/example/repository/InventoryRepository.java`

```java
package org.example.repository;

import org.example.model.Inventory;
import org.example.util.JsonFileUtil;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class InventoryRepository implements CrudRepository<Inventory, String> {

    private final File file;

    public InventoryRepository(File file) {
        this.file = file;
    }

    @Override
    public void save(Inventory entity) {
        List<Inventory> list = JsonFileUtil.readList(file, Inventory.class);
        list.add(entity);
        JsonFileUtil.writeList(file, list);
    }

    @Override
    public Optional<Inventory> findById(String sampleId) {
        return JsonFileUtil.readList(file, Inventory.class).stream()
                .filter(inv -> inv.getSampleId().equals(sampleId))
                .findFirst();
    }

    @Override
    public List<Inventory> findAll() {
        return JsonFileUtil.readList(file, Inventory.class);
    }

    @Override
    public void update(Inventory entity) {
        List<Inventory> list = JsonFileUtil.readList(file, Inventory.class);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getSampleId().equals(entity.getSampleId())) {
                list.set(i, entity);
                break;
            }
        }
        JsonFileUtil.writeList(file, list);
    }

    @Override
    public void deleteById(String sampleId) {
        List<Inventory> list = JsonFileUtil.readList(file, Inventory.class);
        list.removeIf(inv -> inv.getSampleId().equals(sampleId));
        JsonFileUtil.writeList(file, list);
    }

    public Inventory findOrCreate(String sampleId) {
        return findById(sampleId).orElseGet(() -> {
            Inventory inv = new Inventory(sampleId, 0);
            save(inv);
            return inv;
        });
    }
}
```

---

## 테스트 상세

### 테스트 공통 원칙

- `Files.createTempFile()`로 임시 파일 생성 — `data/` 실제 경로 사용 금지
- `@BeforeEach`: 임시 파일 생성 + Repository 인스턴스 생성
- `@AfterEach`: 임시 파일 삭제
- Mockito 사용 금지
- 테스트 메서드 명명: `메서드명_상황_기대결과` (영문 camelCase)
- `@DisplayName`: 한글로 작성

### `src/test/java/org/example/repository/SampleRepositoryTest.java`

```java
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
```

### `src/test/java/org/example/repository/OrderRepositoryTest.java`

```java
package org.example.repository;

import org.example.model.Order;
import org.example.model.OrderStatus;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrderRepository 테스트")
class OrderRepositoryTest {

    private File tempFile;
    private OrderRepository repo;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = Files.createTempFile("orders_", ".json").toFile();
        repo = new OrderRepository(tempFile);
    }

    @AfterEach
    void tearDown() {
        tempFile.delete();
    }

    @Test
    @DisplayName("save_주문저장_파일에기록됨")
    void save_order_savedToFile() {
        Order order = new Order("O001", "S001", "홍길동", 10, OrderStatus.RESERVED, "2026-06-12T10:00:00");
        repo.save(order);

        List<Order> all = repo.findAll();
        assertEquals(1, all.size());
        assertEquals("O001", all.get(0).getId());
    }

    @Test
    @DisplayName("findByStatus_RESERVED상태_해당주문만반환")
    void findByStatus_reserved_returnsOnlyReservedOrders() {
        repo.save(new Order("O001", "S001", "홍길동", 10, OrderStatus.RESERVED, "2026-06-12T10:00:00"));
        repo.save(new Order("O002", "S001", "김철수", 5, OrderStatus.CONFIRMED, "2026-06-12T11:00:00"));
        repo.save(new Order("O003", "S002", "이영희", 20, OrderStatus.RESERVED, "2026-06-12T12:00:00"));

        List<Order> reserved = repo.findByStatus(OrderStatus.RESERVED);
        assertEquals(2, reserved.size());
        assertTrue(reserved.stream().allMatch(o -> o.getStatus() == OrderStatus.RESERVED));
    }

    @Test
    @DisplayName("findBySampleId_특정시료ID_해당주문목록반환")
    void findBySampleId_specificId_returnsMatchingOrders() {
        repo.save(new Order("O001", "S001", "홍길동", 10, OrderStatus.RESERVED, "2026-06-12T10:00:00"));
        repo.save(new Order("O002", "S002", "김철수", 5, OrderStatus.RESERVED, "2026-06-12T11:00:00"));
        repo.save(new Order("O003", "S001", "이영희", 20, OrderStatus.CONFIRMED, "2026-06-12T12:00:00"));

        List<Order> result = repo.findBySampleId("S001");
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(o -> o.getSampleId().equals("S001")));
    }

    @Test
    @DisplayName("update_상태변경후업데이트_변경된상태반환")
    void update_statusChanged_updatedStatusReflected() {
        Order order = new Order("O001", "S001", "홍길동", 10, OrderStatus.RESERVED, "2026-06-12T10:00:00");
        repo.save(order);

        Order updated = order.withStatus(OrderStatus.CONFIRMED);
        repo.update(updated);

        Order found = repo.findById("O001").orElseThrow();
        assertEquals(OrderStatus.CONFIRMED, found.getStatus());
    }

    @Test
    @DisplayName("재시작후findAll_같은파일로새인스턴스생성_데이터복원됨")
    void findAll_afterRestartWithSameFile_restoresData() {
        repo.save(new Order("O001", "S001", "홍길동", 10, OrderStatus.RESERVED, "2026-06-12T10:00:00"));
        repo.save(new Order("O002", "S002", "김철수", 5, OrderStatus.CONFIRMED, "2026-06-12T11:00:00"));

        // 앱 재시작 시뮬레이션: 같은 파일로 새 Repository 인스턴스 생성
        OrderRepository restarted = new OrderRepository(tempFile);
        List<Order> restored = restarted.findAll();

        assertEquals(2, restored.size());
        assertTrue(restored.stream().anyMatch(o -> o.getId().equals("O001") && o.getStatus() == OrderStatus.RESERVED));
        assertTrue(restored.stream().anyMatch(o -> o.getId().equals("O002") && o.getStatus() == OrderStatus.CONFIRMED));
    }
}
```

### `src/test/java/org/example/repository/InventoryRepositoryTest.java`

```java
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
```

---

## 제약 조건

- 테스트에서 `data/` 실제 경로 사용 금지 — `Files.createTempFile()`로 격리된 임시 파일 사용
- `@AfterEach`에서 임시 파일 반드시 삭제
- Mockito 사용 금지
- 모든 저장소 메서드는 read-modify-write 패턴 필수 적용 (캐시 사용 금지)
- `CrudRepository<T, ID>` 인터페이스의 5개 메서드 모두 구현 필수

---

## 완료 기준

1. `./gradlew test --tests "org.example.repository.*"` GREEN (17개 테스트 모두 통과 — SampleRepositoryTest 8, OrderRepositoryTest 5, InventoryRepositoryTest 4)
2. 3개 저장소 모두 `CrudRepository<T, ID>` 인터페이스 완전 구현
3. 재실행 후 `findAll()` 호출로 데이터 복원 확인 가능 (파일 영속화 검증)
4. `SampleRepository.existsById()`, `OrderRepository.findByStatus()`, `OrderRepository.findBySampleId()`, `InventoryRepository.findOrCreate()` 추가 메서드 동작 검증
