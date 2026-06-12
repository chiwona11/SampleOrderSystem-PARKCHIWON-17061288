package org.example.dummy;

import com.github.javafaker.Faker;
import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.Sample;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class OrderGenerator {

    private final OrderRepository orderRepo;
    private final SampleRepository sampleRepo;

    public OrderGenerator(OrderRepository orderRepo, SampleRepository sampleRepo) {
        this.orderRepo = orderRepo;
        this.sampleRepo = sampleRepo;
    }

    public void generate(int count) {
        List<Sample> samples = sampleRepo.findAll();
        if (samples.isEmpty()) {
            throw new IllegalStateException("시료 데이터가 없습니다. 먼저 시료 더미 데이터를 생성하세요.");
        }
        Faker faker = new Faker(new Locale("ko"));
        String createdAt = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        for (int i = 0; i < count; i++) {
            String id = UUID.randomUUID().toString();
            String sampleId = samples.get(faker.number().numberBetween(0, samples.size())).getId();
            String customerName = faker.name().fullName();
            int quantity = faker.number().numberBetween(1, 101);
            Order order = new Order(id, sampleId, customerName, quantity, OrderStatus.RESERVED, createdAt);
            orderRepo.save(order);
        }
    }
}
