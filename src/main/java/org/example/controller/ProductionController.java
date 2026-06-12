package org.example.controller;

import org.example.model.Order;
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
     * FIFO 뷰를 출력하고 번호 선택으로 생산 완료 처리. "0" 입력 시 반환. (Hotfix-5)
     * 큐가 비어있어도 화면을 유지하며 사용자가 "0"을 눌러야 메인으로 복귀한다.
     */
    public void handle() {
        while (true) {
            view.showSectionHeader("[5] 생산 라인");
            List<ProductionItem> queue = productionService.getQueueStatus();

            Map<String, String> sampleNameMap = new HashMap<>();
            for (Sample s : sampleRepo.findAll()) {
                sampleNameMap.put(s.getId(), s.getName());
            }
            Map<String, Integer> orderQuantityMap = new HashMap<>();
            for (Order o : orderRepo.findAll()) {
                orderQuantityMap.put(o.getId(), o.getQuantity());
            }

            view.showProductionLineView(queue, sampleNameMap, orderQuantityMap);

            if (queue.isEmpty()) {
                view.showSubMenu("[0] 뒤로");
                String back = view.readLineRaw();
                if ("0".equals(back)) break;
                continue;
            }

            view.showSubMenu("[번호] 완료 처리", "[0] 뒤로");
            String input = view.readLineRaw();
            if ("0".equals(input)) break;

            int idx;
            try {
                idx = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                view.showError("숫자를 입력하세요.");
                continue;
            }
            if (idx < 1 || idx > queue.size()) {
                view.showError("올바른 번호를 입력하세요.");
                continue;
            }

            try {
                String orderId = queue.get(idx - 1).getOrderId();
                Order updated = productionService.completeProduction(orderId);
                view.showOrderStatusChanged(updated);
            } catch (IllegalArgumentException | IllegalStateException e) {
                view.showError(e.getMessage());
            }
        }
    }
}
