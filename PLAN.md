# PLAN.md — 반도체 시료 생산주문관리 시스템

**문서 버전**: 1.2  
**작성일**: 2026-06-12  
**프로젝트명**: SampleOrderSystem-PARKCHIWON-17061288

---

## 전체 개발 계획 요약

| Phase | 이름 | 핵심 산출물 | 상세 설계 | 상태 |
|-------|------|-----------|---------|------|
| 1 | 기반 구조 | 모델 6종, CrudRepository, JsonFileUtil | [docs/phase1_plan.md](docs/phase1_plan.md) | ✅ 완료 |
| 2 | 저장소 레이어 | SampleRepo, OrderRepo, InventoryRepo + 테스트 17개 | [docs/phase2_plan.md](docs/phase2_plan.md) | ✅ 완료 |
| 3 | 서비스 레이어 | SampleService, OrderService, ProductionService, MonitorService + 테스트 23개 | [docs/phase3_plan.md](docs/phase3_plan.md) | ✅ 완료 |
| 4 | 프레젠테이션 | Controller 5종, ConsoleView, Main | [docs/phase4_plan.md](docs/phase4_plan.md) | ✅ 완료 |
| 5 | 더미 데이터 | SampleGenerator, OrderGenerator, DummyController | docs/phase5_plan.md | ⬜ 미착수 |

### 레이어 의존성 흐름

```
Main → Controller → Service → Repository → data/*.json
                 ↘ ConsoleView
```

### 의존성 주입 구조 (Main 기준)

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

## Phase 1: 기반 구조 ✅

> **상태**: 완료  
> **상세 설계 정본**: [`docs/phase1_plan.md`](docs/phase1_plan.md)  
> 구현 명세 및 완전한 코드는 위 문서를 참조한다.

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

### 완료 기준

- `./gradlew compileJava` 에러 없이 완료
- 9개 파일(모델 6개 + CrudRepository + JsonFileUtil + build.gradle 수정)이 모두 존재
- `Sample`, `Order`, `Inventory`, `ProductionItem` 인스턴스를 Jackson으로 직렬화/역직렬화했을 때 동일 필드값이 복원된다
- `./gradlew build` 에러 없이 완료

---

## Phase 2: 저장소 레이어 ✅

> **상태**: 완료  
> **상세 설계 정본**: [`docs/phase2_plan.md`](docs/phase2_plan.md)  
> 구현 명세 및 완전한 코드는 위 문서를 참조한다.

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

### 완료 기준

- `./gradlew test --tests "org.example.repository.*"` GREEN (17개 테스트 모두 통과 — SampleRepositoryTest 8, OrderRepositoryTest 5, InventoryRepositoryTest 4)
- 3개 저장소 모두 `CrudRepository<T, ID>` 인터페이스 완전 구현
- 재실행 후 `findAll()` 호출로 데이터 복원 확인 가능
- `SampleRepository.existsById()`, `OrderRepository.findByStatus()`, `OrderRepository.findBySampleId()`, `InventoryRepository.findOrCreate()` 추가 메서드 동작 검증

---

## Phase 3: 서비스 레이어 ✅

> **상태**: 완료  
> **상세 설계 정본**: [`docs/phase3_plan.md`](docs/phase3_plan.md)  
> 구현 명세 및 완전한 코드는 위 문서를 참조한다.

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

> 상세 구현 코드는 [`docs/phase3_plan.md`](docs/phase3_plan.md)를 참조한다.

### 완료 기준

- `./gradlew test --tests "org.example.service.*"` GREEN (총 23개 — SampleServiceTest 6, OrderServiceTest 9, ProductionServiceTest 4, MonitorServiceTest 4)
- `shortage=10, yield=0.9` → `actualProduction=13` 검증 통과
- REJECTED 주문이 `getOrderCountByStatus()` 결과에 미포함 검증 통과
- `stock==0` 시료에 pendingDemand 존재해도 DEPLETED 반환 검증 통과

---

## Phase 4: 프레젠테이션 레이어 ✅

> **상태**: 완료  
> **상세 설계 정본**: [`docs/phase4_plan.md`](docs/phase4_plan.md)  
> 구현 명세 및 완전한 코드는 위 문서를 참조한다.

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

> 상세 구현 코드는 [`docs/phase4_plan.md`](docs/phase4_plan.md)를 참조한다.

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
public class SampleGenerator {
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
public class OrderGenerator {
    // 시료 없으면 IllegalStateException("시료 데이터가 없습니다. 먼저 시료 더미 데이터를 생성하세요.")
    // sampleId: 기존 시료 목록에서 랜덤 선택
    // customerName: faker.name().fullName()
    // quantity: 1 ~ 100 / status: RESERVED
    public void generate(int count) { ... }
}
```

#### `controller/DummyController.java` (완성)

```java
public class DummyController {
    private final SampleGenerator sampleGenerator;
    private final OrderGenerator  orderGenerator;
    private final ConsoleView view;

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
