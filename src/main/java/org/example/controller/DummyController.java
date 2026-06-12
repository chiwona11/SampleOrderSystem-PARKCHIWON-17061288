package org.example.controller;

import org.example.dummy.OrderGenerator;
import org.example.dummy.SampleGenerator;
import org.example.view.ConsoleView;

public class DummyController {

    private final SampleGenerator sampleGenerator;
    private final OrderGenerator orderGenerator;
    private final ConsoleView view;

    public DummyController(SampleGenerator sampleGenerator,
                           OrderGenerator orderGenerator,
                           ConsoleView view) {
        this.sampleGenerator = sampleGenerator;
        this.orderGenerator = orderGenerator;
        this.view = view;
    }

    /**
     * 내부 루프를 돌며 "0" 입력 시 반환.
     */
    public void handle() {
        while (true) {
            view.showSectionHeader("[6] 더미 데이터");
            view.showSubMenu("[1] 시료 더미 생성", "[2] 주문 더미 생성", "[0] 뒤로");
            String input = view.readLineRaw();
            if ("0".equals(input)) break;
            try {
                switch (input) {
                    case "1" -> generateSamples();
                    case "2" -> generateOrders();
                    default  -> view.showError("잘못된 입력입니다.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                view.showError(e.getMessage());
            }
        }
    }

    private void generateSamples() {
        int count = view.readInt("생성할 시료 수 > ");
        sampleGenerator.generate(count);
        view.showSuccess(count + "개의 더미 시료 데이터가 생성되었습니다.");
    }

    private void generateOrders() {
        int count = view.readInt("생성할 주문 수 > ");
        orderGenerator.generate(count);
        view.showSuccess(count + "개의 더미 주문 데이터가 생성되었습니다.");
    }
}
