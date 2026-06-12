package org.example.service;

import org.example.model.Inventory;
import org.example.model.InventoryStatus;
import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.Sample;
import org.example.repository.InventoryRepository;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MonitorService {

    private final OrderRepository orderRepo;
    private final InventoryRepository inventoryRepo;
    private final SampleRepository sampleRepo;

    public MonitorService(OrderRepository orderRepo, InventoryRepository inventoryRepo,
                          SampleRepository sampleRepo) {
        this.orderRepo = orderRepo;
        this.inventoryRepo = inventoryRepo;
        this.sampleRepo = sampleRepo;
    }

    public Map<OrderStatus, Long> getOrderCountByStatus() {
        return orderRepo.findAll().stream()
                .filter(o -> o.getStatus() != OrderStatus.REJECTED)
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
    }

    public Map<String, InventoryStatus> getInventoryStatus() {
        List<Sample> samples = sampleRepo.findAll();
        List<Order> allOrders = orderRepo.findAll();

        return samples.stream().collect(Collectors.toMap(
                Sample::getId,
                sample -> {
                    String sampleId = sample.getId();
                    Inventory inventory = inventoryRepo.findOrCreate(sampleId);
                    int stock = inventory.getStock();

                    int pendingDemand = allOrders.stream()
                            .filter(o -> o.getSampleId().equals(sampleId))
                            .filter(o -> o.getStatus() == OrderStatus.RESERVED
                                    || o.getStatus() == OrderStatus.PRODUCING)
                            .mapToInt(Order::getQuantity)
                            .sum();

                    if (stock == 0) {
                        return InventoryStatus.DEPLETED;
                    } else if (stock < pendingDemand) {
                        return InventoryStatus.SHORTAGE;
                    } else {
                        return InventoryStatus.SUFFICIENT;
                    }
                }
        ));
    }
}
