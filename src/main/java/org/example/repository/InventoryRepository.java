package org.example.repository;

import org.example.model.Inventory;
import org.example.util.JsonFileUtil;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class InventoryRepository implements CrudRepository<Inventory, String> {

    private final File file;

    public InventoryRepository(File file) {
        this.file = file;
    }

    @Override
    public void save(Inventory entity) {
        List<Inventory> list = JsonFileUtil.readList(file, Inventory.class);
        list.add(entity);
        JsonFileUtil.writeList(file, list);
    }

    @Override
    public Optional<Inventory> findById(String sampleId) {
        return JsonFileUtil.readList(file, Inventory.class).stream()
                .filter(inv -> inv.getSampleId().equals(sampleId))
                .findFirst();
    }

    @Override
    public List<Inventory> findAll() {
        return JsonFileUtil.readList(file, Inventory.class);
    }

    @Override
    public void update(Inventory entity) {
        List<Inventory> list = JsonFileUtil.readList(file, Inventory.class);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getSampleId().equals(entity.getSampleId())) {
                list.set(i, entity);
                break;
            }
        }
        JsonFileUtil.writeList(file, list);
    }

    @Override
    public void deleteById(String sampleId) {
        List<Inventory> list = JsonFileUtil.readList(file, Inventory.class);
        list.removeIf(inv -> inv.getSampleId().equals(sampleId));
        JsonFileUtil.writeList(file, list);
    }

    public Inventory findOrCreate(String sampleId) {
        return findById(sampleId).orElseGet(() -> {
            Inventory inv = new Inventory(sampleId, 0);
            save(inv);
            return inv;
        });
    }
}
