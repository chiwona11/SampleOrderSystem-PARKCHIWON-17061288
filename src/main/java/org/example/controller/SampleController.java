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
