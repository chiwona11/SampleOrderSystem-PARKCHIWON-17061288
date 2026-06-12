package org.example.service;

import org.example.model.Inventory;
import org.example.model.Sample;
import org.example.repository.InventoryRepository;
import org.example.repository.SampleRepository;

import java.util.List;

public class SampleService {

    private final SampleRepository sampleRepo;
    private final InventoryRepository inventoryRepo;

    public SampleService(SampleRepository sampleRepo, InventoryRepository inventoryRepo) {
        this.sampleRepo = sampleRepo;
        this.inventoryRepo = inventoryRepo;
    }

    public Sample register(String id, String name, long avgProductionTime, double yield) {
        if (sampleRepo.existsById(id)) {
            throw new IllegalArgumentException("이미 존재하는 시료 ID입니다: " + id);
        }
        Sample sample = new Sample(id, name, avgProductionTime, yield);
        sampleRepo.save(sample);
        inventoryRepo.save(new Inventory(id, 0));
        return sample;
    }

    public List<Sample> findAll() {
        return sampleRepo.findAll();
    }

    public List<Sample> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        return sampleRepo.findAll().stream()
                .filter(s -> s.getName().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
    }
}
