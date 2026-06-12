package org.example.controller;

import org.example.view.ConsoleView;

public class DummyController {

    private final ConsoleView view;

    public DummyController(ConsoleView view) {
        this.view = view;
    }

    public void handle(String subMenu) {
        switch (subMenu) {
            case "1" -> generateSamples();
            case "2" -> generateOrders();
            default  -> view.showError("잘못된 메뉴 입력입니다.");
        }
    }

    public void generateSamples() {
        view.showInfo("더미 시료 데이터 생성 기능은 Phase 5에서 구현됩니다.");
    }

    public void generateOrders() {
        view.showInfo("더미 주문 데이터 생성 기능은 Phase 5에서 구현됩니다.");
    }
}
