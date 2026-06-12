package org.example;

import org.example.controller.DummyController;
import org.example.controller.MonitorController;
import org.example.controller.OrderController;
import org.example.controller.ProductionController;
import org.example.controller.SampleController;
import org.example.dummy.OrderGenerator;
import org.example.dummy.SampleGenerator;
import org.example.model.ProductionItem;
import org.example.repository.InventoryRepository;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;
import org.example.service.MonitorService;
import org.example.service.OrderService;
import org.example.service.ProductionService;
import org.example.service.SampleService;
import org.example.view.ConsoleView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        // data/ 디렉터리 자동 생성
        new File("data").mkdirs();

        File samplesFile   = new File("data/samples.json");
        File ordersFile    = new File("data/orders.json");
        File inventoryFile = new File("data/inventory.json");

        // Repository 초기화
        SampleRepository    sampleRepo    = new SampleRepository(samplesFile);
        OrderRepository     orderRepo     = new OrderRepository(ordersFile);
        InventoryRepository inventoryRepo = new InventoryRepository(inventoryFile);

        // 공유 생산 큐 (OrderService ↔ ProductionService 공유)
        List<ProductionItem> productionQueue = new ArrayList<>();

        // Service 초기화
        SampleService     sampleService     = new SampleService(sampleRepo, inventoryRepo);
        OrderService      orderService      = new OrderService(orderRepo, sampleRepo, inventoryRepo, productionQueue);
        ProductionService productionService = new ProductionService(productionQueue, orderRepo, inventoryRepo);
        MonitorService    monitorService    = new MonitorService(orderRepo, inventoryRepo, sampleRepo);

        // View 초기화
        ConsoleView view = new ConsoleView(new Scanner(System.in));

        // Controller 초기화
        SampleController     sampleCtrl      = new SampleController(sampleService, view);
        OrderController      orderCtrl       = new OrderController(orderService, view);
        ProductionController productionCtrl  = new ProductionController(productionService, view);
        MonitorController    monitorCtrl     = new MonitorController(monitorService, inventoryRepo, view);
        SampleGenerator      sampleGenerator = new SampleGenerator(sampleRepo, inventoryRepo);
        OrderGenerator       orderGenerator  = new OrderGenerator(orderRepo, sampleRepo);
        DummyController      dummyCtrl       = new DummyController(sampleGenerator, orderGenerator, view);

        // 메인 루프
        while (true) {
            view.showMainMenu();
            String mainMenu = view.readLine("메뉴 선택: ");

            if ("0".equals(mainMenu)) {
                view.showInfo("시스템을 종료합니다.");
                break;
            }

            // 출고 처리는 서브메뉴 없이 바로 orderId 입력
            if ("4".equals(mainMenu)) {
                try {
                    orderCtrl.handle("4");
                } catch (IllegalArgumentException | IllegalStateException e) {
                    view.showError(e.getMessage());
                }
                continue;
            }

            String subMenu = view.readLine("세부 메뉴 선택: ");
            try {
                switch (mainMenu) {
                    case "1" -> sampleCtrl.handle(subMenu);
                    case "2" -> orderCtrl.handle(subMenu);
                    case "3" -> monitorCtrl.handle(subMenu);
                    case "5" -> productionCtrl.handle(subMenu);
                    case "6" -> dummyCtrl.handle(subMenu);
                    default  -> view.showError("잘못된 메뉴 입력입니다.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                view.showError(e.getMessage());
            }
        }
    }
}
