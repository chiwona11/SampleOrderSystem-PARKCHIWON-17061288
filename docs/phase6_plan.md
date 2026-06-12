# Phase 6 상세 설계 — 콘솔 UI 개선

> 이 문서는 PLAN.md Phase 6의 상세 버전이다. 완료 기준 및 구현 명세는 이 문서를 정본으로 삼으며, PLAN.md와 내용이 다른 경우 이 문서를 우선한다.

---

## 1. Phase 목표

콘솔 UI를 전면 개선하여 가독성과 운영 편의성을 높인다. 구체적으로:

- 메인 메뉴에 ASCII 아트 배너 + 시스템 현황 요약을 추가한다.
- 각 Controller가 내부 루프를 갖도록 변경하여 서브메뉴 선택을 Controller 안에서 처리한다.
- 테이블 출력을 박스 테두리 제거 방식으로 개선하고 헤더 구분선을 사용한다.
- ANSI 색상 코드를 이용한 상태 배지를 도입한다.
- 재고 현황 테이블에 프로그레스 바를 추가한다.
- 주문 승인 플로우를 번호 선택 방식으로 개선한다.
- 주문 접수 플로우에 확인 요약 + Y/N 프롬프트를 추가한다.
- 출고 처리를 OrderController 내부 루프로 통합하고 Main에서 별도 분기를 제거한다.
- MonitorController가 시료명 매핑을 위해 SampleRepository도 주입받는다.
- 생산라인 조회를 FIFO 뷰로 개선한다: 현재 처리 중 블록(주문번호·시료·주문량→재고→부족→실생산량) + 대기 중인 주문 테이블(순서·주문번호·시료·주문량·부족분·실생산량) + 공식/FIFO 안내 문구.
- 출고 처리 완료 결과를 "CONFIRMED → RELEASE" 화살표 전환 표시 방식으로 개선한다.

---

## 2. 변경 대상 파일 목록

| 파일 경로 | 작업 |
|-----------|------|
| `src/main/java/org/example/view/ConsoleView.java` | 수정 |
| `src/main/java/org/example/controller/SampleController.java` | 수정 |
| `src/main/java/org/example/controller/OrderController.java` | 수정 |
| `src/main/java/org/example/controller/ProductionController.java` | 수정 |
| `src/main/java/org/example/controller/MonitorController.java` | 수정 |
| `src/main/java/org/example/controller/DummyController.java` | 수정 |
| `src/main/java/org/example/Main.java` | 수정 |

---

## 3. 구현 상세

### `src/main/java/org/example/view/ConsoleView.java`

```java
package org.example.view;

import org.example.model.Inventory;
import org.example.model.InventoryStatus;
import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.ProductionItem;
import org.example.model.Sample;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ConsoleView {

    // ANSI 색상 코드
    private static final String RESET  = "[0m";
    private static final String RED    = "[31m";
    private static final String GREEN  = "[32m";
    private static final String YELLOW = "[33m";
    private static final String BLUE   = "[34m";
    private static final String CYAN   = "[36m";
    private static final String GRAY   = "[90m";
    private static final String BOLD   = "[1m";

    private static final String SEP60 = "══════════════════════════════════════════════════════════";
    private static final String SEP40 = "────────────────────────────────────────";

    private final Scanner scanner;

    public ConsoleView(Scanner scanner) {
        this.scanner = scanner;
    }

    // ─── 메인 메뉴 ───────────────────────────────────────────────
    public void showMainMenu(long sampleCount, long orderCount, int productionQueueSize) {
        System.out.println();
        System.out.println(CYAN + BOLD +
            "   ███████╗       ███████╗███████╗███╗   ███╗██╗" + RESET);
        System.out.println(CYAN + BOLD +
            "   ██╔════╝       ██╔════╝██╔════╝████╗ ████║██║" + RESET);
        System.out.println(CYAN + BOLD +
            "   ███████╗ ████╗ ███████╗█████╗  ██╔████╔██║██║" + RESET);
        System.out.println(CYAN + BOLD +
            "   ╚════██║╚════╝ ╚════██║██╔══╝  ██║╚██╔╝██║██║" + RESET);
        System.out.println(CYAN + BOLD +
            "   ███████║       ███████║███████╗██║ ╚═╝ ██║██║" + RESET);
        System.out.println(SEP60);

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.printf("  %s  |  시료 %d종  |  주문 %d건  |  생산대기 %d건%n",
                now, sampleCount, orderCount, productionQueueSize);
        System.out.println(SEP60);

        System.out.println("  [1] 시료 관리          [2] 주문 접수");
        System.out.println("  [3] 주문 승인/거절      [4] 모니터링");
        System.out.println("  [5] 생산 라인          [6] 더미 데이터");
        System.out.println("  [0] 종료");
        System.out.println(SEP60);
        System.out.print("선택 > ");
    }

    // ─── 섹션 헤더 ────────────────────────────────────────────────
    public void showSectionHeader(String title) {
        System.out.println();
        System.out.println(BOLD + "─── " + title + " "
                + SEP40.substring(0, Math.max(0, 36 - title.length())) + RESET);
    }

    // ─── 인라인 서브메뉴 ──────────────────────────────────────────
    public void showSubMenu(String... options) {
        StringBuilder sb = new StringBuilder("  ");
        for (String opt : options) {
            sb.append(opt).append("   ");
        }
        System.out.println(sb.toString().stripTrailing());
        System.out.print("선택 > ");
    }

    // ─── 입력 유틸 ───────────────────────────────────────────────
    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public String readLineRaw() {
        return scanner.nextLine().trim();
    }

    public int readInt(String prompt) {
        while (true) {
            String input = readLine(prompt);
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                showError("숫자를 입력해주세요.");
            }
        }
    }

    public long readLong(String prompt) {
        while (true) {
            String input = readLine(prompt);
            try {
                return Long.parseLong(input);
            } catch (NumberFormatException e) {
                showError("숫자를 입력해주세요.");
            }
        }
    }

    public double readDouble(String prompt) {
        while (true) {
            String input = readLine(prompt);
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                showError("숫자를 입력해주세요.");
            }
        }
    }

    // ─── 확인 프롬프트 ────────────────────────────────────────────
    /**
     * [Y]/[N] 확인 프롬프트. Y 또는 y 입력 시 true 반환.
     */
    public boolean confirm(String message) {
        System.out.println(message);
        System.out.print("  [Y] 확인   [N] 취소   > ");
        String input = scanner.nextLine().trim();
        return "y".equalsIgnoreCase(input);
    }

    // ─── 상태 배지 ────────────────────────────────────────────────
    /**
     * OrderStatus에 대한 ANSI 색상 배지 문자열 반환.
     */
    public String badge(OrderStatus status) {
        return switch (status) {
            case RESERVED  -> BLUE   + "[RESERVED ]" + RESET;
            case CONFIRMED -> GREEN  + "[CONFIRMED]" + RESET;
            case PRODUCING -> YELLOW + "[PRODUCING]" + RESET;
            case REJECTED  -> RED    + "[REJECTED ]" + RESET;
            case RELEASE   -> GRAY   + "[RELEASE  ]" + RESET;
        };
    }

    /**
     * InventoryStatus에 대한 ANSI 색상 배지 문자열 반환.
     */
    public String badge(InventoryStatus status) {
        return switch (status) {
            case SUFFICIENT -> GREEN  + "여유" + RESET;
            case SHORTAGE   -> YELLOW + "부족" + RESET;
            case DEPLETED   -> RED    + "고갈" + RESET;
        };
    }

    // ─── 메시지 출력 ─────────────────────────────────────────────
    public void showSuccess(String message) {
        System.out.println(GREEN + "[SUCCESS] " + RESET + message);
    }

    public void showError(String message) {
        System.out.println(RED + "[ERROR] " + RESET + message);
    }

    public void showInfo(String message) {
        System.out.println(message);
    }

    // ─── 시료 테이블 (재고 포함) ──────────────────────────────────
    /**
     * 재고 포함 시료 테이블.
     * stockMap: sampleId -> 현재 재고 수량
     */
    public void showSampleTable(List<Sample> samples, Map<String, Integer> stockMap) {
        if (samples.isEmpty()) {
            System.out.println("  등록된 시료가 없습니다.");
            return;
        }
        System.out.printf("  %-24s  %-20s  %-16s  %-8s  %-8s%n",
                "ID", "시료명", "평균생산시간(분)", "수율", "현재재고");
        System.out.println("  " + "─".repeat(82));
        for (Sample s : samples) {
            int stock = stockMap.getOrDefault(s.getId(), 0);
            System.out.printf("  %-24s  %-20s  %-16d  %-8.2f  %-8d%n",
                    s.getId(), s.getName(), s.getAvgProductionTime(), s.getYield(), stock);
        }
    }

    /**
     * 재고 없이 출력하는 오버로드.
     */
    public void showSampleTable(List<Sample> samples) {
        if (samples.isEmpty()) {
            System.out.println("  등록된 시료가 없습니다.");
            return;
        }
        System.out.printf("  %-24s  %-20s  %-16s  %-8s%n",
                "ID", "시료명", "평균생산시간(분)", "수율");
        System.out.println("  " + "─".repeat(72));
        for (Sample s : samples) {
            System.out.printf("  %-24s  %-20s  %-16d  %-8.2f%n",
                    s.getId(), s.getName(), s.getAvgProductionTime(), s.getYield());
        }
    }

    // ─── 주문 테이블 ──────────────────────────────────────────────
    /**
     * 번호 포함 주문 목록 (선택용).
     * sampleNameMap: sampleId -> 시료명
     */
    public void showNumberedOrderList(List<Order> orders, Map<String, String> sampleNameMap) {
        if (orders.isEmpty()) {
            System.out.println("  주문이 없습니다.");
            return;
        }
        System.out.printf("  %-4s  %-10s  %-16s  %-16s  %-6s  %-13s%n",
                "번호", "주문번호", "고객명", "시료명", "수량", "상태");
        System.out.println("  " + "─".repeat(75));
        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            String shortId = o.getId().length() >= 8 ? o.getId().substring(0, 8) : o.getId();
            String sampleName = sampleNameMap.getOrDefault(o.getSampleId(), o.getSampleId());
            System.out.printf("  [%-2d]  %-10s  %-16s  %-16s  %-6d  %s%n",
                    i + 1, shortId, o.getCustomerName(), sampleName,
                    o.getQuantity(), badge(o.getStatus()));
        }
    }

    /**
     * 기존 showOrderTable 호환 오버로드.
     */
    public void showOrderTable(List<Order> orders) {
        if (orders.isEmpty()) {
            System.out.println("  주문이 없습니다.");
            return;
        }
        System.out.printf("  %-10s  %-16s  %-24s  %-6s  %-13s  %-19s%n",
                "주문번호", "고객명", "시료ID", "수량", "상태", "접수일시");
        System.out.println("  " + "─".repeat(95));
        for (Order o : orders) {
            String shortId = o.getId().length() >= 8 ? o.getId().substring(0, 8) : o.getId();
            System.out.printf("  %-10s  %-16s  %-24s  %-6d  %s  %-19s%n",
                    shortId, o.getCustomerName(), o.getSampleId(),
                    o.getQuantity(), badge(o.getStatus()), o.getCreatedAt());
        }
    }

    // ─── 재고 확인 상세 ───────────────────────────────────────────
    /**
     * 승인 전 재고 확인 정보 출력.
     */
    public void showStockCheckDetail(String sampleName, int currentStock, int requiredQty) {
        System.out.println();
        System.out.println("  ┌─ 재고 확인 ────────────────────────────┐");
        System.out.printf("  │  시료명     : %-26s│%n", sampleName);
        System.out.printf("  │  현재 재고  : %-22s ea  │%n", currentStock);
        System.out.printf("  │  주문 수량  : %-22s ea  │%n", requiredQty);
        int shortage = requiredQty - currentStock;
        if (shortage > 0) {
            System.out.printf("  │  " + YELLOW + "부족분     : %-22s ea" + RESET + "  │%n", shortage);
            System.out.println("  │  → 재고 부족으로 생산 라인에 등록됩니다 │");
        } else {
            System.out.println("  │  " + GREEN + "재고 충분 → 즉시 확정 처리됩니다" + RESET + "       │");
        }
        System.out.println("  └────────────────────────────────────────┘");
    }

    // ─── 재고 현황 테이블 (프로그레스 바 포함) ────────────────────
    /**
     * 재고 현황 테이블.
     * sampleNameMap: sampleId -> 시료명
     * statusMap: sampleId -> InventoryStatus
     */
    public void showInventoryTable(List<Inventory> inventories,
                                   Map<String, String> sampleNameMap,
                                   Map<String, InventoryStatus> statusMap) {
        if (inventories.isEmpty()) {
            System.out.println("  재고 데이터가 없습니다.");
            return;
        }
        System.out.printf("  %-24s  %-20s  %-6s  %-22s  %-6s%n",
                "시료ID", "시료명", "재고", "잔여율", "상태");
        System.out.println("  " + "─".repeat(82));
        for (Inventory inv : inventories) {
            String sampleName = sampleNameMap.getOrDefault(inv.getSampleId(), inv.getSampleId());
            InventoryStatus status = statusMap.getOrDefault(inv.getSampleId(), InventoryStatus.DEPLETED);
            String bar = buildProgressBar(inv.getStock(), status);
            System.out.printf("  %-24s  %-20s  %-6d  %s  %s%n",
                    inv.getSampleId(), sampleName, inv.getStock(), bar, badge(status));
        }
    }

    /**
     * 재고 상태 기반 프로그레스 바 생성 (20자 폭).
     */
    private String buildProgressBar(int stock, InventoryStatus status) {
        int totalWidth = 20;
        int filled;
        String color;
        if (stock == 0 || status == InventoryStatus.DEPLETED) {
            filled = 0;
            color = RED;
        } else if (status == InventoryStatus.SHORTAGE) {
            filled = (int) (totalWidth * 0.4);
            color = YELLOW;
        } else {
            filled = (int) (totalWidth * 0.8);
            color = GREEN;
        }
        int empty = totalWidth - filled;
        return color + "█".repeat(filled) + RESET + GRAY + "░".repeat(empty) + RESET;
    }

    // ─── 상태별 주문 건수 (배지 포함) ────────────────────────────
    /**
     * 상태별 주문 건수 출력.
     */
    public void showOrderCountWithBadge(Map<OrderStatus, Long> countMap, long producingCount) {
        if (countMap.isEmpty()) {
            System.out.println("  집계할 주문이 없습니다.");
            return;
        }
        System.out.printf("  %-15s  %s%n", "상태", "건수");
        System.out.println("  " + "─".repeat(30));
        for (OrderStatus status : OrderStatus.values()) {
            long count = countMap.getOrDefault(status, 0L);
            System.out.printf("  %s  %d건%n", badge(status), count);
        }
        System.out.println();
        System.out.printf("  생산 대기 큐: %d건%n", producingCount);
    }

    /**
     * 기존 showOrderCountTable 호환 오버로드.
     */
    public void showOrderCountTable(Map<OrderStatus, Long> countMap) {
        showOrderCountWithBadge(countMap, 0L);
    }

    // ─── 기존 showInventoryStatusTable 호환 ──────────────────────
    public void showInventoryStatusTable(Map<String, InventoryStatus> statusMap,
                                         List<Inventory> inventories) {
        if (inventories.isEmpty()) {
            System.out.println("  재고 데이터가 없습니다.");
            return;
        }
        System.out.printf("  %-24s  %-6s  %-6s%n", "시료ID", "재고", "상태");
        System.out.println("  " + "─".repeat(38));
        for (Inventory inv : inventories) {
            InventoryStatus status = statusMap.getOrDefault(inv.getSampleId(), InventoryStatus.DEPLETED);
            System.out.printf("  %-24s  %-6d  %s%n",
                    inv.getSampleId(), inv.getStock(), badge(status));
        }
    }

    // ─── 주문 접수 결과 ───────────────────────────────────────────
    public void showOrderPlacedResult(Order order) {
        System.out.println();
        System.out.println(GREEN + "  [SUCCESS] 주문이 접수되었습니다." + RESET);
        System.out.printf("  주문번호  : %s%n", order.getId());
        System.out.printf("  시료 ID   : %s%n", order.getSampleId());
        System.out.printf("  고객명    : %s%n", order.getCustomerName());
        System.out.printf("  수량      : %d ea%n", order.getQuantity());
        System.out.printf("  상태      : %s%n", badge(order.getStatus()));
        System.out.printf("  접수일시  : %s%n", order.getCreatedAt());
    }

    // ─── 승인/거절 결과 ───────────────────────────────────────────
    public void showOrderStatusChanged(Order order) {
        System.out.println();
        System.out.println(GREEN + "  [SUCCESS] 상태가 변경되었습니다." + RESET);
        System.out.printf("  주문번호  : %s%n", order.getId());
        System.out.printf("  고객명    : %s%n", order.getCustomerName());
        System.out.printf("  변경 상태 : %s%n", badge(order.getStatus()));
    }

    // ─── 출고 처리 완료 결과 (Image #7 스타일) ────────────────────
    /**
     * 출고 처리 완료 결과 출력.
     * CONFIRMED → RELEASE 상태 전환 화살표 포함.
     */
    public void showReleaseComplete(Order order) {
        System.out.println();
        System.out.println(BOLD + "  출고 처리 완료." + RESET);
        System.out.println();
        System.out.printf("  주문번호  : %s%n", order.getId());
        System.out.printf("  출고 수량 : %d ea%n", order.getQuantity());
        System.out.printf("  처리일시  : %s%n",
                java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.printf("  상태      : %s  →  %s%n",
                GRAY + "[CONFIRMED]" + RESET,
                badge(OrderStatus.RELEASE));
    }

    // ─── 생산라인 뷰 (Image #6 스타일) ────────────────────────────
    /**
     * 생산라인 FIFO 뷰.
     * queue: productionService.getQueueStatus() 반환값
     * sampleNameMap: sampleId -> 시료명
     * orderQuantityMap: orderId -> 원래 주문 수량 (lookup용)
     */
    public void showProductionLineView(List<ProductionItem> queue,
                                       Map<String, String> sampleNameMap,
                                       Map<String, Integer> orderQuantityMap) {
        boolean running = !queue.isEmpty();
        String lineStatus = running
                ? GREEN  + "RUNNING" + RESET
                : GRAY   + "IDLE"    + RESET;

        System.out.println();
        System.out.println("  생산라인 1개 (단일 라인)   현재 상태: " + lineStatus);
        System.out.println();

        if (!running) {
            System.out.println("  대기 중인 주문이 없습니다.");
            return;
        }

        // 현재 처리 중 (FIFO 첫 번째 항목)
        ProductionItem current = queue.get(0);
        String curSampleName = sampleNameMap.getOrDefault(current.getSampleId(), current.getSampleId());
        int curOrderQty = orderQuantityMap.getOrDefault(current.getOrderId(), current.getRequiredQuantity());

        System.out.println(BOLD + "  현재 처리 중" + RESET);
        System.out.println("  ┌────────────────────────────────────────────────────────────┐");
        System.out.printf("  │  주문번호  %-12s   시료  %-20s    │%n",
                current.getOrderId().substring(0, Math.min(8, current.getOrderId().length())),
                curSampleName);
        System.out.printf("  │  주문량 %d ea   재고 %d ea  →  " + YELLOW + "부족 %d ea" + RESET
                        + "  →  " + GREEN + "실생산량 %d ea" + RESET + "%n",
                curOrderQty,
                Math.max(0, curOrderQty - current.getRequiredQuantity()),
                current.getRequiredQuantity(),
                current.getActualProduction());
        // 진행 바: enqueuedAt 기반으로 경과 시간 계산
        System.out.printf("  │  생산시간  %d분   큐등록  %s%n",
                current.getTotalProductionTime(), current.getEnqueuedAt());
        System.out.println("  └────────────────────────────────────────────────────────────┘");

        // 대기 중인 주문 (나머지)
        System.out.println();
        System.out.println(BOLD + "  대기 중인 주문 (FIFO 순)" + RESET);

        if (queue.size() <= 1) {
            System.out.println("  (대기 주문 없음)");
        } else {
            System.out.printf("  %-4s  %-10s  %-20s  %-8s  %-8s  %-8s  %-19s%n",
                    "순서", "주문번호", "시료", "주문량", "부족분", "실생산량", "큐등록일시");
            System.out.println("  " + "─".repeat(88));
            for (int i = 1; i < queue.size(); i++) {
                ProductionItem item = queue.get(i);
                String shortId = item.getOrderId().length() >= 8
                        ? item.getOrderId().substring(0, 8) : item.getOrderId();
                String sampleName = sampleNameMap.getOrDefault(item.getSampleId(), item.getSampleId());
                int orderQty = orderQuantityMap.getOrDefault(item.getOrderId(), item.getRequiredQuantity());
                System.out.printf("  %-4d  %-10s  %-20s  %-8d  %-8d  %-8d  %-19s%n",
                        i, shortId, sampleName,
                        orderQty,
                        item.getRequiredQuantity(),
                        item.getActualProduction(),
                        item.getEnqueuedAt());
            }
        }

        System.out.println();
        System.out.println("  * 부족분 = 주문량 - 재고,  실생산량 = ceil(부족분 / (수율 × 0.9))");
        System.out.println("  * 선입선출(FIFO) 방식으로 처리됩니다.");
    }

    /**
     * 기존 showProductionQueueTable 호환 오버로드 — 시료명 없이 간이 출력.
     */
    public void showProductionQueueTable(List<ProductionItem> queue) {
        if (queue.isEmpty()) {
            System.out.println("  생산 대기 큐가 비어 있습니다.");
            return;
        }
        System.out.printf("  %-10s  %-24s  %-8s  %-8s  %-14s  %-19s%n",
                "주문번호", "시료ID", "필요수량", "실생산량", "생산시간(분)", "큐등록일시");
        System.out.println("  " + "─".repeat(92));
        for (ProductionItem item : queue) {
            String shortId = item.getOrderId().length() >= 8
                    ? item.getOrderId().substring(0, 8) : item.getOrderId();
            System.out.printf("  %-10s  %-24s  %-8d  %-8d  %-14d  %-19s%n",
                    shortId, item.getSampleId(),
                    item.getRequiredQuantity(), item.getActualProduction(),
                    item.getTotalProductionTime(), item.getEnqueuedAt());
        }
    }
}
```

---

### `src/main/java/org/example/controller/SampleController.java`

```java
package org.example.controller;

import org.example.model.Inventory;
import org.example.model.Sample;
import org.example.repository.InventoryRepository;
import org.example.service.SampleService;
import org.example.view.ConsoleView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SampleController {

    private final SampleService sampleService;
    private final InventoryRepository inventoryRepo;
    private final ConsoleView view;

    public SampleController(SampleService sampleService, InventoryRepository inventoryRepo, ConsoleView view) {
        this.sampleService = sampleService;
        this.inventoryRepo = inventoryRepo;
        this.view = view;
    }

    /**
     * 내부 루프를 돌며 "0" 입력 시 반환.
     */
    public void handle() {
        while (true) {
            view.showSectionHeader("[1] 시료 관리");
            view.showSubMenu("[1] 시료 등록", "[2] 시료 목록", "[3] 시료 검색", "[0] 뒤로");
            String input = view.readLineRaw();
            if ("0".equals(input)) break;
            try {
                switch (input) {
                    case "1" -> register();
                    case "2" -> listAll();
                    case "3" -> search();
                    default  -> view.showError("잘못된 입력입니다.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                view.showError(e.getMessage());
            }
        }
    }

    private void register() {
        String id    = view.readLine("시료 ID         > ");
        String name  = view.readLine("시료명          > ");
        long avgTime = view.readLong("평균생산시간(분) > ");
        double yield = view.readDouble("수율(0.0~1.0)  > ");

        boolean ok = view.confirm(String.format(
                "  등록 정보: ID=%s  이름=%s  생산시간=%d분  수율=%.2f", id, name, avgTime, yield));
        if (!ok) {
            view.showInfo("  등록이 취소되었습니다.");
            return;
        }
        Sample sample = sampleService.register(id, name, avgTime, yield);
        view.showSuccess("시료가 등록되었습니다. ID=" + sample.getId());
    }

    private void listAll() {
        List<Sample> samples = sampleService.findAll();
        Map<String, Integer> stockMap = buildStockMap(samples);
        view.showSampleTable(samples, stockMap);
    }

    private void search() {
        String keyword = view.readLine("검색어 > ");
        List<Sample> samples = sampleService.search(keyword);
        Map<String, Integer> stockMap = buildStockMap(samples);
        view.showSampleTable(samples, stockMap);
    }

    private Map<String, Integer> buildStockMap(List<Sample> samples) {
        Map<String, Integer> map = new HashMap<>();
        for (Sample s : samples) {
            Inventory inv = inventoryRepo.findOrCreate(s.getId());
            map.put(s.getId(), inv.getStock());
        }
        return map;
    }
}
```

---

### `src/main/java/org/example/controller/OrderController.java`

```java
package org.example.controller;

import org.example.model.Inventory;
import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.Sample;
import org.example.repository.InventoryRepository;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;
import org.example.service.OrderService;
import org.example.view.ConsoleView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepo;
    private final SampleRepository sampleRepo;
    private final InventoryRepository inventoryRepo;
    private final ConsoleView view;

    public OrderController(OrderService orderService,
                           OrderRepository orderRepo,
                           SampleRepository sampleRepo,
                           InventoryRepository inventoryRepo,
                           ConsoleView view) {
        this.orderService = orderService;
        this.orderRepo = orderRepo;
        this.sampleRepo = sampleRepo;
        this.inventoryRepo = inventoryRepo;
        this.view = view;
    }

    /**
     * 내부 루프를 돌며 "0" 입력 시 반환.
     * 주문 접수(1), 승인(2), 거절(3), 출고(4) 모두 이 루프 내에서 처리.
     */
    public void handle() {
        while (true) {
            view.showSectionHeader("[2/3] 주문 관리");
            view.showSubMenu(
                "[1] 주문 접수", "[2] 주문 승인", "[3] 주문 거절", "[4] 출고 처리", "[0] 뒤로");
            String input = view.readLineRaw();
            if ("0".equals(input)) break;
            try {
                switch (input) {
                    case "1" -> placeOrder();
                    case "2" -> approve();
                    case "3" -> reject();
                    case "4" -> release();
                    default  -> view.showError("잘못된 입력입니다.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                view.showError(e.getMessage());
            }
        }
    }

    private void placeOrder() {
        view.showSectionHeader("주문 접수");
        String sampleId     = view.readLine("시료 ID   > ");
        String customerName = view.readLine("고객명    > ");
        int quantity        = view.readInt("주문 수량 > ");

        Optional<Sample> sampleOpt = sampleRepo.findById(sampleId);
        if (sampleOpt.isEmpty()) {
            view.showError("등록되지 않은 시료 ID입니다: " + sampleId);
            return;
        }
        String sampleName = sampleOpt.get().getName();

        boolean ok = view.confirm(String.format(
                "  주문 확인: 시료=%s(%s)  고객=%s  수량=%d ea",
                sampleId, sampleName, customerName, quantity));
        if (!ok) {
            view.showInfo("  주문 접수가 취소되었습니다.");
            return;
        }
        Order order = orderService.placeOrder(sampleId, customerName, quantity);
        view.showOrderPlacedResult(order);
    }

    private void approve() {
        view.showSectionHeader("주문 승인");
        List<Order> reserved = orderRepo.findByStatus(OrderStatus.RESERVED);
        Map<String, String> sampleNameMap = buildSampleNameMap();
        view.showNumberedOrderList(reserved, sampleNameMap);
        if (reserved.isEmpty()) return;

        int idx = view.readInt("승인할 번호 > ");
        if (idx < 1 || idx > reserved.size()) {
            view.showError("올바른 번호를 입력하세요.");
            return;
        }
        Order target = reserved.get(idx - 1);

        String sampleName = sampleNameMap.getOrDefault(target.getSampleId(), target.getSampleId());
        Inventory inv = inventoryRepo.findOrCreate(target.getSampleId());
        view.showStockCheckDetail(sampleName, inv.getStock(), target.getQuantity());

        boolean ok = view.confirm("  이 주문을 승인하시겠습니까?");
        if (!ok) {
            view.showInfo("  승인이 취소되었습니다.");
            return;
        }
        Order updated = orderService.approve(target.getId());
        view.showOrderStatusChanged(updated);
    }

    private void reject() {
        view.showSectionHeader("주문 거절");
        List<Order> reserved = orderRepo.findByStatus(OrderStatus.RESERVED);
        Map<String, String> sampleNameMap = buildSampleNameMap();
        view.showNumberedOrderList(reserved, sampleNameMap);
        if (reserved.isEmpty()) return;

        int idx = view.readInt("거절할 번호 > ");
        if (idx < 1 || idx > reserved.size()) {
            view.showError("올바른 번호를 입력하세요.");
            return;
        }
        Order target = reserved.get(idx - 1);

        boolean ok = view.confirm("  이 주문을 거절하시겠습니까?");
        if (!ok) {
            view.showInfo("  거절이 취소되었습니다.");
            return;
        }
        Order updated = orderService.reject(target.getId());
        view.showOrderStatusChanged(updated);
    }

    private void release() {
        view.showSectionHeader("출고 처리");
        List<Order> confirmed = orderRepo.findByStatus(OrderStatus.CONFIRMED);
        Map<String, String> sampleNameMap = buildSampleNameMap();
        view.showNumberedOrderList(confirmed, sampleNameMap);
        if (confirmed.isEmpty()) return;

        int idx = view.readInt("출고할 번호 > ");
        if (idx < 1 || idx > confirmed.size()) {
            view.showError("올바른 번호를 입력하세요.");
            return;
        }
        Order target = confirmed.get(idx - 1);

        boolean ok = view.confirm("  이 주문을 출고 처리하시겠습니까?");
        if (!ok) {
            view.showInfo("  출고 처리가 취소되었습니다.");
            return;
        }
        Order updated = orderService.release(target.getId());
        view.showReleaseComplete(updated);
    }

    private Map<String, String> buildSampleNameMap() {
        Map<String, String> map = new HashMap<>();
        for (Sample s : sampleRepo.findAll()) {
            map.put(s.getId(), s.getName());
        }
        return map;
    }
}
```

---

### `src/main/java/org/example/controller/ProductionController.java`

```java
package org.example.controller;

import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.ProductionItem;
import org.example.model.Sample;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;
import org.example.service.ProductionService;
import org.example.view.ConsoleView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductionController {

    private final ProductionService productionService;
    private final OrderRepository orderRepo;
    private final SampleRepository sampleRepo;
    private final ConsoleView view;

    public ProductionController(ProductionService productionService,
                                OrderRepository orderRepo,
                                SampleRepository sampleRepo,
                                ConsoleView view) {
        this.productionService = productionService;
        this.orderRepo = orderRepo;
        this.sampleRepo = sampleRepo;
        this.view = view;
    }

    /**
     * 내부 루프를 돌며 "0" 입력 시 반환.
     */
    public void handle() {
        while (true) {
            view.showSectionHeader("[5] 생산 라인");
            view.showSubMenu(
                "[1] 생산 중 목록", "[2] 생산라인 조회", "[3] 생산 완료 처리", "[0] 뒤로");
            String input = view.readLineRaw();
            if ("0".equals(input)) break;
            try {
                switch (input) {
                    case "1" -> listActiveProductions();
                    case "2" -> showProductionLine();
                    case "3" -> completeProduction();
                    default  -> view.showError("잘못된 입력입니다.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                view.showError(e.getMessage());
            }
        }
    }

    private void listActiveProductions() {
        List<Order> orders = productionService.getActiveProductions();
        view.showOrderTable(orders);
    }

    /**
     * 생산라인 FIFO 뷰 출력 (Image #6 스타일).
     * sampleNameMap, orderQuantityMap을 조합하여 showProductionLineView 호출.
     */
    private void showProductionLine() {
        List<ProductionItem> queue = productionService.getQueueStatus();

        // 시료명 맵
        Map<String, String> sampleNameMap = new HashMap<>();
        for (Sample s : sampleRepo.findAll()) {
            sampleNameMap.put(s.getId(), s.getName());
        }

        // 주문 수량 맵 (orderId -> quantity)
        Map<String, Integer> orderQuantityMap = new HashMap<>();
        for (Order o : orderRepo.findAll()) {
            orderQuantityMap.put(o.getId(), o.getQuantity());
        }

        view.showProductionLineView(queue, sampleNameMap, orderQuantityMap);
    }

    private void completeProduction() {
        view.showSectionHeader("생산 완료 처리");
        List<Order> producing = orderRepo.findByStatus(OrderStatus.PRODUCING);
        if (producing.isEmpty()) {
            view.showInfo("  생산 중인 주문이 없습니다.");
            return;
        }
        view.showOrderTable(producing);
        String orderId = view.readLine("완료 처리할 주문 ID(전체 입력) > ");
        Order order = productionService.completeProduction(orderId);
        view.showOrderStatusChanged(order);
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
import org.example.model.Sample;
import org.example.repository.InventoryRepository;
import org.example.repository.SampleRepository;
import org.example.service.MonitorService;
import org.example.view.ConsoleView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonitorController {

    private final MonitorService monitorService;
    private final InventoryRepository inventoryRepo;
    private final SampleRepository sampleRepo;
    private final ConsoleView view;

    public MonitorController(MonitorService monitorService,
                             InventoryRepository inventoryRepo,
                             SampleRepository sampleRepo,
                             ConsoleView view) {
        this.monitorService = monitorService;
        this.inventoryRepo = inventoryRepo;
        this.sampleRepo = sampleRepo;
        this.view = view;
    }

    /**
     * 내부 루프를 돌며 "0" 입력 시 반환.
     */
    public void handle() {
        while (true) {
            view.showSectionHeader("[4] 모니터링");
            view.showSubMenu("[1] 주문 현황", "[2] 재고 현황", "[0] 뒤로");
            String input = view.readLineRaw();
            if ("0".equals(input)) break;
            try {
                switch (input) {
                    case "1" -> showOrderCountByStatus();
                    case "2" -> showInventoryStatus();
                    default  -> view.showError("잘못된 입력입니다.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                view.showError(e.getMessage());
            }
        }
    }

    private void showOrderCountByStatus() {
        Map<OrderStatus, Long> countMap = monitorService.getOrderCountByStatus();
        long producingCount = countMap.getOrDefault(OrderStatus.PRODUCING, 0L);
        view.showOrderCountWithBadge(countMap, producingCount);
    }

    private void showInventoryStatus() {
        Map<String, InventoryStatus> statusMap = monitorService.getInventoryStatus();
        List<Inventory> inventories = inventoryRepo.findAll();

        Map<String, String> sampleNameMap = new HashMap<>();
        for (Sample s : sampleRepo.findAll()) {
            sampleNameMap.put(s.getId(), s.getName());
        }

        view.showInventoryTable(inventories, sampleNameMap, statusMap);
    }
}
```

---

### `src/main/java/org/example/controller/DummyController.java`

```java
package org.example.controller;

import org.example.dummy.OrderGenerator;
import org.example.dummy.SampleGenerator;
import org.example.view.ConsoleView;

public class DummyController {

    private final SampleGenerator sampleGenerator;
    private final OrderGenerator orderGenerator;
    private final ConsoleView view;

    public DummyController(SampleGenerator sampleGenerator,
                           OrderGenerator orderGenerator,
                           ConsoleView view) {
        this.sampleGenerator = sampleGenerator;
        this.orderGenerator = orderGenerator;
        this.view = view;
    }

    /**
     * 내부 루프를 돌며 "0" 입력 시 반환.
     */
    public void handle() {
        while (true) {
            view.showSectionHeader("[6] 더미 데이터");
            view.showSubMenu("[1] 시료 더미 생성", "[2] 주문 더미 생성", "[0] 뒤로");
            String input = view.readLineRaw();
            if ("0".equals(input)) break;
            try {
                switch (input) {
                    case "1" -> generateSamples();
                    case "2" -> generateOrders();
                    default  -> view.showError("잘못된 입력입니다.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                view.showError(e.getMessage());
            }
        }
    }

    private void generateSamples() {
        int count = view.readInt("생성할 시료 수 > ");
        sampleGenerator.generate(count);
        view.showSuccess(count + "개의 더미 시료 데이터가 생성되었습니다.");
    }

    private void generateOrders() {
        int count = view.readInt("생성할 주문 수 > ");
        orderGenerator.generate(count);
        view.showSuccess(count + "개의 더미 주문 데이터가 생성되었습니다.");
    }
}
```

---

### `src/main/java/org/example/Main.java`

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
        new File("data").mkdirs();

        File samplesFile   = new File("data/samples.json");
        File ordersFile    = new File("data/orders.json");
        File inventoryFile = new File("data/inventory.json");

        SampleRepository    sampleRepo    = new SampleRepository(samplesFile);
        OrderRepository     orderRepo     = new OrderRepository(ordersFile);
        InventoryRepository inventoryRepo = new InventoryRepository(inventoryFile);

        List<ProductionItem> productionQueue = new ArrayList<>();

        SampleService     sampleService     = new SampleService(sampleRepo, inventoryRepo);
        OrderService      orderService      = new OrderService(orderRepo, sampleRepo, inventoryRepo, productionQueue);
        ProductionService productionService = new ProductionService(productionQueue, orderRepo, inventoryRepo);
        MonitorService    monitorService    = new MonitorService(orderRepo, inventoryRepo, sampleRepo);

        ConsoleView view = new ConsoleView(new Scanner(System.in));

        SampleController     sampleCtrl      = new SampleController(sampleService, inventoryRepo, view);
        OrderController      orderCtrl       = new OrderController(orderService, orderRepo, sampleRepo, inventoryRepo, view);
        ProductionController productionCtrl  = new ProductionController(productionService, orderRepo, sampleRepo, view);
        MonitorController    monitorCtrl     = new MonitorController(monitorService, inventoryRepo, sampleRepo, view);
        SampleGenerator      sampleGenerator = new SampleGenerator(sampleRepo, inventoryRepo);
        OrderGenerator       orderGenerator  = new OrderGenerator(orderRepo, sampleRepo);
        DummyController      dummyCtrl       = new DummyController(sampleGenerator, orderGenerator, view);

        while (true) {
            long sampleCount = sampleRepo.findAll().size();
            long orderCount  = orderRepo.findAll().size();
            int  queueSize   = productionQueue.size();

            view.showMainMenu(sampleCount, orderCount, queueSize);
            String input = view.readLineRaw();

            if ("0".equals(input)) {
                view.showInfo("시스템을 종료합니다.");
                break;
            }

            try {
                switch (input) {
                    case "1" -> sampleCtrl.handle();
                    case "2", "3" -> orderCtrl.handle();
                    case "4" -> monitorCtrl.handle();
                    case "5" -> productionCtrl.handle();
                    case "6" -> dummyCtrl.handle();
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

## 4. 완료 기준

1. `./gradlew run` 실행 시 ASCII 아트 배너와 시스템 현황(시료 N종 | 주문 N건 | 생산대기 N건)이 출력되고 `선택 > ` 프롬프트가 표시된다.
2. 메뉴 1 진입 시 인라인 서브메뉴가 출력되고 `0` 입력 시 메인 메뉴로 복귀한다.
3. 시료 목록 출력 시 `+---+` 박스 테두리 없이 헤더 아래 `─` 구분선만 표시되며, 현재 재고 컬럼이 포함된다.
4. 주문 승인 시 RESERVED 상태 목록이 번호로 표시되고, 번호 선택 후 재고 확인 상세(시료명 | 현재 재고 | 주문 수량 | 부족분)가 출력된다.
5. 주문 접수 시 3개 필드 입력 후 확인 요약이 출력되고 Y/N 프롬프트가 나타난다.
6. ANSI 지원 터미널에서 RESERVED/CONFIRMED/PRODUCING/REJECTED/RELEASE 상태 배지가 각각 파란색/초록색/노란색/빨간색/회색으로 표시된다.
7. 모니터링 재고 현황 테이블에 `█`/`░` 프로그레스 바 컬럼과 시료명 컬럼이 포함된다.
8. 메인 메뉴 선택 `2` 또는 `3` 입력 시 OrderController 내부 루프(주문 접수/승인/거절/출고 통합)로 진입한다.
9. 출고 처리는 OrderController 내부(서브메뉴 `4`)에서 처리되며 Main에서 별도 분기가 없다.
10. 생산라인 조회([5] → [2]) 시 "생산라인 1개 (단일 라인) 현재 상태: RUNNING/IDLE"이 표시되고, 큐가 있을 경우 첫 항목이 "현재 처리 중" 블록으로, 나머지가 "대기 중인 주문 (FIFO 순)" 테이블로 출력된다. 테이블 하단에 부족분 공식과 FIFO 안내 문구가 표시된다.
11. 출고 처리 완료 시 "출고 처리 완료." 헤더와 함께 주문번호·출고수량·처리일시·상태(CONFIRMED → [RELEASE  ]) 항목이 출력된다.
12. `./gradlew build` 에러 없이 완료된다.
