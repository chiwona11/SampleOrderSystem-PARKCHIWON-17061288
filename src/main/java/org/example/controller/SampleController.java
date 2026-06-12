package org.example.controller;

import org.example.model.Sample;
import org.example.service.SampleService;
import org.example.view.ConsoleView;

import java.util.List;

public class SampleController {

    private final SampleService sampleService;
    private final ConsoleView view;

    public SampleController(SampleService sampleService, ConsoleView view) {
        this.sampleService = sampleService;
        this.view = view;
    }

    public void handle(String subMenu) {
        try {
            switch (subMenu) {
                case "1" -> register();
                case "2" -> listAll();
                case "3" -> search();
                default  -> view.showError("잘못된 메뉴 입력입니다.");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            view.showError(e.getMessage());
        }
    }

    private void register() {
        String id = view.readLine("시료 ID: ");
        String name = view.readLine("시료 이름: ");
        long avgProductionTime = view.readLong("평균 생산시간 (분): ");
        double yield = view.readDouble("수율 (0.0 ~ 1.0): ");
        Sample sample = sampleService.register(id, name, avgProductionTime, yield);
        view.showSuccess("시료가 등록되었습니다. ID=" + sample.getId());
    }

    private void listAll() {
        List<Sample> samples = sampleService.findAll();
        view.showSampleTable(samples);
    }

    private void search() {
        String keyword = view.readLine("검색어: ");
        List<Sample> samples = sampleService.search(keyword);
        view.showSampleTable(samples);
    }
}
