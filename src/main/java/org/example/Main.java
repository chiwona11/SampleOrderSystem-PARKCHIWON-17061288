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
        new File("data").mkdirs();

        File samplesFile   = new File("data/samples.json");
        File ordersFile    = new File("data/orders.json");
        File inventoryFile = new File("data/inventory.json");

        SampleRepository    sampleRepo    = new SampleRepository(samplesFile);
        OrderRepository     orderRepo     = new OrderRepository(ordersFile);
        InventoryRepository inventoryRepo = new InventoryRepository(inventoryFile);

        List<ProductionItem> productionQueue = new ArrayList<>();

        SampleService     sampleService     = new SampleService(sampleRepo, inventoryRepo);
        OrderService      orderService      = new OrderService(orderRepo, sampleRepo, inventoryRepo, productionQueue);
        ProductionService productionService = new ProductionService(productionQueue, orderRepo, inventoryRepo);
        MonitorService    monitorService    = new MonitorService(orderRepo, inventoryRepo, sampleRepo);

        ConsoleView view = new ConsoleView(new Scanner(System.in));

        SampleController     sampleCtrl      = new SampleController(sampleService, inventoryRepo, view);
        OrderController      orderCtrl       = new OrderController(orderService, orderRepo, sampleRepo, inventoryRepo, view);
        ProductionController productionCtrl  = new ProductionController(productionService, orderRepo, sampleRepo, view);
        MonitorController    monitorCtrl     = new MonitorController(monitorService, inventoryRepo, sampleRepo, view);
        SampleGenerator      sampleGenerator = new SampleGenerator(sampleRepo, inventoryRepo);
        OrderGenerator       orderGenerator  = new OrderGenerator(orderRepo, sampleRepo);
        DummyController      dummyCtrl       = new DummyController(sampleGenerator, orderGenerator, view);

        while (true) {
            long sampleCount = sampleRepo.findAll().size();
            long orderCount  = orderRepo.findAll().size();
            int  queueSize   = productionQueue.size();

            view.showMainMenu(sampleCount, orderCount, queueSize);
            String input = view.readLineRaw();

            if ("0".equals(input)) {
                view.showInfo("시스템을 종료합니다.");
                break;
            }

            try {
                switch (input) {
                    case "1" -> sampleCtrl.handle();
                    case "2" -> orderCtrl.handlePlace();
                    case "3" -> orderCtrl.handleApproveReject();
                    case "4" -> monitorCtrl.handle();
                    case "5" -> productionCtrl.handle();
                    case "6" -> orderCtrl.handleRelease();
                    case "d" -> dummyCtrl.handle();
                    default  -> view.showError("잘못된 메뉴 입력입니다.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                view.showError(e.getMessage());
            }
        }
    }
}
