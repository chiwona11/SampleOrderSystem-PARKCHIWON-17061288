package org.example.controller;

import org.example.dummy.OrderGenerator;
import org.example.dummy.SampleGenerator;
import org.example.view.ConsoleView;

public class DummyController {

    private final SampleGenerator sampleGenerator;
    private final OrderGenerator orderGenerator;
    private final ConsoleView view;

    public DummyController(SampleGenerator sampleGenerator, OrderGenerator orderGenerator, ConsoleView view) {
        this.sampleGenerator = sampleGenerator;
        this.orderGenerator = orderGenerator;
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
        int count = view.readInt("생성할 시료 수: ");
        sampleGenerator.generate(count);
        view.showSuccess(count + "개의 더미 시료 데이터가 생성되었습니다.");
    }

    public void generateOrders() {
        int count = view.readInt("생성할 주문 수: ");
        try {
            orderGenerator.generate(count);
            view.showSuccess(count + "개의 더미 주문 데이터가 생성되었습니다.");
        } catch (IllegalStateException e) {
            view.showError(e.getMessage());
        }
    }
}
