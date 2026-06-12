package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.model.*;
import org.example.util.JsonFileUtil;

import java.io.File;
import java.util.List;

/**
 * Phase 1 Safety Test — 직렬화/역직렬화, withStatus(), withStock(), JsonFileUtil.readList() 검증
 * 프로덕션 코드를 변경하지 않고 동작 검증만 수행한다.
 */
public class SafetyTest {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Phase 1 Safety Test ===\n");

        testSampleSerializeDeserialize();
        testOrderWithStatus();
        testInventoryWithStock();
        testJsonFileUtilReadListMissingFile();
        testJsonFileUtilReadListEmptyFile();

        System.out.println("\n=== 결과: PASSED=" + passed + " / FAILED=" + failed + " ===");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ---------- TC-1: Sample 직렬화 → 역직렬화 ----------
    static void testSampleSerializeDeserialize() throws Exception {
        Sample original = new Sample("S001", "Alpha-7", 3600L, 0.95);
        String json = MAPPER.writeValueAsString(original);
        Sample restored = MAPPER.readValue(json, Sample.class);

        check("TC-1 Sample.id 복원",            "S001".equals(restored.getId()));
        check("TC-1 Sample.name 복원",           "Alpha-7".equals(restored.getName()));
        check("TC-1 Sample.avgProductionTime 복원", 3600L == restored.getAvgProductionTime());
        check("TC-1 Sample.yield 복원",          0.95 == restored.getYield());
    }

    // ---------- TC-2: Order.withStatus() ----------
    static void testOrderWithStatus() {
        Order original = new Order("O001", "S001", "홍길동", 10,
                OrderStatus.RESERVED, "2026-06-12T10:00:00");

        Order changed = original.withStatus(OrderStatus.PRODUCING);

        check("TC-2 withStatus() 새 인스턴스 반환",  original != changed);
        check("TC-2 원본 status 유지 (RESERVED)",    original.getStatus() == OrderStatus.RESERVED);
        check("TC-2 변경본 status = PRODUCING",       changed.getStatus() == OrderStatus.PRODUCING);
        check("TC-2 나머지 필드 동일 (id)",            "O001".equals(changed.getId()));
        check("TC-2 나머지 필드 동일 (quantity)",      10 == changed.getQuantity());
    }

    // ---------- TC-3: Inventory.withStock() ----------
    static void testInventoryWithStock() {
        Inventory original = new Inventory("S001", 50);
        Inventory changed  = original.withStock(30);

        check("TC-3 withStock() 새 인스턴스 반환", original != changed);
        check("TC-3 원본 stock 유지 (50)",          50 == original.getStock());
        check("TC-3 변경본 stock = 30",              30 == changed.getStock());
        check("TC-3 sampleId 동일",                  "S001".equals(changed.getSampleId()));
    }

    // ---------- TC-4: JsonFileUtil.readList() — 파일 없을 때 빈 리스트 ----------
    static void testJsonFileUtilReadListMissingFile() {
        File missing = new File(System.getProperty("java.io.tmpdir"),
                "safety_test_nonexistent_" + System.nanoTime() + ".json");

        List<Sample> result = JsonFileUtil.readList(missing, Sample.class);
        check("TC-4 파일 없을 때 빈 리스트 반환",    result != null && result.isEmpty());
    }

    // ---------- TC-5: JsonFileUtil.readList() — 빈 파일일 때 빈 리스트 ----------
    static void testJsonFileUtilReadListEmptyFile() throws Exception {
        File empty = File.createTempFile("safety_test_empty_", ".json");
        empty.deleteOnExit();
        // 파일은 생성됐지만 길이 0

        List<Sample> result = JsonFileUtil.readList(empty, Sample.class);
        check("TC-5 빈 파일일 때 빈 리스트 반환",    result != null && result.isEmpty());
    }

    // ---------- helper ----------
    static void check(String name, boolean condition) {
        if (condition) {
            System.out.println("  PASS: " + name);
            passed++;
        } else {
            System.out.println("  FAIL: " + name);
            failed++;
        }
    }
}
