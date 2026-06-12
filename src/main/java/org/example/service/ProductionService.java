package org.example.service;

import org.example.model.Inventory;
import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.ProductionItem;
import org.example.repository.InventoryRepository;
import org.example.repository.OrderRepository;

import java.util.List;

public class ProductionService {

    private final List<ProductionItem> productionQueue;
    private final OrderRepository orderRepo;
    private final InventoryRepository inventoryRepo;

    public ProductionService(List<ProductionItem> productionQueue,
                             OrderRepository orderRepo, InventoryRepository inventoryRepo) {
        this.productionQueue = productionQueue;
        this.orderRepo = orderRepo;
        this.inventoryRepo = inventoryRepo;
    }

    public List<Order> getActiveProductions() {
        return orderRepo.findByStatus(OrderStatus.PRODUCING);
    }

    public List<ProductionItem> getQueueStatus() {
        return List.copyOf(productionQueue);
    }

    public Order completeProduction(String orderId) {
        ProductionItem item = productionQueue.stream()
                .filter(p -> p.getOrderId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("생산 큐에 등록되지 않은 주문 ID입니다: " + orderId));

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문 ID입니다: " + orderId));

        Inventory inventory = inventoryRepo.findOrCreate(order.getSampleId());
        int newStock = Math.max(0, inventory.getStock() + item.getActualProduction() - order.getQuantity());
        inventoryRepo.update(inventory.withStock(newStock));

        Order confirmed = order.withStatus(OrderStatus.CONFIRMED);
        orderRepo.update(confirmed);

        productionQueue.remove(item);
        return confirmed;
    }
}
