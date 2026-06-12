# PRD.md — 반도체 시료 생산주문관리 시스템

**문서 버전**: 1.1  
**작성일**: 2026-06-12  
**프로젝트명**: SampleOrderSystem-PARKCHIWON-17061288

---

## 1. 배경 및 목적

반도체 시료(Sample)를 생산·공급하는 회사에서 주문 증가로 인해 기존 엑셀/메모장 기반 관리의 한계에 도달했다. 본 시스템은 시료 등록, 주문 처리, 생산 현황, 재고 관리를 통합하는 콘솔 기반 관리 시스템을 구축하여 업무 효율성을 높이는 것을 목적으로 한다.

---

## 2. 사용자 역할

| 역할 | 주요 업무 |
|------|-----------|
| 고객 | 시료 주문 요청 |
| 주문 담당자 | 주문서 작성, 주문 관리 |
| 생산 담당자 | 주문 승인/거절, 시료 생산 관리 |

---

## 3. 시스템 범위

- 콘솔(Console) 기반 명령 입력 방식
- Java 17 + Gradle 기반 단독 실행 애플리케이션
- JSON 파일 기반 데이터 영속성 (재실행 후 데이터 유지)
- MVC 아키텍처 패턴 적용

---

## 4. 도메인 모델

### 4.1 시료 (Sample)

| 필드 | 타입 | 설명 |
|------|------|------|
| id | String | 시료 고유 식별자 (사용자 입력 임의 문자열) |
| name | String | 시료 이름 |
| avgProductionTime | long | 평균 생산시간 (분 단위) |
| yield | double | 수율 (0.0 ~ 1.0) |

**수율 정의**: `yield = 정상 시료 수량 / 총 생산 수량`  
예) 100개 생산 → 정상 90개 → yield = 0.9

### 4.2 주문 (Order)

| 필드 | 타입 | 설명 |
|------|------|------|
| id | String | 주문 고유 식별자 (UUID) |
| sampleId | String | 주문 시료 ID |
| customerName | String | 고객명 |
| quantity | int | 주문 수량 |
| status | OrderStatus | 주문 상태 |
| createdAt | String | 주문 생성 일시 |

### 4.3 재고 (Inventory)

| 필드 | 타입 | 설명 |
|------|------|------|
| sampleId | String | 시료 ID |
| stock | int | 현재 재고 수량 |

### 4.4 재고 상태 (InventoryStatus)

재고 현황 모니터링에서 시료별 재고 상태를 표현하는 Enum이다.

| 값 | 조건 | 설명 |
|----|------|------|
| `SUFFICIENT` | `stock >= pendingDemand` | 재고 여유 |
| `SHORTAGE` | `stock > 0 && stock < pendingDemand` | 재고 부족 |
| `DEPLETED` | `stock == 0` | 재고 고갈 |

- `pendingDemand` = 해당 시료에 대한 `RESERVED` 상태 주문량 합계 + `PRODUCING` 상태 주문량 합계

---

### 4.5 생산 큐 항목 (ProductionItem)

| 필드 | 타입 | 설명 |
|------|------|------|
| orderId | String | 주문 ID |
| sampleId | String | 시료 ID |
| requiredQuantity | int | 필요 생산 수량 |
| actualProduction | int | 실 생산량 |
| totalProductionTime | long | 총 생산 시간 (분) |
| enqueuedAt | String | 큐 등록 일시 |

### 4.6 주문 상태 (OrderStatus)

```
RESERVED  → 주문 접수 (초기 상태)
REJECTED  → 주문 거절
PRODUCING → 생산 중 (재고 부족으로 생산 라인 등록)
CONFIRMED → 출고 대기 (승인 완료)
RELEASE   → 출고 완료
```

**상태 전이 규칙**

```
RESERVED ──[승인·재고 충분]──→ CONFIRMED
RESERVED ──[승인·재고 부족]──→ PRODUCING → CONFIRMED
RESERVED ──[거절]────────────→ REJECTED
CONFIRMED ──[출고]───────────→ RELEASE
```

- `REJECTED`는 정상 흐름에서 제외되며 모니터링 집계 대상에서도 제외된다.

---

## 5. 기능 요구사항 (FR)

### FR-1: 시료 관리

#### FR-1-1: 시료 등록
- **메서드**: `SampleService.register(String id, String name, long avgProductionTime, double yield)`
- 입력: 시료 ID, 이름, 평균 생산시간, 수율
- 중복 ID 등록 시 예외 발생 (`IllegalArgumentException`)
- 등록된 시료 데이터를 프로젝트 루트 `data/samples.json`에 즉시 저장

#### FR-1-2: 시료 목록 조회
- **메서드**: `SampleService.findAll()`
- 등록된 모든 시료 목록과 각 시료의 현재 재고 수량 반환

#### FR-1-3: 시료 검색
- **메서드**: `SampleService.search(String keyword)`
- 시료 이름 기반 부분 문자열 검색 (대소문자 무시)

---

### FR-2: 주문 처리

#### FR-2-1: 주문 접수
- **메서드**: `OrderService.placeOrder(String sampleId, String customerName, int quantity)`
- 등록되지 않은 시료 ID 입력 시 예외 발생
- 주문 생성 시 상태: `RESERVED`
- 주문 데이터를 프로젝트 루트 `data/orders.json`에 즉시 저장

#### FR-2-2: 주문 승인
- **메서드**: `OrderService.approve(String orderId)`
- 대상: `RESERVED` 상태 주문만 처리
- 재고 충분 (stock >= quantity):
  - 재고에서 주문 수량 차감
  - 주문 상태 → `CONFIRMED`
- 재고 부족 (stock < quantity):
  - 부족 수량 계산: `shortage = quantity - stock`
  - 실 생산량 계산: `actualProduction = ceil(shortage / (yield * 0.9))`
  - 총 생산시간 계산: `totalProductionTime = avgProductionTime * actualProduction`
  - 생산 라인 큐에 `ProductionItem` 등록 (FIFO)
  - 주문 상태 → `PRODUCING`

#### FR-2-3: 주문 거절
- **메서드**: `OrderService.reject(String orderId)`
- 대상: `RESERVED` 상태 주문만 처리
- 주문 상태 → `REJECTED`

---

### FR-3: 모니터링

#### FR-3-1: 상태별 주문 수 확인
- **메서드**: `MonitorService.getOrderCountByStatus()`
- 집계 대상 상태: `RESERVED`, `CONFIRMED`, `PRODUCING`, `RELEASE`
- `REJECTED` 제외
- 상태별 주문 건수를 Map으로 반환

#### FR-3-2: 시료별 재고 현황 확인
- **메서드**: `MonitorService.getInventoryStatus()`
- 반환 타입: `Map<String, InventoryStatus>` (sampleId → 상태)
- 시료별 현재 재고 수량과 `InventoryStatus` 반환
- 재고 상태 판정 조건 (`pendingDemand` = 해당 시료의 RESERVED 주문량 합 + PRODUCING 주문량 합):
  - **SUFFICIENT**: `stock >= pendingDemand` (재고 여유)
  - **SHORTAGE**: `stock > 0 && stock < pendingDemand` (재고 부족)
  - **DEPLETED**: `stock == 0` (재고 고갈)

---

### FR-4: 출고 처리

#### FR-4-1: 주문 출고
- **메서드**: `OrderService.release(String orderId)`
- 대상: `CONFIRMED` 상태 주문만 처리
- 주문 상태 → `RELEASE`

---

### FR-5: 생산 라인

#### FR-5-1: 생산 중인 시료 확인
- **메서드**: `ProductionService.getActiveProductions()`
- `PRODUCING` 상태 주문 목록과 생산 현황(실 생산량, 총 생산시간) 반환

#### FR-5-2: 생산 대기 큐 확인
- **메서드**: `ProductionService.getQueueStatus()`
- 생산 대기 중인 `ProductionItem` 목록을 FIFO 순서로 반환

#### FR-5-3: 생산 완료 처리
- **메서드**: `ProductionService.completeProduction(String orderId)`
- 생산 완료 시 재고 수량 업데이트: `재고 += actualProduction` (생산된 실 수량을 재고에 추가)
- 재고 추가 후 해당 주문의 수량(`quantity`)을 재고에서 차감하여 출고 대기 상태로 전환
- `PRODUCING` → `CONFIRMED` 상태 전이
- 해당 `ProductionItem`을 생산 큐에서 제거

---

### FR-6: 더미 데이터 생성

#### FR-6-1: 시료 더미 데이터 생성
- **클래스**: `SampleGenerator`
- JavaFaker(한국 로케일)를 사용하여 현실적인 시료 데이터 생성
- 생성된 데이터는 프로젝트 루트 `data/samples.json`에 저장

#### FR-6-2: 주문 더미 데이터 생성
- **클래스**: `OrderGenerator`
- 등록된 시료 ID를 참조하여 주문 데이터 생성
- 생성된 데이터는 프로젝트 루트 `data/orders.json`에 저장

---

## 6. 비기능 요구사항 (NFR)

### NFR-1: 데이터 영속성
- 애플리케이션 재실행 후에도 모든 데이터(시료, 주문, 재고) 유지
- 파일 경로: 프로젝트 루트 `data/samples.json`, `data/orders.json`, `data/inventory.json`
- Jackson ObjectMapper `prettyPrint` 설정으로 가독성 있는 JSON 저장

### NFR-2: 테스트 커버리지
- 각 Service 클래스 핵심 메서드에 대한 단위 테스트 필수
- 각 Repository 클래스에 대한 CRUD 테스트 필수
- Mock 사용 금지 — 실제 임시 파일 또는 인메모리 인스턴스 사용
- `@DisplayName` 한국어 설명 필수

### NFR-3: 아키텍처
- MVC 패턴 엄격 준수
- `System.out` 호출은 `ConsoleView` 클래스에서만 허용
- 패키지 구조: `model`, `repository`, `service`, `controller`, `view`, `util`, `dummy`

### NFR-4: 코드 품질
- Clean Code 원칙 준수
- 메서드는 단일 책임 원칙(SRP) 준수
- 비즈니스 로직 설명이 필요할 경우에만 주석 작성

---

## 7. 메인 메뉴 구성

```
========================================
  반도체 시료 생산주문관리 시스템
========================================
1. 시료 관리
   1-1. 새로운 시료 등록
   1-2. 시료 목록 조회
   1-3. 시료 검색
2. 주문 처리
   2-1. 고객 주문 접수
   2-2. 주문 승인
   2-3. 주문 거절
3. 모니터링
   3-1. 상태별 주문 수 확인
   3-2. 시료별 재고 현황 확인
4. 출고 처리
   4-1. CONFIRMED 주문 출고
5. 생산 라인
   5-1. 생산 중인 시료 확인
   5-2. 생산 대기 큐 확인
   5-3. 생산 완료 처리
6. 더미 데이터 생성
   6-1. 시료 더미 데이터 생성
   6-2. 주문 더미 데이터 생성
0. 종료
```

---

## 8. PoC 참조

| PoC | 저장소 | 적용 내용 |
|-----|--------|-----------|
| MVC 스켈레톤 | ConsoleMVC-PARKCHIWON-17061288 | 패키지 구조, 역할 분리 원칙, 테스트 패턴 |
| 데이터 영속성 | DataPersistence-PARKCHIWON-17060288 | `CrudRepository<T,ID>`, `JsonFileUtil`, Jackson 설정 |
| 데이터 모니터링 | DataMonitor-PARKCHIWON-17061288 | 4계층 구조, 상태 집계, 콘솔 테이블 렌더링 |
| 더미 데이터 생성 | DummyDataGenerator-PARKCHIWON-17061288 | JavaFaker, 엔티티별 Generator 분리 |

---

## 9. 비고 및 가정

- 동시성(멀티스레드) 처리는 이번 버전에서 제외 (단일 스레드 동기 실행)
- 생산 완료는 수동 처리 (`completeProduction` 명령으로 완료 처리)
- 주문 ID는 UUID 자동 생성, 시료 ID는 사용자가 직접 입력하는 임의 문자열 (UUID 형식 불필요)
- `REJECTED` 상태 주문은 조회는 가능하나 모니터링 집계에서 제외
