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
