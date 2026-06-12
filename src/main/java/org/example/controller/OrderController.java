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
