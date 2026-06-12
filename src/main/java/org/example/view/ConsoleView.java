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
