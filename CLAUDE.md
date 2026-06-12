# CLAUDE.md — 반도체 시료 생산주문관리 시스템

## 프로젝트 개요

**목적**: 반도체 시료(Sample) 생산·공급 회사의 주문, 재고, 생산 현황을 통합 관리하는 콘솔 기반 시스템

**기술 스택**

| 항목 | 내용 |
|------|------|
| 언어 | Java 17 |
| 빌드 | Gradle (Wrapper 사용) |
| 직렬화 | Jackson Databind 2.18.x |
| 테스트 | JUnit Jupiter (junit-bom:6.0.0, build.gradle 고정) |
| 더미 데이터 | JavaFaker (한국 로케일) |

---

## 디렉터리 구조

```
src/
├── main/java/org/example/
│   ├── Main.java                          # 진입점, DI 조립
│   ├── model/
│   │   ├── Sample.java                    # 시료 엔티티
│   │   ├── Order.java                     # 주문 엔티티
│   │   ├── OrderStatus.java               # 주문 상태 Enum
│   │   ├── Inventory.java                 # 재고 엔티티
│   │   ├── InventoryStatus.java           # 재고 상태 Enum (SUFFICIENT/SHORTAGE/DEPLETED)
│   │   └── ProductionItem.java            # 생산 큐 항목
│   ├── repository/
│   │   ├── CrudRepository.java            # 제네릭 CRUD 인터페이스
│   │   ├── SampleRepository.java          # 시료 저장소 (JSON)
│   │   ├── OrderRepository.java           # 주문 저장소 (JSON)
│   │   └── InventoryRepository.java       # 재고 저장소 (JSON)
│   ├── service/
│   │   ├── SampleService.java             # 시료 비즈니스 로직
│   │   ├── OrderService.java              # 주문 비즈니스 로직
│   │   ├── ProductionService.java         # 생산 라인 로직
│   │   └── MonitorService.java            # 모니터링 집계
│   ├── controller/
│   │   ├── SampleController.java          # 시료 관리 컨트롤러
│   │   ├── OrderController.java           # 주문 처리 컨트롤러
│   │   ├── ProductionController.java      # 생산 라인 컨트롤러
│   │   ├── MonitorController.java         # 모니터링 컨트롤러
│   │   └── DummyController.java           # 더미 데이터 생성 컨트롤러
│   ├── view/
│   │   └── ConsoleView.java               # 단일 콘솔 I/O 담당 뷰
│   ├── util/
│   │   └── JsonFileUtil.java              # JSON 파일 읽기/쓰기 유틸
│   └── dummy/
│       ├── SampleGenerator.java           # 시료 더미 데이터 생성기
│       └── OrderGenerator.java            # 주문 더미 데이터 생성기
└── test/java/org/example/
    ├── service/
    │   ├── SampleServiceTest.java
    │   ├── OrderServiceTest.java
    │   ├── ProductionServiceTest.java
    │   └── MonitorServiceTest.java
    └── repository/
        ├── SampleRepositoryTest.java
        ├── OrderRepositoryTest.java
        └── InventoryRepositoryTest.java

data/                                      # JSON 영속성 파일 (프로젝트 루트, 런타임 읽기/쓰기)
├── samples.json
├── orders.json
└── inventory.json
```

---

## 빌드 및 실행 명령

```powershell
# 전체 빌드
.\gradlew build

# 테스트 실행
.\gradlew test

# 테스트 결과 확인
# build/reports/tests/test/index.html

# 애플리케이션 실행
.\gradlew run

# 클린 빌드
.\gradlew clean build
```

---

## 아키텍처 원칙 (PoC 기반)

### MVC 역할 분리 (ConsoleMVC PoC 준수)
- **View**: `System.out` 호출은 `ConsoleView`에서만 허용. 비즈니스 로직 포함 금지.
- **Controller**: Service/Repository와 View를 연결하는 조정 역할만 수행.
- **Model**: 불변(immutable) POJO. `final` 필드 + getter 전용.
  - Jackson 역직렬화를 위해 모든 모델 클래스는 `@JsonCreator` + `@JsonProperty`를 생성자에 적용한다.
  - 기본 생성자 또는 setter 추가 금지 — `final` 필드 불변성 유지.

### JSON 영속성 (DataPersistence PoC 준수)
- `CrudRepository<T, ID>` 제네릭 인터페이스로 추상화.
- `JsonFileUtil` 통해 파일 I/O 수행 (Jackson ObjectMapper, prettyPrint).
- 각 저장소는 읽기→수정→쓰기(read-modify-write) 패턴 사용.
- 애플리케이션 재실행 후에도 데이터 유지.
- JSON 파일 저장 경로: 프로젝트 루트 `data/` 디렉터리 (classpath 외부, 런타임 읽기/쓰기 가능).

### 모니터링 레이어 (DataMonitor PoC 준수)
- 4계층 구조: Model → Repository → Service → UI
- 상태별 필터링 및 집계는 `MonitorService`에서 Stream API로 처리.
- 콘솔 테이블 렌더링은 `ConsoleView`에서 담당.

### 더미 데이터 (DummyDataGenerator PoC 준수)
- JavaFaker(한국 로케일)로 현실적인 한국어 데이터 생성.
- `SampleGenerator`, `OrderGenerator` 분리.
- 생성된 데이터는 JSON 저장소에 직접 저장.

---

## 핵심 비즈니스 규칙

### 주문 상태 흐름
```
RESERVED → CONFIRMED  (재고 충분 시 승인)
RESERVED → PRODUCING  (재고 부족 시 생산 라인 등록 후 승인)
RESERVED → REJECTED   (거절)
PRODUCING → CONFIRMED (생산 완료)
CONFIRMED → RELEASE   (출고 처리)
```

### 생산량 계산식
```java
int actualProduction = (int) Math.ceil(shortage / (yield * 0.9));
long totalProductionTime = avgProductionTime * actualProduction;
```

### 수율(yield) 정의
```
yield = 정상 시료 수량 / 총 생산 수량
예: 100개 생산 → 정상 90개 → yield = 0.9
```

---

## 코딩 컨벤션

### 네이밍
- 클래스: PascalCase (`SampleService`)
- 메서드/변수: camelCase (`findById`, `orderStatus`)
- 상수/Enum 값: UPPER_SNAKE_CASE (`ORDER_NOT_FOUND`)
- 테스트 메서드: `메서드명_상황_기대결과` (`approve_whenStockSufficient_setsConfirmed`)

### 패키지
- 루트: `org.example`
- 하위 패키지: `model`, `repository`, `service`, `controller`, `view`, `util`, `dummy`

### 테스트
- Mock 사용 금지 — 실제 임시 파일 또는 인메모리 인스턴스 사용.
- `@BeforeEach` / `@AfterEach`로 테스트 데이터 초기화·정리.
- `@DisplayName`으로 한국어 테스트 설명 필수.

---

## 제약 조건

| 항목 | 내용 |
|------|------|
| 변경 금지 파일 | `settings.gradle`, `.gitignore`, `gradlew`, `gradlew.bat` |
| 외부 라이브러리 | Jackson Databind, JavaFaker만 추가 허용 |
| Spring / Quarkus | 사용 금지 |
| Mockito | 사용 금지 |
| System.out | ConsoleView 외부에서 호출 금지 |
| 데이터 저장 | JSON 파일 방식 (프로젝트 루트 `data/` 디렉터리) |

---

## Verify Harness 워크플로

이 프로젝트는 워크스페이스의 Verify Harness를 따릅니다.

```
SubAgent1 (문서 정합성 검증 — document-consistency-verifier)
    ↓ PASS
SubAgent2 (코드 생성 — ai-action)
    ↓
SubAgent3 (Test Verify) ‖ SubAgent4 (Compliance Verify)
    ↓ 둘 다 PASS
코드 리뷰
```
