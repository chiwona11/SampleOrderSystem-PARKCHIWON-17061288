# Phase 1 상세 설계 — 기반 구조

**Phase**: 1 / 5  
**작성일**: 2026-06-12  
**참조 문서**: PLAN.md, PRD.md, CLAUDE.md

> 이 문서는 `PLAN.md` Phase 1의 상세 버전이다. 완료 기준 및 구현 명세는 이 문서를 정본으로 삼으며, PLAN.md와 내용이 다른 경우 이 문서를 우선한다.

---

## Phase 목표

프로젝트가 컴파일 가능한 상태가 되도록:
1. `build.gradle`에 필요한 의존성 및 `application` 플러그인 추가
2. 6개 도메인 모델 클래스 구현
3. `CrudRepository<T, ID>` 제네릭 인터페이스 정의
4. `JsonFileUtil` JSON 파일 I/O 유틸리티 구현

---

## FR-N / NFR-N 매핑

| 요구사항 | 설명 |
|---------|------|
| NFR-1 | Jackson ObjectMapper prettyPrint 기반 JSON 영속성 유틸리티 마련 |
| NFR-3 | 패키지 구조 수립: `model`, `repository`, `util` 골격 |
| NFR-4 | 불변 모델(final 필드), SRP 원칙 준수 |
| PRD §4.1 | `Sample` 엔티티: id, name, avgProductionTime, yield |
| PRD §4.2 | `Order` 엔티티: id, sampleId, customerName, quantity, status, createdAt |
| PRD §4.3 | `Inventory` 엔티티: sampleId, stock |
| PRD §4.4 | `InventoryStatus` Enum: SUFFICIENT, SHORTAGE, DEPLETED |
| PRD §4.5 | `ProductionItem` 엔티티: orderId, sampleId, requiredQuantity, actualProduction, totalProductionTime, enqueuedAt |
| PRD §4.6 | `OrderStatus` Enum: RESERVED, REJECTED, PRODUCING, CONFIRMED, RELEASE |

---

## 변경 대상 파일 목록

| 파일 경로 | 작업 |
|-----------|------|
| `build.gradle` | 수정 |
| `src/main/java/org/example/model/Sample.java` | 생성 |
| `src/main/java/org/example/model/Order.java` | 생성 |
| `src/main/java/org/example/model/OrderStatus.java` | 생성 |
| `src/main/java/org/example/model/Inventory.java` | 생성 |
| `src/main/java/org/example/model/InventoryStatus.java` | 생성 |
| `src/main/java/org/example/model/ProductionItem.java` | 생성 |
| `src/main/java/org/example/repository/CrudRepository.java` | 생성 |
| `src/main/java/org/example/util/JsonFileUtil.java` | 생성 |

---

## 구현 상세

### 1. `build.gradle`

> **현재 상태**: `id 'java'` 플러그인, JUnit 의존성만 존재. `application` 플러그인·Jackson·JavaFaker가 없으므로 반드시 아래 내용을 추가해야 `./gradlew compileJava`가 성공한다.

현재 파일에서 아래 두 가지를 수정한다.

#### 1-1. `plugins` 블록에 `application` 플러그인 추가

```groovy
plugins {
    id 'java'
    id 'application'
}
```

#### 1-2. `application` 블록 추가 (plugins 바로 아래)

```groovy
application {
    mainClass = 'org.example.Main'
}
```

#### 1-3. `dependencies` 블록에 두 의존성 추가

```groovy
dependencies {
    testImplementation platform('org.junit:junit-bom:6.0.0')   // 변경 금지
    testImplementation 'org.junit.jupiter:junit-jupiter'        // 변경 금지
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher' // 변경 금지

    implementation 'com.fasterxml.jackson.core:jackson-databind:2.18.3'
    implementation 'com.github.javafaker:javafaker:1.0.2'
}
```

> **제약**: 기존 junit 관련 3줄은 절대 수정하지 않는다.

---

### 2. `model/OrderStatus.java`

```java
package org.example.model;

public enum OrderStatus {
    RESERVED,
    REJECTED,
    PRODUCING,
    CONFIRMED,
    RELEASE
}
```

- Jackson은 Enum을 기본적으로 문자열로 직렬화/역직렬화하므로 별도 애노테이션 불필요.

---

### 3. `model/InventoryStatus.java`

```java
package org.example.model;

public enum InventoryStatus {
    SUFFICIENT,
    SHORTAGE,
    DEPLETED
}
```

- 판정 로직은 `MonitorService`에 위치. 이 Enum은 값 정의만 포함.

---

### 4. `model/Sample.java`

#### 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `id` | `String` | 시료 고유 식별자 (사용자 입력 임의 문자열) |
| `name` | `String` | 시료 이름 |
| `avgProductionTime` | `long` | 평균 생산시간 (분 단위, 양수) |
| `yield` | `double` | 수율 (0.0 초과 ~ 1.0 이하) |

#### 전체 구현

```java
package org.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Sample {

    private final String id;
    private final String name;
    private final long avgProductionTime;
    private final double yield;

    @JsonCreator
    public Sample(
            @JsonProperty("id")                String id,
            @JsonProperty("name")              String name,
            @JsonProperty("avgProductionTime") long avgProductionTime,
            @JsonProperty("yield")             double yield) {
        this.id = id;
        this.name = name;
        this.avgProductionTime = avgProductionTime;
        this.yield = yield;
    }

    public String getId()               { return id; }
    public String getName()             { return name; }
    public long getAvgProductionTime()  { return avgProductionTime; }
    public double getYield()            { return yield; }

    @Override
    public String toString() {
        return "Sample{id='" + id + "', name='" + name +
               "', avgProductionTime=" + avgProductionTime +
               ", yield=" + yield + "}";
    }
}
```

---

### 5. `model/Order.java`

#### 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `id` | `String` | 주문 고유 식별자 (UUID, 자동 생성) |
| `sampleId` | `String` | 주문 시료 ID |
| `customerName` | `String` | 고객명 |
| `quantity` | `int` | 주문 수량 (1 이상) |
| `status` | `OrderStatus` | 주문 상태 |
| `createdAt` | `String` | 주문 생성 일시 (`LocalDateTime.now().toString()`) |

#### 전체 구현

```java
package org.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Order {

    private final String id;
    private final String sampleId;
    private final String customerName;
    private final int quantity;
    private final OrderStatus status;
    private final String createdAt;

    @JsonCreator
    public Order(
            @JsonProperty("id")           String id,
            @JsonProperty("sampleId")     String sampleId,
            @JsonProperty("customerName") String customerName,
            @JsonProperty("quantity")     int quantity,
            @JsonProperty("status")       OrderStatus status,
            @JsonProperty("createdAt")    String createdAt) {
        this.id = id;
        this.sampleId = sampleId;
        this.customerName = customerName;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId()           { return id; }
    public String getSampleId()     { return sampleId; }
    public String getCustomerName() { return customerName; }
    public int getQuantity()        { return quantity; }
    public OrderStatus getStatus()  { return status; }
    public String getCreatedAt()    { return createdAt; }

    // 상태 변경 시 새 인스턴스 반환 (불변 객체 유지)
    public Order withStatus(OrderStatus newStatus) {
        return new Order(id, sampleId, customerName, quantity, newStatus, createdAt);
    }

    @Override
    public String toString() {
        return "Order{id='" + id + "', sampleId='" + sampleId +
               "', customerName='" + customerName + "', quantity=" + quantity +
               ", status=" + status + ", createdAt='" + createdAt + "'}";
    }
}
```

---

### 6. `model/Inventory.java`

#### 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `sampleId` | `String` | 시료 ID (CrudRepository의 ID 역할) |
| `stock` | `int` | 현재 재고 수량 (0 이상) |

#### 전체 구현

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
            @JsonProperty("stock")    int stock) {
        this.sampleId = sampleId;
        this.stock = stock;
    }

    public String getSampleId() { return sampleId; }
    public int getStock()       { return stock; }

    // 재고 변경 시 새 인스턴스 반환 (불변 객체 유지)
    public Inventory withStock(int newStock) {
        return new Inventory(sampleId, newStock);
    }

    @Override
    public String toString() {
        return "Inventory{sampleId='" + sampleId + "', stock=" + stock + "}";
    }
}
```

---

### 7. `model/ProductionItem.java`

#### 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `orderId` | `String` | 연관 주문 ID |
| `sampleId` | `String` | 생산할 시료 ID |
| `requiredQuantity` | `int` | 부족 수량 (`shortage = quantity - stock`) |
| `actualProduction` | `int` | 실 생산량 (`ceil(shortage / (yield * 0.9))`) |
| `totalProductionTime` | `long` | 총 생산시간 (`avgProductionTime * actualProduction`, 분) |
| `enqueuedAt` | `String` | 큐 등록 일시 (`LocalDateTime.now().toString()`) |

#### 전체 구현

```java
package org.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ProductionItem {

    private final String orderId;
    private final String sampleId;
    private final int requiredQuantity;
    private final int actualProduction;
    private final long totalProductionTime;
    private final String enqueuedAt;

    @JsonCreator
    public ProductionItem(
            @JsonProperty("orderId")             String orderId,
            @JsonProperty("sampleId")            String sampleId,
            @JsonProperty("requiredQuantity")    int requiredQuantity,
            @JsonProperty("actualProduction")    int actualProduction,
            @JsonProperty("totalProductionTime") long totalProductionTime,
            @JsonProperty("enqueuedAt")          String enqueuedAt) {
        this.orderId = orderId;
        this.sampleId = sampleId;
        this.requiredQuantity = requiredQuantity;
        this.actualProduction = actualProduction;
        this.totalProductionTime = totalProductionTime;
        this.enqueuedAt = enqueuedAt;
    }

    public String getOrderId()            { return orderId; }
    public String getSampleId()           { return sampleId; }
    public int getRequiredQuantity()      { return requiredQuantity; }
    public int getActualProduction()      { return actualProduction; }
    public long getTotalProductionTime()  { return totalProductionTime; }
    public String getEnqueuedAt()         { return enqueuedAt; }

    @Override
    public String toString() {
        return "ProductionItem{orderId='" + orderId + "', sampleId='" + sampleId +
               "', requiredQuantity=" + requiredQuantity +
               ", actualProduction=" + actualProduction +
               ", totalProductionTime=" + totalProductionTime +
               ", enqueuedAt='" + enqueuedAt + "'}";
    }
}
```

---

### 8. `repository/CrudRepository.java`

```java
package org.example.repository;

import java.util.List;
import java.util.Optional;

public interface CrudRepository<T, ID> {

    // 신규 엔티티 추가 후 파일에 즉시 저장
    void save(T entity);

    // ID로 단건 조회. 없으면 Optional.empty() 반환
    Optional<T> findById(ID id);

    // 전체 목록 조회
    List<T> findAll();

    // 기존 엔티티 교체 후 파일에 즉시 저장 (ID 기준 매칭)
    void update(T entity);

    // ID 기준으로 엔티티 삭제 후 파일에 즉시 저장
    void deleteById(ID id);
}
```

---

### 9. `util/JsonFileUtil.java`

#### 설계 원칙
- Jackson `ObjectMapper`를 `static final` 싱글턴으로 보유 (`INDENT_OUTPUT` 활성화)
- `readList`: 파일 없거나 비어 있으면 빈 `ArrayList` 반환 (예외 발생 금지)
- `writeList`: 부모 디렉터리(`data/`) 없으면 자동 생성 후 저장
- `IOException`은 `RuntimeException`으로 래핑하여 전파

#### 전체 구현

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

    private JsonFileUtil() {}

    /**
     * JSON 파일에서 List<T>를 읽어 반환한다.
     * 파일이 없거나 내용이 비어있으면 빈 리스트를 반환한다.
     */
    public static <T> List<T> readList(File file, Class<T> clazz) {
        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(
                    file,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            throw new RuntimeException("JSON 파일 읽기 실패: " + file.getPath(), e);
        }
    }

    /**
     * List<T>를 JSON 파일에 prettyPrint 형식으로 저장한다.
     * 부모 디렉터리가 없으면 자동으로 생성한다.
     */
    public static <T> void writeList(File file, List<T> list) {
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        try {
            MAPPER.writeValue(file, list);
        } catch (IOException e) {
            throw new RuntimeException("JSON 파일 쓰기 실패: " + file.getPath(), e);
        }
    }
}
```

---

## 제약 조건

| 항목 | 규칙 |
|------|------|
| 변경 금지 파일 | `settings.gradle`, `gradlew`, `gradlew.bat`, `.gitignore` |
| JUnit bom 버전 | `junit-bom:6.0.0` 변경 금지 |
| 모델 클래스 | setter, 기본 생성자(no-arg constructor) 추가 금지 |
| `final` 필드 | 모든 모델 클래스 필드는 `private final` |
| Jackson | 모든 모델 생성자에 `@JsonCreator` + `@JsonProperty` 적용 필수 |
| `System.out` | 이 Phase에서는 사용 없음 |
| 새 외부 라이브러리 | Jackson Databind 2.18.3, JavaFaker 1.0.2 외 추가 금지 |

---

## 완료 기준

아래 조건을 모두 만족하면 Phase 1이 완료된 것으로 간주한다.

1. `./gradlew compileJava` 에러 없이 완료된다.
2. 9개 파일이 모두 존재한다.
3. 아래 Jackson 직렬화 동작이 보장된다:
   - `Sample`, `Order`, `Inventory`, `ProductionItem` 인스턴스를 `ObjectMapper.writeValueAsString()`으로 직렬화할 수 있다.
   - 직렬화된 JSON 문자열을 `ObjectMapper.readValue()`로 역직렬화하여 동일 필드값을 가진 인스턴스를 얻을 수 있다.
4. `./gradlew build` 에러 없이 완료된다 (테스트 클래스 없으므로 테스트 단계는 SKIP 허용).

---

## 디렉터리 구조 (Phase 1 완료 후 예상)

```
src/
└── main/
    └── java/
        └── org/
            └── example/
                ├── model/
                │   ├── Sample.java
                │   ├── Order.java
                │   ├── OrderStatus.java
                │   ├── Inventory.java
                │   ├── InventoryStatus.java
                │   └── ProductionItem.java
                ├── repository/
                │   └── CrudRepository.java
                └── util/
                    └── JsonFileUtil.java
```
