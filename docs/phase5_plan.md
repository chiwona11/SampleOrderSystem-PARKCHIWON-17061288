# Phase 5 상세 설계 — 더미 데이터 도구

> 이 문서는 PLAN.md Phase 5의 상세 버전이다. 완료 기준 및 구현 명세는 이 문서를 정본으로 삼으며, PLAN.md와 내용이 다른 경우 이 문서를 우선한다.

---

## Phase 목표

JavaFaker(한국 로케일)를 사용하여 현실적인 시료·주문 더미 데이터를 생성하는 `SampleGenerator`와 `OrderGenerator` 클래스를 `org.example.dummy` 패키지에 구현하고, Phase 4에서 스텁으로 남겨진 `DummyController`를 완성하며, `Main.java`의 DI 조립 코드를 업데이트하여 메뉴 6-1(시료 더미 데이터 생성)과 6-2(주문 더미 데이터 생성)가 실제로 동작하도록 한다.

---

## FR-N / NFR-N 매핑

| 요구사항 | 설명 |
|---------|------|
| FR-6-1 | `SampleGenerator` — JavaFaker(한국 로케일)로 시료 데이터 생성 후 `data/samples.json`에 저장 |
| FR-6-2 | `OrderGenerator` — 등록된 시료 ID를 참조하여 주문 데이터 생성 후 `data/orders.json`에 저장 |
| NFR-1 | 생성된 데이터는 기존 데이터에 추가 (덮어쓰기 금지) — Repository의 read-modify-write 패턴 준수 |
| NFR-3 | `System.out` 호출 금지 — 모든 출력은 `ConsoleView` 경유 |
| NFR-4 | Generator 클래스는 데이터 생성·저장만 담당 (SRP), Controller는 조정 역할만 수행 |

---

## 변경 대상 파일 목록

| 파일 경로 | 작업 |
|-----------|------|
| `src/main/java/org/example/dummy/SampleGenerator.java` | 생성 |
| `src/main/java/org/example/dummy/OrderGenerator.java` | 생성 |
| `src/main/java/org/example/controller/DummyController.java` | 수정 (완성) |
| `src/main/java/org/example/Main.java` | 수정 (DummyController DI 업데이트) |

---

## 구현 상세

### `src/main/java/org/example/dummy/SampleGenerator.java`

```java
package org.example.dummy;

import com.github.javafaker.Faker;
import org.example.model.Inventory;
import org.example.model.Sample;
import org.example.repository.InventoryRepository;
import org.example.repository.SampleRepository;

import java.util.List;
import java.util.Locale;

public class SampleGenerator {

    private static final List<String> SAMPLE_NAMES = List.of(
            "실리콘 웨이퍼 12인치",
            "실리콘 웨이퍼 8인치",
            "NPW 12인치",
            "NPW 8인치",
            "테스트 웨이퍼 12인치",
            "에피택셜 웨이퍼 12인치",
            "SOI 웨이퍼 12인치",
            "폴리실리콘 웨이퍼 12인치",
            "더미 웨이퍼 12인치",
            "모니터 웨이퍼 8인치"
    );

    private final SampleRepository sampleRepo;
    private final InventoryRepository inventoryRepo;

    public SampleGenerator(SampleRepository sampleRepo, InventoryRepository inventoryRepo) {
        this.sampleRepo = sampleRepo;
        this.inventoryRepo = inventoryRepo;
    }

    public void generate(int count) {
        Faker faker = new Faker(new Locale("ko"));
        for (int i = 0; i < count; i++) {
            String id = "S-" + System.currentTimeMillis() + "-" + i;
            if (sampleRepo.existsById(id)) {
                continue;
            }
            String name = SAMPLE_NAMES.get(faker.number().numberBetween(0, SAMPLE_NAMES.size()));
            long avgProductionTime = 10L + (long)(faker.number().numberBetween(0, 48)) * 10;
            double yield = Math.round((0.70 + faker.number().randomDouble(2, 0, 29) / 100.0) * 100.0) / 100.0;
            Sample sample = new Sample(id, name, avgProductionTime, yield);
            sampleRepo.save(sample);
            inventoryRepo.save(new Inventory(id, 0));
        }
    }
}
```

**설계 근거**

| 항목 | 상세 |
|------|------|
| `SAMPLE_NAMES` | 반도체 WF 관련 시료 이름 10종 고정 목록 — JavaFaker 상품명 대신 도메인 특화 이름 사용 |
| `name` 생성 | `SAMPLE_NAMES.get(faker.number().numberBetween(0, SAMPLE_NAMES.size()))` — 목록에서 랜덤 선택 |
| `id = "S-" + System.currentTimeMillis() + "-" + i` | 밀리초 타임스탬프 + 루프 인덱스 조합으로 중복 방지 |
| `avgProductionTime` 범위 | `10 + (0~47) * 10` → 10~480분 (10분 단위) |
| `yield` 범위 | `0.70 + (0~0.29)` → 0.70~0.99, `Math.round(... * 100.0) / 100.0`으로 소수 2자리 확정 |
| 중복 ID 체크 | `sampleRepo.existsById(id)` true이면 해당 항목 skip |
| 재고 초기화 | `new Inventory(id, 0)` — 시료 등록 즉시 재고 0으로 초기화 |

---

### `src/main/java/org/example/dummy/OrderGenerator.java`

```java
package org.example.dummy;

import com.github.javafaker.Faker;
import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.Sample;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class OrderGenerator {

    private final OrderRepository orderRepo;
    private final SampleRepository sampleRepo;

    public OrderGenerator(OrderRepository orderRepo, SampleRepository sampleRepo) {
        this.orderRepo = orderRepo;
        this.sampleRepo = sampleRepo;
    }

    public void generate(int count) {
        List<Sample> samples = sampleRepo.findAll();
        if (samples.isEmpty()) {
            throw new IllegalStateException("시료 데이터가 없습니다. 먼저 시료 더미 데이터를 생성하세요.");
        }
        Faker faker = new Faker(new Locale("ko"));
        String createdAt = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        for (int i = 0; i < count; i++) {
            String id = UUID.randomUUID().toString();
            String sampleId = samples.get(faker.number().numberBetween(0, samples.size())).getId();
            String customerName = faker.name().fullName();
            int quantity = faker.number().numberBetween(1, 101);
            Order order = new Order(id, sampleId, customerName, quantity, OrderStatus.RESERVED, createdAt);
            orderRepo.save(order);
        }
    }
}
```

**설계 근거**

| 항목 | 상세 |
|------|------|
| 시료 없음 예외 | `samples.isEmpty()` → `IllegalStateException` 발생 (시스템 종료 없음, DummyController에서 catch) |
| `id` | `UUID.randomUUID().toString()` — Order ID는 UUID 자동 생성 |
| `sampleId` | `sampleRepo.findAll()` 결과에서 랜덤 인덱스로 선택 |
| `customerName` | `faker.name().fullName()` — 한국 로케일 전체 이름 |
| `quantity` 범위 | `numberBetween(1, 101)` → 1~100 (JavaFaker 상한 exclusive) |
| `status` | `OrderStatus.RESERVED` 고정 |
| `createdAt` | `yyyy-MM-dd'T'HH:mm:ss` 형식 문자열 (Order 모델 규격 준수) |

---

### `src/main/java/org/example/controller/DummyController.java` (수정 완성)

Phase 4 스텁에서 생성자 파라미터를 교체하고 실제 Generator 호출로 완성한다.

```java
package org.example.controller;

import org.example.dummy.OrderGenerator;
import org.example.dummy.SampleGenerator;
import org.example.view.ConsoleView;

public class DummyController {

    private final SampleGenerator sampleGenerator;
    private final OrderGenerator orderGenerator;
    private final ConsoleView view;

    public DummyController(SampleGenerator sampleGenerator, OrderGenerator orderGenerator, ConsoleView view) {
        this.sampleGenerator = sampleGenerator;
        this.orderGenerator = orderGenerator;
        this.view = view;
    }

    public void handle(String subMenu) {
        switch (subMenu) {
            case "1" -> generateSamples();
            case "2" -> generateOrders();
            default  -> view.showError("잘못된 메뉴 입력입니다.");
        }
    }

    public void generateSamples() {
        int count = view.readInt("생성할 시료 수: ");
        sampleGenerator.generate(count);
        view.showSuccess(count + "개의 더미 시료 데이터가 생성되었습니다.");
    }

    public void generateOrders() {
        int count = view.readInt("생성할 주문 수: ");
        try {
            orderGenerator.generate(count);
            view.showSuccess(count + "개의 더미 주문 데이터가 생성되었습니다.");
        } catch (IllegalStateException e) {
            view.showError(e.getMessage());
        }
    }
}
```

**변경 요약**

| 항목 | Phase 4 스텁 | Phase 5 완성 |
|------|-------------|-------------|
| 생성자 파라미터 | `(ConsoleView view)` | `(SampleGenerator, OrderGenerator, ConsoleView)` |
| `generateSamples()` | 안내 메시지 출력 | `view.readInt` → `sampleGenerator.generate(count)` → `view.showSuccess` |
| `generateOrders()` | 안내 메시지 출력 | `view.readInt` → `orderGenerator.generate(count)` with try-catch |

---

### `src/main/java/org/example/Main.java` (수정)

`SampleGenerator`, `OrderGenerator` import를 추가하고 `DummyController` 생성 라인을 교체한다. 나머지 코드는 변경하지 않는다.

```java
package org.example;

import org.example.controller.DummyController;
import org.example.controller.MonitorController;
import org.example.controller.OrderController;
import org.example.controller.ProductionController;
import org.example.controller.SampleController;
import org.example.dummy.OrderGenerator;
import org.example.dummy.SampleGenerator;
import org.example.model.ProductionItem;
import org.example.repository.InventoryRepository;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;
import org.example.service.MonitorService;
import org.example.service.OrderService;
import org.example.service.ProductionService;
import org.example.service.SampleService;
import org.example.view.ConsoleView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        // data/ 디렉터리 자동 생성
        new File("data").mkdirs();

        File samplesFile   = new File("data/samples.json");
        File ordersFile    = new File("data/orders.json");
        File inventoryFile = new File("data/inventory.json");

        // Repository 초기화
        SampleRepository    sampleRepo    = new SampleRepository(samplesFile);
        OrderRepository     orderRepo     = new OrderRepository(ordersFile);
        InventoryRepository inventoryRepo = new InventoryRepository(inventoryFile);

        // 공유 생산 큐 (OrderService ↔ ProductionService 공유)
        List<ProductionItem> productionQueue = new ArrayList<>();

        // Service 초기화
        SampleService     sampleService     = new SampleService(sampleRepo, inventoryRepo);
        OrderService      orderService      = new OrderService(orderRepo, sampleRepo, inventoryRepo, productionQueue);
        ProductionService productionService = new ProductionService(productionQueue, orderRepo, inventoryRepo);
        MonitorService    monitorService    = new MonitorService(orderRepo, inventoryRepo, sampleRepo);

        // View 초기화
        ConsoleView view = new ConsoleView(new Scanner(System.in));

        // Controller 초기화
        SampleController     sampleCtrl      = new SampleController(sampleService, view);
        OrderController      orderCtrl       = new OrderController(orderService, view);
        ProductionController productionCtrl  = new ProductionController(productionService, view);
        MonitorController    monitorCtrl     = new MonitorController(monitorService, inventoryRepo, view);
        SampleGenerator      sampleGenerator = new SampleGenerator(sampleRepo, inventoryRepo);
        OrderGenerator       orderGenerator  = new OrderGenerator(orderRepo, sampleRepo);
        DummyController      dummyCtrl       = new DummyController(sampleGenerator, orderGenerator, view);

        // 메인 루프
        while (true) {
            view.showMainMenu();
            String mainMenu = view.readLine("메뉴 선택: ");

            if ("0".equals(mainMenu)) {
                view.showInfo("시스템을 종료합니다.");
                break;
            }

            // 출고 처리는 서브메뉴 없이 바로 orderId 입력
            if ("4".equals(mainMenu)) {
                try {
                    orderCtrl.handle("4");
                } catch (IllegalArgumentException | IllegalStateException e) {
                    view.showError(e.getMessage());
                }
                continue;
            }

            String subMenu = view.readLine("세부 메뉴 선택: ");
            try {
                switch (mainMenu) {
                    case "1" -> sampleCtrl.handle(subMenu);
                    case "2" -> orderCtrl.handle(subMenu);
                    case "3" -> monitorCtrl.handle(subMenu);
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

---

## 제약 조건

| 항목 | 규칙 |
|------|------|
| JavaFaker 의존성 | `build.gradle`에 `implementation 'com.github.javafaker:javafaker:1.0.2'` 이미 추가됨 — 추가 수정 불필요 |
| 시료 없이 주문 생성 | `IllegalStateException` throw → `DummyController.generateOrders()`에서 catch → `view.showError()` 출력 후 메뉴로 복귀 (시스템 종료 없음) |
| 데이터 추가 방식 | `Repository.save()`는 기존 read-modify-write 패턴으로 기존 데이터에 추가 (덮어쓰기 금지) |
| `System.out` | `SampleGenerator`, `OrderGenerator`, `DummyController` 어디에도 직접 호출 금지 — `ConsoleView` 경유 |
| DI 조립 위치 | `new` 키워드로 객체 생성은 `Main.java`에서만 허용 |
| 단위 테스트 | 더미 데이터 생성 도구이므로 단위 테스트 없음 |

---

## 완료 기준

1. `./gradlew run` 실행 후 메뉴 6-1 선택 → 생성 수 입력 → `data/samples.json`에 faker 생성 시료 데이터가 추가된다.
2. 메뉴 6-2 선택 → 생성 수 입력 → `data/orders.json`에 faker 생성 주문 데이터(status=RESERVED)가 추가된다.
3. `data/samples.json`이 비어 있는 상태에서 메뉴 6-2 실행 시 `[ERROR] 시료 데이터가 없습니다. 먼저 시료 더미 데이터를 생성하세요.` 메시지가 출력되고 메뉴로 복귀한다 (시스템 종료 없음).
4. 생성된 시료의 `avgProductionTime`은 10~480 범위 내 10의 배수이다.
5. 생성된 시료의 `yield`는 0.70~0.99 범위의 소수 2자리 값이다.
6. 생성된 주문의 `quantity`는 1~100 범위이다.
7. `./gradlew build` 에러 없이 완료된다.
8. `./gradlew test` 전체 GREEN (Phase 1~3 기존 40개 테스트 모두 통과, Phase 5 신규 테스트 없음).
