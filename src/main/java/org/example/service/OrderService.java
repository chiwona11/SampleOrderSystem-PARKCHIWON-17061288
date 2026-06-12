package org.example.service;

import org.example.model.Inventory;
import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.ProductionItem;
import org.example.model.Sample;
import org.example.repository.InventoryRepository;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class OrderService {

    private final OrderRepository orderRepo;
    private final SampleRepository sampleRepo;
    private final InventoryRepository inventoryRepo;
    private final List<ProductionItem> productionQueue;

    public OrderService(OrderRepository orderRepo, SampleRepository sampleRepo,
                        InventoryRepository inventoryRepo, List<ProductionItem> productionQueue) {
        this.orderRepo = orderRepo;
        this.sampleRepo = sampleRepo;
        this.inventoryRepo = inventoryRepo;
        this.productionQueue = productionQueue;
    }

    public Order placeOrder(String sampleId, String customerName, int quantity) {
        if (!sampleRepo.existsById(sampleId)) {
            throw new IllegalArgumentException("등록되지 않은 시료 ID입니다: " + sampleId);
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("주문 수량은 1 이상이어야 합니다.");
        }
        Order order = new Order(
                UUID.randomUUID().toString(),
                sampleId,
                customerName,
                quantity,
                OrderStatus.RESERVED,
                LocalDateTime.now().toString()
        );
        orderRepo.save(order);
        return order;
    }

    public Order approve(String orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문 ID입니다: " + orderId));
        if (order.getStatus() != OrderStatus.RESERVED) {
            throw new IllegalStateException("RESERVED 상태인 주문만 승인할 수 있습니다. 현재 상태: " + order.getStatus());
        }
        Sample sample = sampleRepo.findById(order.getSampleId())
                .orElseThrow(() -> new IllegalArgumentException("시료를 찾을 수 없습니다: " + order.getSampleId()));
        Inventory inventory = inventoryRepo.findOrCreate(order.getSampleId());

        if (inventory.getStock() >= order.getQuantity()) {
            int newStock = inventory.getStock() - order.getQuantity();
            inventoryRepo.update(inventory.withStock(newStock));
            Order confirmed = order.withStatus(OrderStatus.CONFIRMED);
            orderRepo.update(confirmed);
            return confirmed;
        } else {
            int shortage = order.getQuantity() - inventory.getStock();
            int actualProduction = (int) Math.ceil(shortage / (sample.getYield() * 0.9));
            long totalProductionTime = sample.getAvgProductionTime() * actualProduction;
            ProductionItem item = new ProductionItem(
                    order.getId(),
                    order.getSampleId(),
                    shortage,
                    actualProduction,
                    totalProductionTime,
                    LocalDateTime.now().toString()
            );
            productionQueue.add(item);
            Order producing = order.withStatus(OrderStatus.PRODUCING);
            orderRepo.update(producing);
            return producing;
        }
    }

    public Order reject(String orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문 ID입니다: " + orderId));
        if (order.getStatus() != OrderStatus.RESERVED) {
            throw new IllegalStateException("RESERVED 상태인 주문만 거절할 수 있습니다. 현재 상태: " + order.getStatus());
        }
        Order rejected = order.withStatus(OrderStatus.REJECTED);
        orderRepo.update(rejected);
        return rejected;
    }

    public Order release(String orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문 ID입니다: " + orderId));
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("CONFIRMED 상태인 주문만 출고할 수 있습니다. 현재 상태: " + order.getStatus());
        }
        Order released = order.withStatus(OrderStatus.RELEASE);
        orderRepo.update(released);
        return released;
    }

    public List<Order> findAll() {
        return orderRepo.findAll();
    }
}
