package org.example.repository;

import org.example.model.ProductionItem;
import org.example.util.JsonFileUtil;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class ProductionQueueRepository implements CrudRepository<ProductionItem, String> {

    private final File file;

    public ProductionQueueRepository(File file) {
        this.file = file;
    }

    @Override
    public void save(ProductionItem entity) {
        List<ProductionItem> list = JsonFileUtil.readList(file, ProductionItem.class);
        list.add(entity);
        JsonFileUtil.writeList(file, list);
    }

    @Override
    public Optional<ProductionItem> findById(String orderId) {
        return JsonFileUtil.readList(file, ProductionItem.class).stream()
                .filter(p -> p.getOrderId().equals(orderId))
                .findFirst();
    }

    @Override
    public List<ProductionItem> findAll() {
        return JsonFileUtil.readList(file, ProductionItem.class);
    }

    @Override
    public void update(ProductionItem entity) {
        List<ProductionItem> list = JsonFileUtil.readList(file, ProductionItem.class);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getOrderId().equals(entity.getOrderId())) {
                list.set(i, entity);
                break;
            }
        }
        JsonFileUtil.writeList(file, list);
    }

    @Override
    public void deleteById(String orderId) {
        List<ProductionItem> list = JsonFileUtil.readList(file, ProductionItem.class);
        list.removeIf(p -> p.getOrderId().equals(orderId));
        JsonFileUtil.writeList(file, list);
    }

    public boolean existsById(String orderId) {
        return findById(orderId).isPresent();
    }

    public void deleteAll() {
        JsonFileUtil.writeList(file, List.of());
    }
}
