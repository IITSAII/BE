# 제휴 업체 랜덤 배정 로직

결제가 승인된 세션에 제휴 업체(쿠폰)를 무작위로 배정하는 로직 문서.
구현: `PartnerService.assignRandomPartner()` / 관련 상태: `Partner` 엔티티.

## 1. 언제 실행되나

`PartnerService.assignPartnerToSession(Session)` → 결제 승인 직후, 또는 그 시점에
배정이 실패했던 세션을 재조회할 때 재시도된다. 배정에 실패해도 예외를 던지지 않고
로그만 남긴다 — 결제는 이미 외부(토스)에서 승인되어 되돌릴 수 없으므로, 배정 실패로
결제 확정/세션 조회 흐름 자체를 막지 않기 위함이다.

## 2. 후보 선정 3단계

```
전체 업체
  │  ① 노출 가능 필터 (findAvailableOrderByLastAssignedSeqAsc)
  ▼
노출 가능 업체 (활성 + 협약 미만료)
  │  ② 영업시간 필터 (isOperatingAt)
  ▼
영업 중인 업체 ──(하나도 없으면)──▶ FALLBACK_PARTNER_NAMES 중 활성 업체
  │
  │  ③ 배정 비율 최저 + LOW_PRIORITY 페널티 (selectLowestRatio)
  ▼
최저 실질비율 업체(들)
  │  ④ 직전 당첨 업체 제외 (excludeMostRecentlyAssigned, 후보 2곳 이상일 때만)
  ▼
최종 후보 중 무작위 1곳 선택
```

### ① 노출 가능 필터
`active = true` 이고 협약이 만료되지 않은 업체만 대상.

### ② 영업시간 필터 — `Partner.isOperatingAt(now)`
`operatingDays` / `openHour` / `closeHour` 중 하나라도 비어 있으면 그 업체는
**무조건 후보에서 제외**된다 (운영정보 미입력 업체가 실수로 배정되는 사고 방지).
시간 비교는 시(hour) 단위만 지원하고, `closeHour = 24`는 자정을 의미한다.

영업 중인 업체가 **하나도 없으면**, `FALLBACK_PARTNER_NAMES`(`피치못한`, `반짝`) 중
활성 상태인 업체만 후보로 대신 사용한다 — 영업시간 밖이라고 배정 자체가 막히지
않도록 하는 정책.

### ③ 배정 비율 최저 + 후순위 페널티 — `selectLowestRatio`
후보 풀 안에서 **배정 비율(`assignmentRatio`)이 가장 낮은 업체(들)**만 남긴다.

```
assignmentRatio = eligibleCount == 0 ? 0.0 : assignedCount / eligibleCount
```

| 값 | 의미 |
|---|---|
| `assignedCount` | 실제로 당첨되어 배정된 누적 횟수 |
| `eligibleCount`  | 영업 중이라 후보 풀에 들었던 누적 횟수 (당첨 여부 무관) |

영업일 수가 다른 업체끼리도 **기회 대비 당첨 비율**로 비교하기 때문에, 일주일에
하루만 영업하는 업체와 매일 영업하는 업체를 공평하게 다룰 수 있다. 아래 예시를 보면:

| 업체 | assignedCount | eligibleCount | assignmentRatio |
|---|---:|---:|---:|
| A (매일 영업) | 10 | 100 | 0.10 |
| B (주 1회 영업) | 2  | 10  | 0.20 |
| C (아직 후보 된 적 없음) | 0 | 0 | 0.00 (최우선) |

→ 이번엔 C가 먼저 후보가 되고, C가 없으면 A가 B보다 우선.

**리셋하지 않는 이유**: 특정 요일에만 영업하는 업체가 그 요일을 독점해 당첨이
일시적으로 몰려도 `eligibleCount`도 같이 커지므로 비율 자체는 자연히 낮아지지
않는다. 주기적으로 리셋하면 이 자기 교정 효과가 매번 사라지므로 리셋하지 않는다.

#### `LOW_PRIORITY_PARTNER_NAMES` 페널티

현재 `피치못한` 1곳이 지정되어 있다. 다른 업체와 영업시간이 겹치는 동안, 영업일이
겹친다는 이유만으로 배정 기회를 과도하게 가져가지 않도록 **완전 배제가 아니라
비율에 페널티를 더해 후순위로 미루는** 방식을 쓴다.

```java
private static final double LOW_PRIORITY_PENALTY = 0.5;
```

| 조건 | effectiveRatio |
|---|---|
| 후보 풀에 LOW_PRIORITY가 아닌 업체가 1곳이라도 있음 | `assignmentRatio + 0.5` |
| 후보 풀이 전부 LOW_PRIORITY뿐 (다른 업체 전부 휴무) | `assignmentRatio` (페널티 없음) |

`assignmentRatio`는 항상 `[0, 1]` 범위이므로, 다른 업체의 비율이
**(피치못한의 비율 + 0.5) 보다 높아야만** 역전되어 피치못한이 뽑힐 수 있다.
즉 다른 업체들이 유난히 자주 뽑혀 비율이 크게 오르면, 그때는 피치못한도 다시 뽑힌다
— 확률을 낮췄을 뿐 완전히 막지는 않는다.

**예시 (화요일, 피치못한 + 마주하다 둘 다 영업 중)**

| 업체 | assignmentRatio | 페널티 적용 여부 | effectiveRatio | 결과 |
|---|---:|:---:|---:|---|
| 피치못한 | 0.0 | O (+0.5) | 0.5 | 후순위 |
| 마주하다 | 0.0 | X | 0.0 | **선택** |

마주하다가 계속 당첨되어 비율이 오른 상황이라면:

| 업체 | assignmentRatio | 페널티 적용 여부 | effectiveRatio | 결과 |
|---|---:|:---:|---:|---|
| 피치못한 | 0.0 | O (+0.5) | 0.5 | — |
| 마주하다 | 0.6 | X | 0.6 | — |

→ `0.5 < 0.6`이므로 이번엔 **피치못한이 선택**된다 (역전 성립).

### ④ 직전 당첨 업체 제외 — `excludeMostRecentlyAssigned`
③에서 남은 후보가 2곳 이상이면, `lastAssignedSeq`가 가장 큰(=가장 최근 당첨된)
업체 1곳만 제외하고 나머지 중 무작위로 고른다. 특정 업체가 연속으로 당첨되는
상황을 최소화하기 위함. 후보가 1곳뿐이거나 아무도 당첨된 적 없으면 제외하지 않는다.

## 3. 배정 후 상태 갱신

선택 여부와 무관하게 **후보 풀 전체**가 `eligibleCount++` (③ 이전 시점의
`candidatePool` 기준 — fallback도 동일하게 적용).
**선택된 업체만** 추가로:

- `assignedCount++`
- `lastAssignedSeq = 전역 시퀀스 다음 값` (`nextAssignedSeq()`, `MAX(lastAssignedSeq) + 1`)

> ⚠️ 순서 중요: 후보 선정(③)은 반드시 `eligibleCount`를 올리기 **전의** 누적 비율로
> 해야 한다. 먼저 전부 `+1` 해버리면 분모가 같이 커져서 비율 순서 자체가 뒤집힐 수
> 있다 (예: A=10/11, B=1/1이면 A가 더 낮지만, 먼저 +1하면 A=10/12, B=1/2로
> 역전된다).

## 4. 동시성

`Partner.version`(`@Version`)으로 낙관적 락을 건다. 동시 결제 승인으로 여러 세션이
같은 시점에 배정을 시도해 `assignedCount`/`eligibleCount` 갱신이 충돌하면
`ObjectOptimisticLockingFailureException`이 발생하고, 이 경우도 예외를 삼키고
배정 실패로 처리한다 (별도 재시도 없음).

## 5. 전체 흐름 요약 표

| 단계 | 조건 | 결과 |
|---|---|---|
| 영업 중인 업체 있음 | LOW_PRIORITY 아닌 업체 포함 | 전체 후보, LOW_PRIORITY에 페널티 적용 |
| 영업 중인 업체 있음 | LOW_PRIORITY만 영업 | 전체 후보, 페널티 없음 |
| 영업 중인 업체 없음 | — | `FALLBACK_PARTNER_NAMES`(피치못한, 반짝) 중 활성 업체만 |
| 후보 풀 자체가 비어있음 | fallback도 없음 | `CustomException(NO_ACTIVE_PARTNER)` → 배정 실패, 로그만 남김 |

## 6. 관련 상수 (변경 시 여기도 갱신)

| 상수 | 위치 | 현재 값 | 의미 |
|---|---|---|---|
| `FALLBACK_PARTNER_NAMES` | `PartnerService` | `{"피치못한", "반짝"}` | 전부 휴무일 때 대신 배정할 업체 |
| `LOW_PRIORITY_PARTNER_NAMES` | `PartnerService` | `{"피치못한"}` | 겹치는 시간대에 후순위로 미룰 업체 |
| `LOW_PRIORITY_PENALTY` | `PartnerService` | `0.5` | 후순위 페널티 크기 (클수록 더 드물게 뽑힘) |
