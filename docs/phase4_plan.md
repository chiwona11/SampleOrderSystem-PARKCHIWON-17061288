# Phase 4 상세 설계 — 프레젠테이션 레이어

> 이 문서는 PLAN.md Phase 4의 상세 버전이다. 완료 기준 및 구현 명세는 이 문서를 정본으로 삼으며, PLAN.md와 내용이 다른 경우 이 문서를 우선한다.

---

## Phase 목표

`ConsoleView`(단일 I/O 담당)와 5개 Controller(`SampleController`, `OrderController`, `ProductionController`, `MonitorController`, `DummyController`)를 구현하고, `Main`에서 DI를 조립하여 전체 애플리케이션이 `./gradlew run`으로 실행 가능한 상태가 되도록 한다. 이 Phase에서 Phase 3까지 구현된 모든 Service가 콘솔 메뉴를 통해 동작하며, DummyController는 Phase 5 스텁으로서 안내 메시지만 출력한다.

---

## FR-N / NFR-N 매핑

| 요구사항 | 설명 |
|---------|------|
| FR-1 전체 | `SampleController`가 `SampleService`를 호출하여 시료 등록/목록/검색 기능 실행 |
| FR-2 전체 | `OrderController`가 `OrderService`를 호출하여 주문 접수/승인/거절/출고 기능 실행 |
| FR-3 전체 | `MonitorController`가 `MonitorService`를 호출하여 상태별 주문 건수 및 재고 현황 표시 |
| FR-4-1 | `OrderController.handle("4")`가 `OrderService.release()`를 호출하여 출고 처리 |
| FR-5 전체 | `ProductionController`가 `ProductionService`를 호출하여 생산 중 목록/대기 큐/완료 처리 |
| FR-6 (스텁) | `DummyController`가 Phase 5 안내 메시지를 출력 (실제 생성 로직은 Phase 5에서 구현) |
| NFR-3 | `System.out` 호출은 `ConsoleView`에서만 허용, MVC 역할 엄격 분리 |
| NFR-4 | Controller는 조정 역할만 수행, 비즈니스 로직 없음 |

---

## 변경 대상 파일 목록

| 파일 경로 | 작업 |
|-----------|------|
| `src/main/java/org/example/view/ConsoleView.java` | 생성 |
| `src/main/java/org/example/controller/SampleController.java` | 생성 |
| `src/main/java/org/example/controller/OrderController.java` | 생성 |
| `src/main/java/org/example/controller/ProductionController.java` | 생성 |
| `src/main/java/org/example/controller/MonitorController.java` | 생성 |
| `src/main/java/org/example/controller/DummyController.java` | 생성 (Phase 5에서 완성) |
| `src/main/java/org/example/Main.java` | 생성 |

---

## 구현 상세

### `src/main/java/org/example/view/ConsoleView.java`

모든 `System.out.println` / `System.out.print` 호출은 이 클래스에서만 허용된다.

```java
package org.example.view;

import org.example.model.Inventory;
import org.example.model.InventoryStatus;
import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.ProductionItem;
import org.example.model.Sample;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ConsoleView {

    private final Scanner scanner;

    public ConsoleView(Scanner scanner) {
        this.scanner = scanner;
    }

    public void showMainMenu() {
        System.out.println();
        System.out.println("===== 반도체 시료 생산주문관리 시스템 =====");
        System.out.println("1. 시료 관리");
        System.out.println("   1-1. 새로운 시료 등록");
        System.out.println("   1-2. 시료 목록 조회");
        System.out.println("   1-3. 시료 검색");
        System.out.println("2. 주문 처리");
        System.out.println("   2-1. 고객 주문 접수");
        System.out.println("   2-2. 주문 승인");
        System.out.println("   2-3. 주문 거절");
        System.out.println("3. 모니터링");
        System.out.println("   3-1. 상태별 주문 수 확인");
        System.out.println("   3-2. 시료별 재고 현황 확인");
        System.out.println("4. 출고 처리");
        System.out.println("   4-1. CONFIRMED 주문 출고");
        System.out.println("5. 생산 라인");
        System.out.println("   5-1. 생산 중인 시료 확인");
        System.out.println("   5-2. 생산 대기 큐 확인");
        System.out.println("   5-3. 생산 완료 처리");
        System.out.println("6. 더미 데이터 생성");
        System.out.println("   6-1. 시료 더미 데이터 생성");
        System.out.println("   6-2. 주문 더미 데이터 생성");
        System.out.println("0. 종료");
        System.out.println("==========================================");
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public int readInt(String prompt) {
        while (true) {
            String input = readLine(prompt);
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] 숫자를 입력해주세요.");
            }
        }
    }

    public long readLong(String prompt) {
        while (true) {
            String input = readLine(prompt);
            try {
                return Long.parseLong(input);
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] 숫자를 입력해주세요.");
            }
        }
    }

    public double readDouble(String prompt) {
        while (true) {
            String input = readLine(prompt);
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] 숫자를 입력해주세요.");
            }
        }
    }

    public void showSuccess(String message) {
        System.out.println("[SUCCESS] " + message);
    }

    public void showError(String message) {
        System.out.println("[ERROR] " + message);
    }

    public void showInfo(String message) {
        System.out.println(message);
    }

    public void showSampleTable(List<Sample> samples) {
        if (samples.isEmpty()) {
            System.out.println("등록된 시료가 없습니다.");
            return;
        }
        System.out.println("+------------+----------------------+--------------------+--------+");
        System.out.println("| ID         | 이름                 | 평균생산시간(분)   | 수율   |");
        System.out.println("+------------+----------------------+--------------------+--------+");
        for (Sample s : samples) {
            System.out.printf("| %-10s | %-20s | %-18d | %-6.2f |%n",
                    s.getId(), s.getName(), s.getAvgProductionTime(), s.getYield());
        }
        System.out.println("+------------+----------------------+--------------------+--------+");
    }

    public void showOrderTable(List<Order> orders) {
        if (orders.isEmpty()) {
            System.out.println("주문이 없습니다.");
            return;
        }
        System.out.println("+--------------------------------------+------------+--------------------+------+----------+---------------------+");
        System.out.println("| 주문ID                               | 시료ID     | 고객명             | 수량 | 상태     | 접수일시            |");
        System.out.println("+--------------------------------------+------------+--------------------+------+----------+---------------------+");
        for (Order o : orders) {
            System.out.printf("| %-36s | %-10s | %-18s | %-4d | %-8s | %-19s |%n",
                    o.getId(), o.getSampleId(), o.getCustomerName(),
                    o.getQuantity(), o.getStatus(), o.getCreatedAt());
        }
        System.out.println("+--------------------------------------+------------+--------------------+------+----------+---------------------+");
    }

    public void showInventoryStatusTable(Map<String, InventoryStatus> statusMap, List<Inventory> inventories) {
        if (inventories.isEmpty()) {
            System.out.println("재고 데이터가 없습니다.");
            return;
        }
        System.out.println("+------------+------+----------+");
        System.out.println("| 시료ID     | 재고 | 상태     |");
        System.out.println("+------------+------+----------+");
        for (Inventory inv : inventories) {
            InventoryStatus status = statusMap.getOrDefault(inv.getSampleId(), InventoryStatus.DEPLETED);
            System.out.printf("| %-10s | %-4d | %-8s |%n",
                    inv.getSampleId(), inv.getStock(), status);
        }
        System.out.println("+------------+------+----------+");
    }

    public void showOrderCountTable(Map<OrderStatus, Long> countMap) {
        if (countMap.isEmpty()) {
            System.out.println("집계할 주문이 없습니다.");
            return;
        }
        System.out.println("+----------+------+");
        System.out.println("| 상태     | 건수 |");
        System.out.println("+----------+------+");
        for (Map.Entry<OrderStatus, Long> entry : countMap.entrySet()) {
            System.out.printf("| %-8s | %-4d |%n", entry.getKey(), entry.getValue());
        }
        System.out.println("+----------+------+");
    }

    public void showProductionQueueTable(List<ProductionItem> queue) {
        if (queue.isEmpty()) {
            System.out.println("생산 대기 큐가 비어 있습니다.");
            return;
        }
        System.out.println("+--------------------------------------+------------+----------+----------+------------------+---------------------+");
        System.out.println("| 주문ID                               | 시료ID     | 필요수량 | 실생산량 | 총생산시간(분)   | 큐등록일시          |");
        System.out.println("+--------------------------------------+------------+----------+----------+------------------+---------------------+");
        for (ProductionItem item : queue) {
            System.out.printf("| %-36s | %-10s | %-8d | %-8d | %-16d | %-19s |%n",
                    item.getOrderId(), item.getSampleId(),
                    item.getRequiredQuantity(), item.getActualProduction(),
                    item.getTotalProductionTime(), item.getEnqueuedAt());
        }
        System.out.println("+--------------------------------------+------------+----------+----------+------------------+---------------------+");
    }
}
```

---

### `src/main/java/org/example/controller/SampleController.java`

```java
package org.example.controller;

import org.example.model.Sample;
import org.example.service.SampleService;
import org.example.view.ConsoleView;

import java.util.List;

public class SampleController {

    private final SampleService sampleService;
    private final ConsoleView view;

    public SampleController(SampleService sampleService, ConsoleView view) {
        this.sampleService = sampleService;
        this.view = view;
    }

    public void handle(String subMenu) {
        try {
            switch (subMenu) {
                case "1" -> register();
                case "2" -> listAll();
                case "3" -> search();
                default  -> view.showError("잘못된 메뉴 입력입니다.");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            view.showError(e.getMessage());
        }
    }

    private void register() {
        String id = view.readLine("시료 ID: ");
        String name = view.readLine("시료 이름: ");
        long avgProductionTime = view.readLong("평균 생산시간 (분): ");
        double yield = view.readDouble("수율 (0.0 ~ 1.0): ");
        Sample sample = sampleService.register(id, name, avgProductionTime, yield);
        view.showSuccess("시료가 등록되었습니다. ID=" + sample.getId());
    }

    private void listAll() {
        List<Sample> samples = sampleService.findAll();
        view.showSampleTable(samples);
    }

    private void search() {
        String keyword = view.readLine("검색어: ");
        List<Sample> samples = sampleService.search(keyword);
        view.showSampleTable(samples);
    }
}
```

---

### `src/main/java/org/example/controller/OrderController.java`

```java
package org.example.controller;

import org.example.model.Order;
import org.example.service.OrderService;
import org.example.view.ConsoleView;

public class OrderController {

    private final OrderService orderService;
    private final ConsoleView view;

    public OrderController(OrderService orderService, ConsoleView view) {
        this.orderService = orderService;
        this.view = view;
    }

    public void handle(String subMenu) {
        try {
            switch (subMenu) {
                case "1" -> placeOrder();
                case "2" -> approve();
                case "3" -> reject();
                case "4" -> release();
                default  -> view.showError("잘못된 메뉴 입력입니다.");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            view.showError(e.getMessage());
        }
    }

    private void placeOrder() {
        String sampleId = view.readLine("시료 ID: ");
        String customerName = view.readLine("고객명: ");
        int quantity = view.readInt("주문 수량: ");
        Order order = orderService.placeOrder(sampleId, customerName, quantity);
        view.showSuccess("주문이 접수되었습니다. 주문ID=" + order.getId());
    }

    private void approve() {
        String orderId = view.readLine("주문 ID: ");
        Order order = orderService.approve(orderId);
        view.showSuccess("주문이 승인되었습니다. 상태=" + order.getStatus());
    }

    private void reject() {
        String orderId = view.readLine("주문 ID: ");
        Order order = orderService.reject(orderId);
        view.showSuccess("주문이 거절되었습니다. 주문ID=" + order.getId());
    }

    private void release() {
        String orderId = view.readLine("출고할 주문 ID: ");
        Order order = orderService.release(orderId);
        view.showSuccess("출고 처리가 완료되었습니다. 주문ID=" + order.getId());
    }
}
```

---

### `src/main/java/org/example/controller/ProductionController.java`

```java
package org.example.controller;

import org.example.model.Order;
import org.example.model.ProductionItem;
import org.example.service.ProductionService;
import org.example.view.ConsoleView;

import java.util.List;

public class ProductionController {

    private final ProductionService productionService;
    private final ConsoleView view;

    public ProductionController(ProductionService productionService, ConsoleView view) {
        this.productionService = productionService;
        this.view = view;
    }

    public void handle(String subMenu) {
        try {
            switch (subMenu) {
                case "1" -> listActiveProductions();
                case "2" -> listQueueStatus();
                case "3" -> completeProduction();
                default  -> view.showError("잘못된 메뉴 입력입니다.");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            view.showError(e.getMessage());
        }
    }

    private void listActiveProductions() {
        List<Order> orders = productionService.getActiveProductions();
        view.showOrderTable(orders);
    }

    private void listQueueStatus() {
        List<ProductionItem> queue = productionService.getQueueStatus();
        view.showProductionQueueTable(queue);
    }

    private void completeProduction() {
        String orderId = view.readLine("완료 처리할 주문 ID: ");
        Order order = productionService.completeProduction(orderId);
        view.showSuccess("생산이 완료되었습니다. 주문ID=" + order.getId() + ", 상태=" + order.getStatus());
    }
}
```

---

### `src/main/java/org/example/controller/MonitorController.java`

```java
package org.example.controller;

import org.example.model.Inventory;
import org.example.model.InventoryStatus;
import org.example.model.OrderStatus;
import org.example.repository.InventoryRepository;
import org.example.service.MonitorService;
import org.example.view.ConsoleView;

import java.util.List;
import java.util.Map;

public class MonitorController {

    private final MonitorService monitorService;
    private final InventoryRepository inventoryRepo;
    private final ConsoleView view;

    public MonitorController(MonitorService monitorService, InventoryRepository inventoryRepo, ConsoleView view) {
        this.monitorService = monitorService;
        this.inventoryRepo = inventoryRepo;
        this.view = view;
    }

    public void handle(String subMenu) {
        try {
            switch (subMenu) {
                case "1" -> showOrderCountByStatus();
                case "2" -> showInventoryStatus();
                default  -> view.showError("잘못된 메뉴 입력입니다.");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            view.showError(e.getMessage());
        }
    }

    private void showOrderCountByStatus() {
        Map<OrderStatus, Long> countMap = monitorService.getOrderCountByStatus();
        view.showOrderCountTable(countMap);
    }

    private void showInventoryStatus() {
        Map<String, InventoryStatus> statusMap = monitorService.getInventoryStatus();
        List<Inventory> inventories = inventoryRepo.findAll();
        view.showInventoryStatusTable(statusMap, inventories);
    }
}
```

---

### `src/main/java/org/example/controller/DummyController.java` (Phase 4 스텁)

Phase 5에서 `SampleGenerator` / `OrderGenerator`를 주입받아 완성된다. Phase 4에서는 안내 메시지만 출력한다.

```java
package org.example.controller;

import org.example.view.ConsoleView;

public class DummyController {

    private final ConsoleView view;

    public DummyController(ConsoleView view) {
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
        view.showInfo("더미 시료 데이터 생성 기능은 Phase 5에서 구현됩니다.");
    }

    public void generateOrders() {
        view.showInfo("더미 주문 데이터 생성 기능은 Phase 5에서 구현됩니다.");
    }
}
```

---

### `src/main/java/org/example/Main.java`

DI 조립 전담. `new` 키워드로 객체를 생성하는 곳은 이 클래스뿐이다.

```java
package org.example;

import org.example.controller.DummyController;
import org.example.controller.MonitorController;
import org.example.controller.OrderController;
import org.example.controller.ProductionController;
import org.example.controller.SampleController;
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
        SampleController     sampleCtrl     = new SampleController(sampleService, view);
        OrderController      orderCtrl      = new OrderController(orderService, view);
        ProductionController productionCtrl = new ProductionController(productionService, view);
        MonitorController    monitorCtrl    = new MonitorController(monitorService, inventoryRepo, view);
        DummyController      dummyCtrl      = new DummyController(view);

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
| `System.out` | `ConsoleView` 클래스 외부에서 호출 금지 |
| Controller 비즈니스 로직 | Controller 내부에서 직접 데이터 계산/판단 금지. Service 호출 + View 출력만 허용 |
| 예외 처리 | `IllegalArgumentException` / `IllegalStateException` → `view.showError(e.getMessage())` |
| DI 조립 | `new` 키워드로 객체 생성은 `Main.java`에서만 허용 |
| 메인 루프 | 메뉴 "4" (출고 처리)는 서브메뉴 없이 바로 `orderCtrl.handle("4")` 호출 |
| `readInt` / `readLong` / `readDouble` | 파싱 실패 시 `[ERROR]` 메시지 출력 후 재입력 루프 |
| Phase 4 단위 테스트 | 없음 — 콘솔 I/O 레이어이므로 `./gradlew run` 수동 실행으로 검증 |

---

## 완료 기준

1. `./gradlew run` 실행 시 메인 메뉴가 출력되고 입력 대기 상태가 된다.
2. 메뉴 1-1 시료 등록 실행 후 `data/samples.json`이 생성되고 등록한 시료 데이터가 저장된다.
3. 메뉴 2-1 주문 접수 후 메뉴 2-2 승인(재고 부족 시나리오) 실행 시 주문 상태가 `PRODUCING`으로 변경된다.
4. 메뉴 4 (출고 처리) 실행 시 `CONFIRMED` 상태 주문이 `RELEASE`로 전환된다.
5. 메뉴 6-1, 6-2 실행 시 "Phase 5에서 구현됩니다" 안내 메시지가 출력된다.
6. `./gradlew build` 에러 없이 완료된다.
7. 콘솔에서 잘못된 메뉴 입력 시 `[ERROR]` 메시지가 출력되고 시스템이 종료되지 않는다.
