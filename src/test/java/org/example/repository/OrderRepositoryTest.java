package org.example.repository;

import org.example.model.Order;
import org.example.model.OrderStatus;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrderRepository 테스트")
class OrderRepositoryTest {

    private File tempFile;
    private OrderRepository repo;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = Files.createTempFile("orders_", ".json").toFile();
        repo = new OrderRepository(tempFile);
    }

    @AfterEach
    void tearDown() {
        tempFile.delete();
    }

    @Test
    @DisplayName("save_주문저장_파일에기록됨")
    void save_order_savedToFile() {
        Order order = new Order("O001", "S001", "홍길동", 10, OrderStatus.RESERVED, "2026-06-12T10:00:00");
        repo.save(order);

        List<Order> all = repo.findAll();
        assertEquals(1, all.size());
        assertEquals("O001", all.get(0).getId());
    }

    @Test
    @DisplayName("findByStatus_RESERVED상태_해당주문만반환")
    void findByStatus_reserved_returnsOnlyReservedOrders() {
        repo.save(new Order("O001", "S001", "홍길동", 10, OrderStatus.RESERVED, "2026-06-12T10:00:00"));
        repo.save(new Order("O002", "S001", "김철수", 5, OrderStatus.CONFIRMED, "2026-06-12T11:00:00"));
        repo.save(new Order("O003", "S002", "이영희", 20, OrderStatus.RESERVED, "2026-06-12T12:00:00"));

        List<Order> reserved = repo.findByStatus(OrderStatus.RESERVED);
        assertEquals(2, reserved.size());
        assertTrue(reserved.stream().allMatch(o -> o.getStatus() == OrderStatus.RESERVED));
    }

    @Test
    @DisplayName("findBySampleId_특정시료ID_해당주문목록반환")
    void findBySampleId_specificId_returnsMatchingOrders() {
        repo.save(new Order("O001", "S001", "홍길동", 10, OrderStatus.RESERVED, "2026-06-12T10:00:00"));
        repo.save(new Order("O002", "S002", "김철수", 5, OrderStatus.RESERVED, "2026-06-12T11:00:00"));
        repo.save(new Order("O003", "S001", "이영희", 20, OrderStatus.CONFIRMED, "2026-06-12T12:00:00"));

        List<Order> result = repo.findBySampleId("S001");
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(o -> o.getSampleId().equals("S001")));
    }

    @Test
    @DisplayName("update_상태변경후업데이트_변경된상태반환")
    void update_statusChanged_updatedStatusReflected() {
        Order order = new Order("O001", "S001", "홍길동", 10, OrderStatus.RESERVED, "2026-06-12T10:00:00");
        repo.save(order);

        Order updated = order.withStatus(OrderStatus.CONFIRMED);
        repo.update(updated);

        Order found = repo.findById("O001").orElseThrow();
        assertEquals(OrderStatus.CONFIRMED, found.getStatus());
    }

    @Test
    @DisplayName("재시작후findAll_같은파일로새인스턴스생성_데이터복원됨")
    void findAll_afterRestartWithSameFile_restoresData() {
        repo.save(new Order("O001", "S001", "홍길동", 10, OrderStatus.RESERVED, "2026-06-12T10:00:00"));
        repo.save(new Order("O002", "S002", "김철수", 5, OrderStatus.CONFIRMED, "2026-06-12T11:00:00"));

        // 앱 재시작 시뮬레이션: 같은 파일로 새 Repository 인스턴스 생성
        OrderRepository restarted = new OrderRepository(tempFile);
        List<Order> restored = restarted.findAll();

        assertEquals(2, restored.size());
        assertTrue(restored.stream().anyMatch(o -> o.getId().equals("O001") && o.getStatus() == OrderStatus.RESERVED));
        assertTrue(restored.stream().anyMatch(o -> o.getId().equals("O002") && o.getStatus() == OrderStatus.CONFIRMED));
    }
}
