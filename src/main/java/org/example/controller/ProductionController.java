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
