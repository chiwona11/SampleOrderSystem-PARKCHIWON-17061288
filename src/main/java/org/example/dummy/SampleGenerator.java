package org.example.dummy;

import com.github.javafaker.Faker;
import org.example.model.Inventory;
import org.example.model.Sample;
import org.example.repository.InventoryRepository;
import org.example.repository.SampleRepository;

import java.util.List;
import java.util.Locale;

public class SampleGenerator {

    private static final List<String> SAMPLE_NAMES = List.of(
            "실리콘 웨이퍼 12인치",
            "실리콘 웨이퍼 8인치",
            "NPW 12인치",
            "NPW 8인치",
            "테스트 웨이퍼 12인치",
            "에피택셜 웨이퍼 12인치",
            "SOI 웨이퍼 12인치",
            "폴리실리콘 웨이퍼 12인치",
            "더미 웨이퍼 12인치",
            "모니터 웨이퍼 8인치"
    );

    private final SampleRepository sampleRepo;
    private final InventoryRepository inventoryRepo;

    public SampleGenerator(SampleRepository sampleRepo, InventoryRepository inventoryRepo) {
        this.sampleRepo = sampleRepo;
        this.inventoryRepo = inventoryRepo;
    }

    public void generate(int count) {
        Faker faker = new Faker(new Locale("ko"));
        for (int i = 0; i < count; i++) {
            String id = "S-" + System.currentTimeMillis() + "-" + i;
            if (sampleRepo.existsById(id)) {
                continue;
            }
            String name = SAMPLE_NAMES.get(faker.number().numberBetween(0, SAMPLE_NAMES.size()));
            long avgProductionTime = 10L + (long)(faker.number().numberBetween(0, 48)) * 10;
            double yield = Math.round((0.70 + faker.number().randomDouble(2, 0, 29) / 100.0) * 100.0) / 100.0;
            Sample sample = new Sample(id, name, avgProductionTime, yield);
            sampleRepo.save(sample);
            inventoryRepo.save(new Inventory(id, 0));
        }
    }
}
