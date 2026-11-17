# SMT 라인 미니 MES

설비(OPC UA)에서 올라오는 데이터를 작업지시·자재 LOT와 연결해, 불량이 나면 **어떤 자재·설비·시각 때문인지 역추적**할 수 있게 만든 개인 프로젝트(4주, 1인).

> ⚠ **가상 프로젝트입니다.** 설비는 Python 시뮬레이터이고 데이터는 전부 가상입니다. 실제 현장에서 검증하지 않았습니다.
> 상태: **0일차 — 구조만 만든 상태** (아래 "진행 현황" 참고)

## 이 프로젝트의 핵심 (구현 후 결과를 여기에 채운다)

| 핵심 | 내용 | 결과 |
|---|---|---|
| 작업지시 상태 전이 | 허용되지 않은 전이는 서버가 거부 | 테스트 __개 (작성 예정) |
| 멱등 수집·복구 | 설비 이벤트에 순번(`seq`)을 붙여 중복 방지, 통신 복구 시 놓친 구간 재수신 | 시나리오 시험 (작성 예정) |
| LOT 정/역추적 | 불량 LOT → 사용된 릴·솔더 LOT·설비·시각 조회 | (작성 예정) |
| 수량 정정 이력 | 사유 필수, 이전 값 보존 | (작성 예정) |
| OEE·불량률 | 손계산 기대값과 일치하는 테스트 | (작성 예정) |

`__`와 "작성 예정"은 **구현 후 측정한 값으로만** 채운다.

## 구조

```
[설비 시뮬레이터]  --OPC UA-->  [backend: 수집기 → 도메인 → DB]  <--REST--  [frontend]
 Python asyncua                 Java 17 · Spring Boot 3             React + Vite
 PRT-01 MNT-01/02               Eclipse Milo (collector)
 RFL-01 AOI-01                  Oracle Free 23ai · MyBatis
```

```
mes project/
├─ backend/      Spring Boot API + OPC UA 수집기
│  └─ src/main/java/com/smtmes/
│     ├─ common/      공통(예외, 설정)
│     ├─ master/      기준정보 — 제품·BOM·공정·설비·불량 코드
│     ├─ workorder/   작업지시 + 상태 전이
│     ├─ equipment/   설비 상태·알람
│     ├─ collector/   OPC UA 수집(멱등·재연결 복구)
│     ├─ lot/         자재 LOT·생산 LOT·정/역추적
│     ├─ quality/     검사(AOI)·불량·재작업·수량 정정
│     └─ oee/         OEE·KPI
│  └─ src/main/resources/db/migration/   DB 스키마 변경 이력(SQL)
├─ simulator/    가상 설비 5대 (OPC UA 서버)
├─ frontend/     화면 4개 — 작업지시 / 설비 현황 / LOT 추적 / 대시보드
├─ docs/         요구사항·ERD·상태 전이·OPC UA 노드·AI 사용 기록
└─ docker-compose.yml
```

## 실행

```bash
docker compose up -d --build   # DB · 시뮬레이터 · 서버 · 화면이 한 번에
docker compose stop            # 종료 (데이터 유지)
docker compose up -d db        # DB만 (Oracle Free, localhost:1521/FREEPDB1, 계정/비밀번호: mes/mes) — 첫 기동은 1~2분
```

> 현재(0일차)는 시뮬레이터가 스텁(설비 1대, 노드 2개)이고 화면은 Vite 기본 템플릿입니다.

- 화면 http://localhost:5173 · API http://localhost:8080 · OPC UA `opc.tcp://localhost:4840` (UaExpert 등 OPC UA 클라이언트로 확인 가능)
- 서버 테스트: `cd backend && gradlew.bat test` (JDK 17, Docker 필요)
- DB 비밀번호 등은 로컬 개발용 기본값입니다. 바꾸려면 `.env.example`을 `.env`로 복사해 수정합니다.

## 가상 SMT 라인

`PCB 투입 → 스크린 프린터(PRT-01) → 마운터(MNT-01, MNT-02) → 리플로우(RFL-01) → AOI(AOI-01) → 완성/불량`
제품 2종(CTRL-A, SENS-B), 2교대, 추적 단위는 생산 LOT.

## 한계 (반드시 함께 읽을 것)

- 가상 설비·가상 데이터이며 실제 현장 검증이 없습니다.
- 추적 단위는 **LOT**이고 보드 시리얼 단위 추적은 하지 않습니다(실제 SMT는 시리얼 단위가 많음).
- 사용자 권한·로그인, 모바일, 생산계획 최적화, ERP 연동은 범위 밖입니다.

## AI 도구 사용

어떤 부분을 AI에 맡기고 어떤 부분을 직접 했는지 [`docs/ai-usage.md`](docs/ai-usage.md)에 날짜별로 기록합니다.

## 진행 현황

- [x] 0일차: 폴더 구조 · docker-compose · README 초안
- [x] 0일차: JDK 17 · Docker · Node 준비, backend(Spring Boot 3.5 + Gradle) 생성, DB 연결·Flyway 확인
- [x] 0일차: simulator 스텁 · frontend(Vite) 생성, `docker compose up` 전체 기동 확인, 스모크 테스트
- [x] 0일차: IntelliJ IDEA Community 설치
- [x] 0일차: DB를 Oracle Free, 영속 계층을 MyBatis로 확정·반영 (`gradlew test` 2개 통과, 전체 기동 확인)
- [ ] 0일차: UaExpert는 선택(가입 필요)
- [ ] 1주차: 요구사항 · ERD · 상태 전이 · 기준정보 · 작업지시 API
- [ ] 2주차: 시뮬레이터 · 수집기(멱등·복구) · 설비 상태·알람 · 작업지시 화면
- [ ] 3주차: 품질 · LOT 정/역추적 · 정정/재작업 · 설비/LOT 화면
- [ ] 4주차: OEE · 대시보드 · 예외 시나리오 시험 · 문서 · 시연 영상 · 회고
