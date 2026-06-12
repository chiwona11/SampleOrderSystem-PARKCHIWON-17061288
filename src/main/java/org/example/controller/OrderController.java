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
