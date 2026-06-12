# PLAN.md — 반도체 시료 생산주문관리 시스템

**문서 버전**: 1.0  
**작성일**: 2026-06-12  
**프로젝트명**: SampleOrderSystem-PARKCHIWON-17061288

---

## 전제 조건 및 전역 규칙

- **패키지 루트**: `org.example`
- **Java 버전**: 17
- **빌드 도구**: Gradle Wrapper (`gradlew`)
- **JSON 저장 경로**: 프로젝트 루트 `data/` 디렉터리 (`data/samples.json`, `data/orders.json`, `data/inventory.json`)
- **모델 불변성**: 모든 모델 클래스는 `final` 필드 + getter 전용. 기본 생성자·setter 금지
- **Jackson 역직렬화**: 모든 모델 생성자에 `@JsonCreator` + `@JsonProperty` 적용
- **System.out 제한**: `ConsoleView` 클래스에서만 허용
- **Mock 금지**: 테스트는 실제 임시 파일 또는 인메모리 인스턴스 사용
- **생산량 계산식**: `int actualProduction = (int) Math.ceil(shortage / (yield * 0.9));`
- **테스트 메서드 네이밍**: `메서드명_상황_기대결과` 패턴
- **@DisplayName**: 한국어 필수

---

## Phase 1: 기반 구조

### Phase 목표

프로젝트가 컴파일 가능한 상태가 되도록 의존성을 추가하고, 도메인 모델 클래스·CRUD 인터페이스·JSON 유틸리티를 구현한다.

### FR-N / NFR-N 매핑

| 요구사항 | 설명 |
|---------|------|
| NFR-1 | JSON 파일 기반 데이터 영속성 기반 마련 (Jackson ObjectMapper prettyPrint) |
| NFR-3 | 패키지 구조 수립, model / util / repository 인터페이스 골격 |
| NFR-4 | 불변 모델(final 필드), SRP 원칙 준수 |

### 변경 대상 파일 목록

```
build.gradle                                                    (수정)
src/main/java/org/example/model/Sample.java                    (생성)
src/main/java/org/example/model/Order.java                     (생성)
src/main/java/org/example/model/OrderStatus.java               (생성)
src/main/java/org/example/model/Inventory.java                 (생성)
src/main/java/org/example/model/InventoryStatus.java           (생성)
src/main/java/org/example/model/ProductionItem.java            (생성)
src/main/java/org/example/repository/CrudRepository.java       (생성)
src/main/java/org/example/util/JsonFileUtil.java               (생성)
```

### 구현 상세

#### `build.gradle` 수정

기존 `dependencies` 블록에 두 의존성을 추가한다. `junit-bom:6.0.0` 고정 버전은 변경하지 않는다.

```groovy
plugins {
    id 'java'
    id 'application'
}

application {
    mainClass = 'org.example.Main'
}

dependencies {
    // 기존 junit 의존성 유지 (변경 금지)
    testImplementation platform('org.junit:junit-bom:6.0.0')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'

    // 추가
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.18.3'
    implementation 'com.github.javafaker:javafaker:1.0.2'
}
```

#### `model/Sample.java`

```java
package org.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Sample {
    private final String id;
    private final String name;
    private final long avgProductionTime;   // 분 단위
    private final double yield;             // 0.0 ~ 1.0

    @JsonCreator
    public Sample(
        @JsonProperty("id")                String id,
        @JsonProperty("name")              String name,
        @JsonProperty("avgProductionTime") long avgProductionTime,
        @JsonProperty("yield")             double yield
    ) { /* 필드 할당 */ }

    // getter: getId(), getName(), getAvgProductionTime(), getYield()
    // toString(): "Sample{id='...', name='...', avgProductionTime=..., yield=...}"
}
```

#### `model/OrderStatus.java`

```java
package org.example.model;

public enum OrderStatus {
    RESERVED, REJECTED, PRODUCING, CONFIRMED, RELEASE
}
```

#### `model/Order.java`

```java
package org.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Order {
    private final String id;            // UUID 자동 생성
    private final String sampleId;
    private final String customerName;
    private final int quantity;
    private final OrderStatus status;
    private final String createdAt;     // ISO-8601 문자열 (LocalDateTime.now().toString())

    @JsonCreator
    public Order(
        @JsonProperty("id")           String id,
        @JsonProperty("sampleId")     String sampleId,
        @JsonProperty("customerName") String customerName,
        @JsonProperty("quantity")     int quantity,
        @JsonProperty("status")       OrderStatus status,
        @JsonProperty("createdAt")    String createdAt
    ) { /* 필드 할당 */ }

    // getter 6개
    // 상태 변경은 새 Order 인스턴스를 반환하는 withStatus(OrderStatus) 제공
    public Order withStatus(OrderStatus newStatus) {
        return new Order(id, sampleId, customerName, quantity, newStatus, createdAt);
    }
}
```

#### `model/InventoryStatus.java`

```java
package org.example.model;

public enum InventoryStatus {
    SUFFICIENT,   // stock >= pendingDemand
    SHORTAGE,     // stock > 0 && stock < pendingDemand
    DEPLETED      // stock == 0
}
```

#### `model/Inventory.java`

```java
package org.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Inventory {
    private final String sampleId;
    private final int stock;

    @JsonCreator
    public Inventory(
        @JsonProperty("sampleId") String sampleId,
        @JsonProperty("stock")    int stock
    ) { /* 필드 할당 */ }

    // getter: getSampleId(), getStock()
    // 재고 변경은 새 인스턴스 반환
    public Inventory withStock(int newStock) {
        return new Inventory(sampleId, newStock);
    }
}
```

#### `model/ProductionItem.java`

```java
package org.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ProductionItem {
    private final String orderId;
    private final String sampleId;
    private final int requiredQuantity;       // shortage (부족 수량)
    private final int actualProduction;       // ceil(shortage / (yield * 0.9))
    private final long totalProductionTime;   // avgProductionTime * actualProduction (분)
    private final String enqueuedAt;          // ISO-8601 문자열

    @JsonCreator
    public ProductionItem(
        @JsonProperty("orderId")             String orderId,
        @JsonProperty("sampleId")            String sampleId,
        @JsonProperty("requiredQuantity")    int requiredQuantity,
        @JsonProperty("actualProduction")    int actualProduction,
        @JsonProperty("totalProductionTime") long totalProductionTime,
        @JsonProperty("enqueuedAt")          String enqueuedAt
    ) { /* 필드 할당 */ }

    // getter 6개
}
```

#### `repository/CrudRepository.java`

```java
package org.example.repository;

import java.util.List;
import java.util.Optional;

public interface CrudRepository<T, ID> {
    void save(T entity);               // 신규 엔티티 추가 후 파일 즉시 저장
    Optional<T> findById(ID id);
    List<T> findAll();
    void update(T entity);             // 기존 엔티티 교체 후 파일 즉시 저장
    void deleteById(ID id);            // 해당 ID 엔티티 제거 후 파일 즉시 저장
}
```

#### `util/JsonFileUtil.java`

```java
package org.example.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsonFileUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    // 파일이 없거나 비어있으면 빈 리스트 반환
    public static <T> List<T> readList(File file, Class<T> clazz) throws IOException {
        if (!file.exists() || file.length() == 0) return new ArrayList<>();
        return MAPPER.readValue(file,
            MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
    }

    // 부모 디렉터리(data/) 없으면 자동 생성 후 prettyPrint 저장
    public static <T> void writeList(File file, List<T> list) throws IOException {
        file.getParentFile().mkdirs();
        MAPPER.writeValue(file, list);
    }
}
```

- IOException은 RuntimeException으로 래핑하여 전파 허용

### 제약 조건

- `build.gradle`의 JUnit bom 버전(`junit-bom:6.0.0`) 변경 금지
- `settings.gradle`, `gradlew`, `gradlew.bat`, `.gitignore` 수정 금지
- 모델 클래스에 setter·기본 생성자 추가 금지

### 완료 기준

- `./gradlew compileJava` 에러 없이 완료
- 6개 모델 클래스, `CrudRepository` 인터페이스, `JsonFileUtil` 모두 컴파일 성공

---

## Phase 2: 저장소 레이어

### Phase 목표

`CrudRepository` 인터페이스를 구현하는 3개의 JSON 파일 기반 저장소를 작성하고, 각각에 대한 단위 테스트를 작성한다.

### FR-N / NFR-N 매핑

| 요구사항 | 설명 |
|---------|------|
| NFR-1 | 각 저장소가 `data/` 경로에 JSON 파일로 데이터를 영속화 |
| NFR-2 | 각 Repository 클래스에 대한 CRUD 테스트 필수 |
| FR-1-1 | `SampleRepository.save()` — 시료 등록 및 즉시 저장 |
| FR-2-1 | `OrderRepository.save()` — 주문 접수 후 즉시 저장 |

### 변경 대상 파일 목록

```
src/main/java/org/example/repository/SampleRepository.java         (생성)
src/main/java/org/example/repository/OrderRepository.java          (생성)
src/main/java/org/example/repository/InventoryRepository.java      (생성)
src/test/java/org/example/repository/SampleRepositoryTest.java     (생성)
src/test/java/org/example/repository/OrderRepositoryTest.java      (생성)
src/test/java/org/example/repository/InventoryRepositoryTest.java  (생성)
```

### 구현 상세

#### `repository/SampleRepository.java`

```java
package org.example.repository;

import org.example.model.Sample;
import org.example.util.JsonFileUtil;
import java.io.File;
import java.util.List;
import java.util.Optional;

public class SampleRepository implements CrudRepository<Sample, String> {
    private final File file;  // 생성자 주입 — 테스트에서 임시 파일 사용 가능

    public SampleRepository(File file) { this.file = file; }

    @Override public void save(Sample entity) { /* readList → add → writeList */ }
    @Override public Optional<Sample> findById(String id) { /* stream findFirst */ }
    @Override public List<Sample> findAll() { /* readList */ }
    @Override public void update(Sample entity) { /* readList → replaceIf id 일치 → writeList */ }
    @Override public void deleteById(String id) { /* readList → removeIf → writeList */ }

    public boolean existsById(String id) { return findById(id).isPresent(); }
}
```

모든 메서드는 **read-modify-write** 패턴 적용 (파일에서 읽기 → 메모리 수정 → 파일 쓰기).

#### `repository/OrderRepository.java`

```java
package org.example.repository;

import org.example.model.Order;
import org.example.model.OrderStatus;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OrderRepository implements CrudRepository<Order, String> {
    private final File file;

    public OrderRepository(File file) { this.file = file; }

    @Override public void save(Order entity) { ... }
    @Override public Optional<Order> findById(String id) { ... }
    @Override public List<Order> findAll() { ... }
    @Override public void update(Order entity) { /* id 기준 교체 */ }
    @Override public void deleteById(String id) { ... }

    public List<Order> findByStatus(OrderStatus status) {
        return findAll().stream()
            .filter(o -> o.getStatus() == status)
            .collect(Collectors.toList());
    }

    public List<Order> findBySampleId(String sampleId) {
        return findAll().stream()
            .filter(o -> o.getSampleId().equals(sampleId))
            .collect(Collectors.toList());
    }
}
```

#### `repository/InventoryRepository.java`

```java
package org.example.repository;

import org.example.model.Inventory;
import java.io.File;
import java.util.List;
import java.util.Optional;

public class InventoryRepository implements CrudRepository<Inventory, String> {
    private final File file;  // data/inventory.json

    public InventoryRepository(File file) { this.file = file; }

    @Override public void save(Inventory entity) { ... }
    @Override public Optional<Inventory> findById(String sampleId) { ... }
    @Override public List<Inventory> findAll() { ... }
    @Override public void update(Inventory entity) { /* sampleId 기준 교체 */ }
    @Override public void deleteById(String sampleId) { ... }

    // 재고가 없는 시료에 대해 stock=0으로 자동 생성
    public Inventory findOrCreate(String sampleId) {
        return findById(sampleId).orElseGet(() -> {
            Inventory inv = new Inventory(sampleId, 0);
            save(inv);
            return inv;
        });
    }
}
```

#### 테스트 파일

**`SampleRepositoryTest.java`**

```java
@DisplayName("SampleRepository 테스트")
class SampleRepositoryTest {
    private File tempFile;
    private SampleRepository repo;

    @BeforeEach void setUp() throws IOException {
        tempFile = Files.createTempFile("samples", ".json").toFile();
        repo = new SampleRepository(tempFile);
    }
    @AfterEach void tearDown() { tempFile.delete(); }

    @Test @DisplayName("save_새로운시료_파일에저장됨")
    void save_newSample_savedToFile() { ... }

    @Test @DisplayName("findById_존재하는ID_시료반환")
    void findById_existingId_returnsSample() { ... }

    @Test @DisplayName("findById_존재하지않는ID_빈Optional반환")
    void findById_nonExistingId_returnsEmpty() { ... }

    @Test @DisplayName("findAll_여러시료저장후_전체목록반환")
    void findAll_multipleSamples_returnsAll() { ... }

    @Test @DisplayName("update_기존시료수정_변경사항반영됨")
    void update_existingSample_updatesFile() { ... }

    @Test @DisplayName("deleteById_존재하는ID_삭제됨")
    void deleteById_existingId_removed() { ... }

    @Test @DisplayName("existsById_저장된시료ID_true반환")
    void existsById_savedId_returnsTrue() { ... }
}
```

**`OrderRepositoryTest.java`**

```java
@DisplayName("OrderRepository 테스트")
class OrderRepositoryTest {
    @Test @DisplayName("save_주문저장_파일에기록됨")
    void save_order_savedToFile() { ... }

    @Test @DisplayName("findByStatus_RESERVED상태_해당주문만반환")
    void findByStatus_reserved_returnsOnlyReservedOrders() { ... }

    @Test @DisplayName("findBySampleId_특정시료ID_해당주문목록반환")
    void findBySampleId_specificId_returnsMatchingOrders() { ... }

    @Test @DisplayName("update_상태변경후업데이트_변경된상태반환")
    void update_statusChanged_updatedStatusReflected() { ... }
}
```

**`InventoryRepositoryTest.java`**

```java
@DisplayName("InventoryRepository 테스트")
class InventoryRepositoryTest {
    @Test @DisplayName("findOrCreate_존재하지않는시료_재고0으로생성")
    void findOrCreate_nonExistingSampleId_createsWithZeroStock() { ... }

    @Test @DisplayName("findOrCreate_이미존재하는시료_기존재고반환")
    void findOrCreate_existingSampleId_returnsExistingInventory() { ... }

    @Test @DisplayName("update_재고변경후업데이트_변경된재고반환")
    void update_stockChanged_updatedStockReflected() { ... }
}
```

### 제약 조건

- 테스트에서 `data/` 실제 경로 사용 금지 — `Files.createTempFile()`로 격리된 임시 파일 사용
- `@AfterEach`에서 임시 파일 반드시 삭제
- Mockito 사용 금지

### 완료 기준

- `./gradlew test --tests "org.example.repository.*"` GREEN
- 3개 저장소 모두 `CrudRepository<T, ID>` 인터페이스 완전 구현
- 재실행 후 `findAll()` 호출로 데이터 복원 확인 가능

---

## Phase 3: 서비스 레이어

### Phase 목표

핵심 비즈니스 로직(시료 관리, 주문 처리, 생산 라인, 모니터링)을 Service 클래스에 구현하고, 단위 테스트로 검증한다.

### FR-N / NFR-N 매핑

| 요구사항 | 설명 |
|---------|------|
| FR-1-1 | `SampleService.register()` — 중복 ID 시 IllegalArgumentException |
| FR-1-2 | `SampleService.findAll()` — 전체 시료 목록 반환 |
| FR-1-3 | `SampleService.search()` — 이름 부분 문자열 검색 (대소문자 무시) |
| FR-2-1 | `OrderService.placeOrder()` — RESERVED 상태로 주문 생성, 미등록 시료 예외 |
| FR-2-2 | `OrderService.approve()` — 재고 충분: CONFIRMED, 재고 부족: PRODUCING + ProductionItem 등록 |
| FR-2-3 | `OrderService.reject()` — RESERVED → REJECTED |
| FR-3-1 | `MonitorService.getOrderCountByStatus()` — REJECTED 제외 상태별 집계 |
| FR-3-2 | `MonitorService.getInventoryStatus()` — pendingDemand 기반 상태 판정 |
| FR-4-1 | `OrderService.release()` — CONFIRMED → RELEASE |
| FR-5-1 | `ProductionService.getActiveProductions()` — PRODUCING 상태 주문 목록 |
| FR-5-2 | `ProductionService.getQueueStatus()` — 생산 대기 큐 FIFO 순서 |
| FR-5-3 | `ProductionService.completeProduction()` — 재고 업데이트 후 CONFIRMED 전환 |
| NFR-2 | 각 Service 핵심 메서드 단위 테스트 |

### 변경 대상 파일 목록

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

### 구현 상세

#### `service/SampleService.java`

```java
package org.example.service;

import org.example.model.Sample;
import org.example.repository.InventoryRepository;
import org.example.repository.SampleRepository;
import java.util.List;

public class SampleService {
    private final SampleRepository sampleRepo;
    private final InventoryRepository inventoryRepo;

    public SampleService(SampleRepository sampleRepo, InventoryRepository inventoryRepo) { ... }

    // FR-1-1: 중복 ID → IllegalArgumentException("이미 존재하는 시료 ID입니다: " + id)
    // 등록 성공 시 Inventory stock=0으로 초기화
    public Sample register(String id, String name, long avgProductionTime, double yield) { ... }

    // FR-1-2
    public List<Sample> findAll() { return sampleRepo.findAll(); }

    // FR-1-3: keyword null/blank → 전체 목록 반환
    public List<Sample> search(String keyword) {
        if (keyword == null || keyword.isBlank()) return findAll();
        return sampleRepo.findAll().stream()
            .filter(s -> s.getName().toLowerCase().contains(keyword.toLowerCase()))
            .toList();
    }
}
```

#### `service/OrderService.java`

```java
package org.example.service;

import org.example.model.*;
import org.example.repository.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class OrderService {
    private final OrderRepository orderRepo;
    private final SampleRepository sampleRepo;
    private final InventoryRepository inventoryRepo;
    private final List<ProductionItem> productionQueue;  // Main에서 주입, ProductionService와 공유

    public OrderService(OrderRepository orderRepo, SampleRepository sampleRepo,
                        InventoryRepository inventoryRepo, List<ProductionItem> productionQueue) { ... }

    // FR-2-1: sampleId 미존재 → IllegalArgumentException
    //         quantity <= 0 → IllegalArgumentException
    //         생성 상태: RESERVED
    public Order placeOrder(String sampleId, String customerName, int quantity) { ... }

    // FR-2-2: RESERVED가 아니면 IllegalStateException
    //   재고 충분(stock >= quantity):
    //     inventory.stock -= quantity → inventoryRepo.update()
    //     order.status → CONFIRMED
    //   재고 부족(stock < quantity):
    //     shortage = quantity - stock
    //     actualProduction = (int) Math.ceil(shortage / (sample.getYield() * 0.9))
    //     totalProductionTime = sample.getAvgProductionTime() * actualProduction
    //     ProductionItem 생성 → productionQueue.add()
    //     order.status → PRODUCING
    public Order approve(String orderId) { ... }

    // FR-2-3: RESERVED가 아니면 IllegalStateException
    public Order reject(String orderId) { ... }

    // FR-4-1: CONFIRMED가 아니면 IllegalStateException
    public Order release(String orderId) { ... }

    public List<Order> findAll() { return orderRepo.findAll(); }
}
```

> **설계 결정**: `ProductionItem` 목록은 런타임 `List<ProductionItem>`으로 관리한다.  
> `Main`에서 `new ArrayList<>()`를 생성하여 `OrderService`와 `ProductionService` 양쪽에 동일 참조를 주입한다.  
> 단일 스레드 환경에 적합하며 구현 단순성을 유지한다 (재실행 시 생산 큐는 초기화됨).

#### `service/ProductionService.java`

```java
package org.example.service;

import org.example.model.*;
import org.example.repository.*;
import java.util.List;

public class ProductionService {
    private final List<ProductionItem> productionQueue;  // OrderService와 공유 참조
    private final OrderRepository orderRepo;
    private final InventoryRepository inventoryRepo;

    public ProductionService(List<ProductionItem> productionQueue,
                             OrderRepository orderRepo, InventoryRepository inventoryRepo) { ... }

    // FR-5-1: PRODUCING 상태 주문 목록
    public List<Order> getActiveProductions() {
        return orderRepo.findByStatus(OrderStatus.PRODUCING);
    }

    // FR-5-2: FIFO 순서 (List 삽입 순서 = 큐 등록 순서)
    public List<ProductionItem> getQueueStatus() {
        return List.copyOf(productionQueue);
    }

    // FR-5-3:
    //   1. productionQueue에서 orderId 일치 항목 검색 (없으면 IllegalArgumentException)
    //   2. inventory.stock += item.actualProduction
    //   3. inventory.stock -= order.quantity (출고 대기를 위한 수량 차감)
    //   4. inventoryRepo.update() (Math.max(0, newStock) 적용)
    //   5. order.status → CONFIRMED, orderRepo.update()
    //   6. productionQueue에서 해당 item 제거
    public Order completeProduction(String orderId) { ... }
}
```

#### `service/MonitorService.java`

```java
package org.example.service;

import org.example.model.*;
import org.example.repository.*;
import java.util.*;
import java.util.stream.Collectors;

public class MonitorService {
    private final OrderRepository orderRepo;
    private final InventoryRepository inventoryRepo;
    private final SampleRepository sampleRepo;

    public MonitorService(OrderRepository orderRepo, InventoryRepository inventoryRepo,
                          SampleRepository sampleRepo) { ... }

    // FR-3-1: REJECTED 제외, Map<OrderStatus, Long> 반환
    public Map<OrderStatus, Long> getOrderCountByStatus() {
        return orderRepo.findAll().stream()
            .filter(o -> o.getStatus() != OrderStatus.REJECTED)
            .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
    }

    // FR-3-2: Map<String(sampleId), InventoryStatus> 반환
    // pendingDemand = 해당 시료의 RESERVED 주문량 합 + PRODUCING 주문량 합
    // DEPLETED 우선 판정 (stock == 0, pendingDemand 무관)
    // SHORTAGE: stock > 0 && stock < pendingDemand
    // SUFFICIENT: stock >= pendingDemand
    public Map<String, InventoryStatus> getInventoryStatus() { ... }
}
```

#### 테스트 파일

**`SampleServiceTest.java`**

```java
@DisplayName("SampleService 테스트")
class SampleServiceTest {
    @Test @DisplayName("register_새로운시료_저장소에추가됨")
    void register_newSample_addedToRepository() { ... }

    @Test @DisplayName("register_중복ID_IllegalArgumentException발생")
    void register_duplicateId_throwsIllegalArgumentException() { ... }

    @Test @DisplayName("register_시료등록시_재고0으로초기화됨")
    void register_newSample_inventoryInitializedWithZero() { ... }

    @Test @DisplayName("search_부분문자열_일치하는시료반환")
    void search_partialKeyword_returnsMatchingSamples() { ... }

    @Test @DisplayName("search_대소문자무시_일치하는시료반환")
    void search_caseInsensitive_returnsMatchingSamples() { ... }

    @Test @DisplayName("search_빈키워드_전체목록반환")
    void search_emptyKeyword_returnsAllSamples() { ... }
}
```

**`OrderServiceTest.java`**

```java
@DisplayName("OrderService 테스트")
class OrderServiceTest {
    @Test @DisplayName("placeOrder_유효한시료ID_RESERVED상태주문생성")
    void placeOrder_validSampleId_createsReservedOrder() { ... }

    @Test @DisplayName("placeOrder_존재하지않는시료ID_예외발생")
    void placeOrder_invalidSampleId_throwsException() { ... }

    @Test @DisplayName("approve_재고충분_CONFIRMED상태로전환")
    void approve_sufficientStock_setsConfirmed() { ... }

    @Test @DisplayName("approve_재고부족_PRODUCING상태로전환및생산큐등록")
    void approve_insufficientStock_setsProducingAndEnqueuesItem() { ... }

    @Test @DisplayName("approve_재고부족_생산량계산식검증")
    void approve_insufficientStock_calculatesActualProductionCorrectly() {
        // shortage=10, yield=0.9 → actualProduction = ceil(10 / (0.9*0.9)) = ceil(12.34) = 13
    }

    @Test @DisplayName("approve_RESERVED아닌상태_예외발생")
    void approve_nonReservedStatus_throwsIllegalStateException() { ... }

    @Test @DisplayName("reject_RESERVED상태_REJECTED로전환")
    void reject_reservedOrder_setsRejected() { ... }

    @Test @DisplayName("release_CONFIRMED상태_RELEASE로전환")
    void release_confirmedOrder_setsRelease() { ... }

    @Test @DisplayName("release_CONFIRMED아닌상태_예외발생")
    void release_nonConfirmedStatus_throwsIllegalStateException() { ... }
}
```

**`ProductionServiceTest.java`**

```java
@DisplayName("ProductionService 테스트")
class ProductionServiceTest {
    @Test @DisplayName("getActiveProductions_PRODUCING주문존재_목록반환")
    void getActiveProductions_hasProducingOrders_returnsList() { ... }

    @Test @DisplayName("getQueueStatus_큐에항목존재_FIFO순서반환")
    void getQueueStatus_queueHasItems_returnsInFifoOrder() { ... }

    @Test @DisplayName("completeProduction_생산완료_재고증가및CONFIRMED전환")
    void completeProduction_validOrderId_increasesStockAndSetsConfirmed() { ... }

    @Test @DisplayName("completeProduction_큐에없는주문ID_예외발생")
    void completeProduction_orderNotInQueue_throwsException() { ... }
}
```

**`MonitorServiceTest.java`**

```java
@DisplayName("MonitorService 테스트")
class MonitorServiceTest {
    @Test @DisplayName("getOrderCountByStatus_REJECTED제외_상태별건수반환")
    void getOrderCountByStatus_excludesRejected_returnsCountByStatus() { ... }

    @Test @DisplayName("getInventoryStatus_재고0인시료_DEPLETED반환")
    void getInventoryStatus_zeroStock_returnsDepleted() { ... }

    @Test @DisplayName("getInventoryStatus_재고부족_SHORTAGE반환")
    void getInventoryStatus_partialStock_returnsShortage() { ... }

    @Test @DisplayName("getInventoryStatus_재고충분_SUFFICIENT반환")
    void getInventoryStatus_sufficientStock_returnsSufficient() { ... }
}
```

### 제약 조건

- `System.out` 사용 금지 (서비스 로직 내)
- Spring / Quarkus / Mockito 사용 금지
- `approve()` 내 생산량 계산식 정확히 적용: `(int) Math.ceil(shortage / (sample.getYield() * 0.9))`
- `getInventoryStatus()` DEPLETED 판정은 `stock == 0` 조건 우선 적용 (pendingDemand 무관)

### 완료 기준

- `./gradlew test --tests "org.example.service.*"` GREEN
- `shortage=10, yield=0.9` → `actualProduction=13` 검증 통과
- REJECTED 주문이 `getOrderCountByStatus()` 결과에 미포함 검증 통과

---

## Phase 4: 프레젠테이션 레이어

### Phase 목표

`ConsoleView`(단일 I/O 담당)와 5개 Controller를 구현하고, `Main`에서 DI를 조립하여 전체 애플리케이션이 실행 가능한 상태로 만든다.

### FR-N / NFR-N 매핑

| 요구사항 | 설명 |
|---------|------|
| FR-1 ~ FR-5 전체 | 각 Controller가 Service를 호출하여 메뉴별 기능 실행 |
| NFR-3 | System.out은 ConsoleView에서만 호출, MVC 역할 분리 |
| NFR-4 | Controller는 조정 역할만 수행 (비즈니스 로직 없음) |

### 변경 대상 파일 목록

```
src/main/java/org/example/view/ConsoleView.java                   (생성)
src/main/java/org/example/controller/SampleController.java        (생성)
src/main/java/org/example/controller/OrderController.java         (생성)
src/main/java/org/example/controller/ProductionController.java    (생성)
src/main/java/org/example/controller/MonitorController.java       (생성)
src/main/java/org/example/controller/DummyController.java         (생성 — Phase 5에서 완성)
src/main/java/org/example/Main.java                               (생성)
```

### 구현 상세

#### `view/ConsoleView.java`

모든 `System.out.println` / `System.out.print` 호출은 이 클래스에서만 허용.

```java
package org.example.view;

import org.example.model.*;
import java.util.*;

public class ConsoleView {
    private final Scanner scanner;

    public ConsoleView(Scanner scanner) { this.scanner = scanner; }

    public void showMainMenu() {
        // 메인 메뉴 전체 출력 (1.시료관리 ~ 6.더미데이터 ~ 0.종료)
    }

    // 입력 메서드
    public String readLine(String prompt) { System.out.print(prompt); return scanner.nextLine().trim(); }
    public int readInt(String prompt) { /* readLine → Integer.parseInt, 실패 시 재입력 */ }
    public double readDouble(String prompt) { /* readLine → Double.parseDouble */ }
    public long readLong(String prompt) { /* readLine → Long.parseLong */ }

    // 출력 메서드
    public void showSuccess(String message) { System.out.println("[SUCCESS] " + message); }
    public void showError(String message) { System.out.println("[ERROR] " + message); }
    public void showInfo(String message) { System.out.println(message); }

    // 테이블 출력 메서드 (컬럼 구분: | 로 정렬)
    public void showSampleTable(List<Sample> samples) {
        // 컬럼: ID | 이름 | 평균생산시간(분) | 수율
    }

    public void showOrderTable(List<Order> orders) {
        // 컬럼: 주문ID | 시료ID | 고객명 | 수량 | 상태 | 접수일시
    }

    public void showInventoryStatusTable(Map<String, InventoryStatus> statusMap,
                                          List<Inventory> inventories) {
        // 컬럼: 시료ID | 재고 | 상태
    }

    public void showOrderCountTable(Map<OrderStatus, Long> countMap) {
        // 컬럼: 상태 | 건수
    }

    public void showProductionQueueTable(List<ProductionItem> queue) {
        // 컬럼: 주문ID | 시료ID | 필요수량 | 실생산량 | 총생산시간(분) | 큐등록일시
    }
}
```

#### `controller/SampleController.java`

```java
public class SampleController {
    private final SampleService sampleService;
    private final ConsoleView view;

    // handle("1") → register()   : id, name, avgProductionTime, yield 입력 → register()
    // handle("2") → listAll()    : findAll() → showSampleTable()
    // handle("3") → search()     : keyword 입력 → search() → showSampleTable()
    // 예외는 view.showError()로 출력
    public void handle(String subMenu) { ... }
}
```

#### `controller/OrderController.java`

```java
public class OrderController {
    private final OrderService orderService;
    private final ConsoleView view;

    // handle("1") → placeOrder() : sampleId, customerName, quantity 입력
    // handle("2") → approve()    : orderId 입력 → approve()
    // handle("3") → reject()     : orderId 입력 → reject()
    // handle("4") → release()    : orderId 입력 → release() (메인메뉴 4번에서 호출)
    public void handle(String subMenu) { ... }
}
```

#### `controller/ProductionController.java`

```java
public class ProductionController {
    private final ProductionService productionService;
    private final ConsoleView view;

    // handle("1") → getActiveProductions() → showOrderTable()
    // handle("2") → getQueueStatus() → showProductionQueueTable()
    // handle("3") → orderId 입력 → completeProduction()
    public void handle(String subMenu) { ... }
}
```

#### `controller/MonitorController.java`

```java
public class MonitorController {
    private final MonitorService monitorService;
    private final InventoryRepository inventoryRepo;
    private final ConsoleView view;

    // handle("1") → getOrderCountByStatus() → showOrderCountTable()
    // handle("2") → getInventoryStatus() + inventoryRepo.findAll() → showInventoryStatusTable()
    public void handle(String subMenu) { ... }
}
```

#### `controller/DummyController.java` (Phase 4 스텁)

```java
public class DummyController {
    private final ConsoleView view;

    public DummyController(ConsoleView view) { this.view = view; }

    public void generateSamples() {
        view.showInfo("더미 시료 데이터 생성 기능은 Phase 5에서 구현됩니다.");
    }

    public void generateOrders() {
        view.showInfo("더미 주문 데이터 생성 기능은 Phase 5에서 구현됩니다.");
    }

    public void handle(String subMenu) {
        switch (subMenu) {
            case "1" -> generateSamples();
            case "2" -> generateOrders();
            default  -> view.showError("잘못된 메뉴 입력입니다.");
        }
    }
}
```

#### `Main.java`

```java
package org.example;

import org.example.model.ProductionItem;
import org.example.repository.*;
import org.example.service.*;
import org.example.controller.*;
import org.example.view.ConsoleView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // 1. JSON 파일
        File samplesFile   = new File("data/samples.json");
        File ordersFile    = new File("data/orders.json");
        File inventoryFile = new File("data/inventory.json");

        // 2. Repository
        SampleRepository    sampleRepo    = new SampleRepository(samplesFile);
        OrderRepository     orderRepo     = new OrderRepository(ordersFile);
        InventoryRepository inventoryRepo = new InventoryRepository(inventoryFile);

        // 3. 공유 생산 큐 (OrderService와 ProductionService가 동일 참조 사용)
        List<ProductionItem> productionQueue = new ArrayList<>();

        // 4. Service
        SampleService     sampleService     = new SampleService(sampleRepo, inventoryRepo);
        OrderService      orderService      = new OrderService(orderRepo, sampleRepo, inventoryRepo, productionQueue);
        ProductionService productionService = new ProductionService(productionQueue, orderRepo, inventoryRepo);
        MonitorService    monitorService    = new MonitorService(orderRepo, inventoryRepo, sampleRepo);

        // 5. View / Controller
        ConsoleView view = new ConsoleView(new Scanner(System.in));
        SampleController     sampleCtrl     = new SampleController(sampleService, view);
        OrderController      orderCtrl      = new OrderController(orderService, view);
        ProductionController productionCtrl = new ProductionController(productionService, view);
        MonitorController    monitorCtrl    = new MonitorController(monitorService, inventoryRepo, view);
        DummyController      dummyCtrl      = new DummyController(view);

        // 6. 메인 루프
        while (true) {
            view.showMainMenu();
            String mainMenu = view.readLine("메뉴 선택: ");
            if ("0".equals(mainMenu)) { view.showInfo("시스템을 종료합니다."); break; }
            String subMenu = view.readLine("세부 메뉴 선택: ");
            try {
                switch (mainMenu) {
                    case "1" -> sampleCtrl.handle(subMenu);
                    case "2" -> orderCtrl.handle(subMenu);
                    case "3" -> monitorCtrl.handle(subMenu);
                    case "4" -> orderCtrl.handle("4");
                    case "5" -> productionCtrl.handle(subMenu);
                    case "6" -> dummyCtrl.handle(subMenu);
                    default  -> view.showError("잘못된 메뉴 입력입니다.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                view.showError(e.getMessage());
            }
        }
    }
}
```

### 제약 조건

- Controller 내부에 비즈니스 로직 금지 (Service 호출 + View 출력만 허용)
- `System.out` 직접 호출 금지 — `view.showSuccess()` / `view.showError()` / `view.showInfo()` 경유
- `Main.java`에서만 `new` 키워드로 객체 생성 (DI 조립점)

### 완료 기준

- `./gradlew run` 실행 시 메인 메뉴 출력 후 입력 대기
- 메뉴 1-1 시료 등록 → `data/samples.json` 생성 확인
- 메뉴 2-2 재고 부족 승인 → 주문 상태 PRODUCING 변경 확인
- `./gradlew build` 에러 없음

---

## Phase 5: 더미 데이터 도구

### Phase 목표

JavaFaker(한국 로케일)를 사용하여 현실적인 시료·주문 더미 데이터를 생성하는 Generator 클래스를 구현하고, `DummyController`를 완성한다.

### FR-N / NFR-N 매핑

| 요구사항 | 설명 |
|---------|------|
| FR-6-1 | `SampleGenerator` — JavaFaker로 시료 데이터 생성 후 `data/samples.json`에 저장 |
| FR-6-2 | `OrderGenerator` — 등록된 시료 ID 참조하여 주문 데이터 생성 후 `data/orders.json`에 저장 |

### 변경 대상 파일 목록

```
src/main/java/org/example/dummy/SampleGenerator.java              (생성)
src/main/java/org/example/dummy/OrderGenerator.java               (생성)
src/main/java/org/example/controller/DummyController.java         (수정 — 완성)
src/main/java/org/example/Main.java                               (수정 — DummyController DI 업데이트)
```

### 구현 상세

#### `dummy/SampleGenerator.java`

```java
package org.example.dummy;

import com.github.javafaker.Faker;
import org.example.model.Sample;
import org.example.repository.SampleRepository;
import org.example.repository.InventoryRepository;
import java.util.Locale;
import java.util.Random;

public class SampleGenerator {
    private final SampleRepository sampleRepo;
    private final InventoryRepository inventoryRepo;
    private final Faker faker = new Faker(new Locale("ko"));
    private final Random random = new Random();

    public SampleGenerator(SampleRepository sampleRepo, InventoryRepository inventoryRepo) { ... }

    // count 개의 시료 생성 후 저장
    // id: "S-" + System.currentTimeMillis() + "-" + i (중복 방지)
    // name: faker.commerce().productName() + " 시료"
    // avgProductionTime: 10 ~ 480 (분)
    // yield: 0.70 ~ 0.99 (소수점 2자리)
    // 등록 시 InventoryRepository에 stock=0으로 초기화
    public void generate(int count) { ... }
}
```

#### `dummy/OrderGenerator.java`

```java
package org.example.dummy;

import com.github.javafaker.Faker;
import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

public class OrderGenerator {
    private final OrderRepository orderRepo;
    private final SampleRepository sampleRepo;
    private final Faker faker = new Faker(new Locale("ko"));
    private final Random random = new Random();

    public OrderGenerator(OrderRepository orderRepo, SampleRepository sampleRepo) { ... }

    // count 개의 주문 생성 후 저장
    // 시료 없으면 IllegalStateException("시료 데이터가 없습니다. 먼저 시료 더미 데이터를 생성하세요.")
    // sampleId: 기존 시료 목록에서 랜덤 선택
    // customerName: faker.name().fullName()
    // quantity: 1 ~ 100
    // status: RESERVED
    public void generate(int count) {
        List<String> sampleIds = sampleRepo.findAll().stream().map(s -> s.getId()).toList();
        if (sampleIds.isEmpty())
            throw new IllegalStateException("시료 데이터가 없습니다. 먼저 시료 더미 데이터를 생성하세요.");
        for (int i = 0; i < count; i++) {
            String sampleId = sampleIds.get(random.nextInt(sampleIds.size()));
            Order order = new Order(UUID.randomUUID().toString(), sampleId,
                faker.name().fullName(), 1 + random.nextInt(100),
                OrderStatus.RESERVED, LocalDateTime.now().toString());
            orderRepo.save(order);
        }
    }
}
```

#### `controller/DummyController.java` (완성)

```java
public class DummyController {
    private final SampleGenerator sampleGenerator;
    private final OrderGenerator  orderGenerator;
    private final ConsoleView view;

    public DummyController(SampleGenerator sampleGenerator, OrderGenerator orderGenerator,
                           ConsoleView view) { ... }

    // 메뉴 6-1: 생성 개수 입력 → sampleGenerator.generate(count)
    public void generateSamples() { ... }

    // 메뉴 6-2: 생성 개수 입력 → orderGenerator.generate(count), 예외 시 showError
    public void generateOrders() { ... }

    public void handle(String subMenu) {
        switch (subMenu) {
            case "1" -> generateSamples();
            case "2" -> generateOrders();
            default  -> view.showError("잘못된 메뉴 입력입니다.");
        }
    }
}
```

#### `Main.java` 수정 (DummyController DI 부분)

```java
// Phase 4의 DummyController 생성 라인을 아래로 교체
SampleGenerator sampleGenerator = new SampleGenerator(sampleRepo, inventoryRepo);
OrderGenerator  orderGenerator  = new OrderGenerator(orderRepo, sampleRepo);
DummyController dummyCtrl = new DummyController(sampleGenerator, orderGenerator, view);
```

### 제약 조건

- JavaFaker 의존성은 Phase 1 `build.gradle`에 이미 추가되어 있어야 함
- 시료 없이 주문 생성 시도 시 생성 중단 + 명확한 메시지 출력 (시스템 종료 없음)
- 생성 데이터는 기존 데이터에 추가 (덮어쓰기 금지)
- `System.out` 호출 금지 — ConsoleView 경유

### 완료 기준

- 메뉴 6-1 실행 → `data/samples.json`에 faker 시료 데이터 추가 확인
- 메뉴 6-2 실행 → `data/orders.json`에 faker 주문 데이터 추가 확인
- 시료 없이 주문 생성 시 에러 메시지 출력 후 메뉴로 복귀 확인
- `./gradlew build` 에러 없음 / `./gradlew test` 전체 GREEN

---

## 전체 완료 기준 체크리스트

| 항목 | 확인 방법 |
|------|-----------|
| `./gradlew build` 성공 | BUILD SUCCESSFUL |
| `./gradlew test` 전체 GREEN | 7개 테스트 클래스 모두 PASS |
| JSON 파일 영속성 | 앱 재실행 후 `findAll()` 데이터 유지 확인 |
| 주문 상태 전이 정확성 | RESERVED→CONFIRMED, RESERVED→PRODUCING→CONFIRMED, CONFIRMED→RELEASE |
| 생산량 계산식 | `shortage=10, yield=0.9` → `actualProduction=13` |
| REJECTED 모니터링 제외 | `getOrderCountByStatus()` 결과에 REJECTED 없음 |
| System.out ConsoleView 독점 | grep으로 ConsoleView 외 System.out 없음 확인 |
| Mock 사용 금지 | build.gradle에 mockito 없음 확인 |

---

## 의존성 주입 다이어그램 (Main 기준)

```
Main
├── SampleRepository(samplesFile)
├── OrderRepository(ordersFile)
├── InventoryRepository(inventoryFile)
├── List<ProductionItem> productionQueue  ← OrderService, ProductionService 공유
│
├── SampleService(sampleRepo, inventoryRepo)
├── OrderService(orderRepo, sampleRepo, inventoryRepo, productionQueue)
├── ProductionService(productionQueue, orderRepo, inventoryRepo)
├── MonitorService(orderRepo, inventoryRepo, sampleRepo)
│
├── ConsoleView(Scanner)
├── SampleController(sampleService, view)
├── OrderController(orderService, view)
├── ProductionController(productionService, view)
├── MonitorController(monitorService, inventoryRepo, view)
│
├── SampleGenerator(sampleRepo, inventoryRepo)
├── OrderGenerator(orderRepo, sampleRepo)
└── DummyController(sampleGenerator, orderGenerator, view)
```
