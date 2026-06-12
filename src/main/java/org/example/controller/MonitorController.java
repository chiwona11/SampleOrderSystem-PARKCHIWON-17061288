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
