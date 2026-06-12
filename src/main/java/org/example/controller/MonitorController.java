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
