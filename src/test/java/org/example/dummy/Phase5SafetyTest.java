package org.example.dummy;

import org.example.model.Inventory;
import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.Sample;
import org.example.repository.InventoryRepository;
import org.example.repository.OrderRepository;
import org.example.repository.SampleRepository;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase5 Safety Test — SampleGenerator / OrderGenerator")
class Phase5SafetyTest {

    private File samplesFile;
    private File ordersFile;
    private File inventoryFile;
    private SampleRepository sampleRepo;
    private OrderRepository orderRepo;
    private InventoryRepository inventoryRepo;

    @BeforeEach
    void setUp() throws IOException {
        samplesFile   = Files.createTempFile("p5_samples_",   ".json").toFile();
        ordersFile    = Files.createTempFile("p5_orders_",    ".json").toFile();
        inventoryFile = Files.createTempFile("p5_inventory_", ".json").toFile();

        sampleRepo    = new SampleRepository(samplesFile);
        orderRepo     = new OrderRepository(ordersFile);
        inventoryRepo = new InventoryRepository(inventoryFile);
    }

    @AfterEach
    void tearDown() {
        samplesFile.delete();
        ordersFile.delete();
        inventoryFile.delete();
    }

    // -----------------------------------------------------------------------
    // TC-P5-1: SampleGenerator.generate(3) 호출 시 samples.json에 3개 시료 추가
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("SampleGenerator_generate_3_시료3개저장됨")
    void sampleGenerator_generate3_saves3Samples() {
        SampleGenerator gen = new SampleGenerator(sampleRepo, inventoryRepo);
        gen.generate(3);

        List<Sample> samples = sampleRepo.findAll();
        assertEquals(3, samples.size(), "시료 3개가 저장되어야 한다");
    }

    // -----------------------------------------------------------------------
    // TC-P5-2: SampleGenerator.generate(3) 시 각 시료에 재고 0이 초기화됨
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("SampleGenerator_generate_재고0으로초기화됨")
    void sampleGenerator_generate_inventoryInitializedWithZero() {
        SampleGenerator gen = new SampleGenerator(sampleRepo, inventoryRepo);
        gen.generate(3);

        List<Sample> samples = sampleRepo.findAll();
        for (Sample s : samples) {
            Inventory inv = inventoryRepo.findById(s.getId()).orElseThrow(
                    () -> new AssertionError("재고 항목 없음: " + s.getId()));
            assertEquals(0, inv.getStock(), "초기 재고는 0이어야 한다: " + s.getId());
        }
    }

    // -----------------------------------------------------------------------
    // TC-P5-3: OrderGenerator.generate(2) 호출 시 orders.json에 2개 주문 추가
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("OrderGenerator_generate_2_주문2개저장됨")
    void orderGenerator_generate2_saves2Orders() {
        // 사전 시료 데이터 필요
        SampleGenerator sampleGen = new SampleGenerator(sampleRepo, inventoryRepo);
        sampleGen.generate(2);

        OrderGenerator orderGen = new OrderGenerator(orderRepo, sampleRepo);
        orderGen.generate(2);

        List<Order> orders = orderRepo.findAll();
        assertEquals(2, orders.size(), "주문 2개가 저장되어야 한다");
    }

    // -----------------------------------------------------------------------
    // TC-P5-4: OrderGenerator.generate(n) 시 시료 없으면 IllegalStateException
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("OrderGenerator_generate_시료없으면_IllegalStateException")
    void orderGenerator_generate_noSamples_throwsIllegalStateException() {
        // 시료를 저장하지 않은 상태
        OrderGenerator orderGen = new OrderGenerator(orderRepo, sampleRepo);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> orderGen.generate(1),
                "시료가 없을 때 IllegalStateException이 발생해야 한다"
        );
        assertNotNull(ex.getMessage());
        assertFalse(ex.getMessage().isBlank(), "예외 메시지가 비어있으면 안 된다");
    }

    // -----------------------------------------------------------------------
    // TC-P5-5: 생성된 주문의 초기 상태는 RESERVED
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("OrderGenerator_generate_주문초기상태_RESERVED")
    void orderGenerator_generate_initialStatus_isReserved() {
        SampleGenerator sampleGen = new SampleGenerator(sampleRepo, inventoryRepo);
        sampleGen.generate(1);

        OrderGenerator orderGen = new OrderGenerator(orderRepo, sampleRepo);
        orderGen.generate(3);

        List<Order> orders = orderRepo.findAll();
        for (Order o : orders) {
            assertEquals(OrderStatus.RESERVED, o.getStatus(),
                    "생성된 주문의 초기 상태는 RESERVED여야 한다");
        }
    }

    // -----------------------------------------------------------------------
    // TC-P5-6: SampleGenerator 경계값 — count=0이면 아무것도 저장 안 됨
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("SampleGenerator_generate_0_빈목록")
    void sampleGenerator_generate0_savesNothing() {
        SampleGenerator gen = new SampleGenerator(sampleRepo, inventoryRepo);
        gen.generate(0);

        List<Sample> samples = sampleRepo.findAll();
        assertTrue(samples.isEmpty(), "count=0이면 시료가 저장되지 않아야 한다");
    }
}
