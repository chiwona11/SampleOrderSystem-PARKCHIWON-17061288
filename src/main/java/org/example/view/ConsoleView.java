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
