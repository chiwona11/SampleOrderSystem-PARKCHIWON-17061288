package org.example.repository;

import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.util.JsonFileUtil;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OrderRepository implements CrudRepository<Order, String> {

    private final File file;

    public OrderRepository(File file) {
        this.file = file;
    }

    @Override
    public void save(Order entity) {
        List<Order> list = JsonFileUtil.readList(file, Order.class);
        list.add(entity);
        JsonFileUtil.writeList(file, list);
    }

    @Override
    public Optional<Order> findById(String id) {
        return JsonFileUtil.readList(file, Order.class).stream()
                .filter(o -> o.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<Order> findAll() {
        return JsonFileUtil.readList(file, Order.class);
    }

    @Override
    public void update(Order entity) {
        List<Order> list = JsonFileUtil.readList(file, Order.class);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(entity.getId())) {
                list.set(i, entity);
                break;
            }
        }
        JsonFileUtil.writeList(file, list);
    }

    @Override
    public void deleteById(String id) {
        List<Order> list = JsonFileUtil.readList(file, Order.class);
        list.removeIf(o -> o.getId().equals(id));
        JsonFileUtil.writeList(file, list);
    }

    public List<Order> findByStatus(OrderStatus status) {
        return JsonFileUtil.readList(file, Order.class).stream()
                .filter(o -> o.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<Order> findBySampleId(String sampleId) {
        return JsonFileUtil.readList(file, Order.class).stream()
                .filter(o -> o.getSampleId().equals(sampleId))
                .collect(Collectors.toList());
    }
}
