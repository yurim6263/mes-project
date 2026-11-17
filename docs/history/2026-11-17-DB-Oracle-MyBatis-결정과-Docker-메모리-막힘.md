# 2026-11-17 — DB는 Oracle, 영속 계층은 MyBatis로 결정 / Docker 메모리 한도 때문에 반영 보류

> 이어서 하는 사람(새 세션)은 **맨 아래 "다음에 할 일"부터** 읽는다. 이 날 **저장소 코드는 바꾸지 않았다**(결정만 기록). 코드·compose는 아직 PostgreSQL 기준이다.

## 결정한 것
### 1. DB = Oracle (Oracle Database Free 23ai, Docker)
- 사용자 요청: "DB도 가장 많이 쓰이는 거로". 
- 근거: 웹 검색 결과(헤드헌팅·채용 사이트)에서 MES 공고는 **Oracle이 가장 자주 보임**(생산·품질 데이터를 Oracle에 저장, PL/SQL, Oracle/MSSQL 병기). MSSQL은 C# 계열 공고 일부, PostgreSQL·MariaDB는 일부 최신 공고. **점유율 통계는 찾지 못했고, 표본이 작고 편향될 수 있음**(03 문서의 앞선 표본 3건에서도 Oracle·MSSQL 확인).
- 구현 방침(아직 미적용): 이미지 `gvenzl/oracle-free:23-slim-faststart`(로컬에 pull 완료), 접속 `jdbc:oracle:thin:@//localhost:1521/FREEPDB1`, 앱 계정 `mes`/`mes`(로컬 개발용), 드라이버 `ojdbc11`, Flyway `flyway-database-oracle`, 테스트 `org.testcontainers:oracle-free`(1.21.4).
- 버린 대안: PostgreSQL 유지(이미 동작 확인함), MSSQL Express.
- 영향 받는 파일(바꿀 때): `docker-compose.yml`(db 서비스·포트·헬스체크·backend의 SPRING_DATASOURCE_*), `.env.example`, `backend/build.gradle`, `backend/src/main/resources/application.yml`, `backend/src/main/resources/db/migration/V1__init.sql`(`SELECT 1 FROM DUAL;`), `backend/src/test/.../MesApplicationTests.java`(OracleContainer), `README.md`, `CLAUDE.md`, `03_확정안` 1장의 DB 행과 PostgreSQL 언급, 메모리 `mes-project-decisions.md`. 이 파일들은 **전부 아직 PostgreSQL 기준**.
- Oracle 주의점(1주차 설계 시): 식별자는 기본 대문자, `LIMIT` 없음(`FETCH FIRST n ROWS ONLY`), 자동증가는 `GENERATED AS IDENTITY` 또는 시퀀스, 빈 문자열은 NULL로 취급, 날짜는 `TIMESTAMP`.

### 2. 영속 계층 = MyBatis
- 사용자 요청: "영속계층도 너 의견으로". 
- 이유: 국내 MES 현장(Oracle·MSSQL + SQL 중심 개발)에서 흔하고, LOT 정/역추적·OEE처럼 복잡한 SQL을 직접 통제하기 쉽다. 이 프로젝트가 보여 주려는 것(상태 전이·멱등 수집 `UNIQUE(equipment_id, seq)`·추적 쿼리)도 SQL이 드러나는 편이 설명하기 좋다.
- 구현 방침(아직 미적용): `org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.5`(Boot 3.x용, Maven Central에 있음을 확인), 매퍼 XML은 `src/main/resources/mapper/`, `mybatis.configuration.map-underscore-to-camel-case: true`. 매퍼 인터페이스는 도메인 패키지(`workorder`, `lot` …) 안에 둔다.
- 버린 대안: JPA(CRUD는 편하지만 추적·OEE 쿼리에서 불리).

## 막힌 것 — Docker VM 메모리 2GB (사용자 승인 필요)
- 증상: Oracle Free 컨테이너(`ora-test`)를 단독으로 띄우자 **Docker 엔진이 응답하지 않음**(`docker ps` 등이 500 Internal Server Error). `wsl -l -v`에서 `docker-desktop`은 Running으로 보임.
- 원인(추정, 아직 확인 못 함): `C:\Users\dbfla\.wslconfig`에 `memory=2GB`, `swap=0`이 설정돼 있어 Docker VM 한도가 약 2GB다(`docker info` MemTotal ≈ 1.99GB 확인함). Oracle Free는 최소 약 2GB가 필요하다고 알려져 있어 한도를 넘긴 것으로 보임. 이 PC의 물리 메모리는 약 15.7GB.
- 시도: 한도를 6GB로 올리려고 `.wslconfig`를 수정하려 했으나 **자동 권한 검사가 거부**했다(사용자 전역 설정 파일 변경). 우회하지 않았다. **`.wslconfig`는 그대로 `memory=2GB`이고 백업 파일은 만들지 못함**(확인함).
- 남아 있는 것: `ora-test` 컨테이너와 `gvenzl/oracle-free:23-slim-faststart` 이미지가 Docker 안에 있을 수 있음(엔진이 응답하지 않아 확인 못 함). 포트 1521을 쓰고 있을 수 있음.
- 해결안(사용자 선택):
  - (A, 권장) `.wslconfig`의 `memory=2GB` → `memory=6GB`로 올리고 `wsl --shutdown` 후 Docker Desktop 재시작. 15.7GB 중 6GB를 WSL이 쓰게 되고 IDE·브라우저 사용 시 여유는 남는다. 원래 값(2GB)은 `.wslconfig.bak-날짜`로 백업해 두고 되돌릴 수 있게 한다.
  - (B) 이 변경이 싫다면 DB를 PostgreSQL로 유지(이미 동작 확인됨). 이 경우 "가장 많이 쓰이는 DB" 요구는 포기하는 셈이고, SQL은 표준 위주로 쓰면 이력서에서 "Oracle로 이식 가능"이라고만 말할 수 있음(실제 Oracle 검증은 없음).

## 설치·결정 점검 (요구사항·ERD 시작 전)
| 항목 | 상태 |
|---|---|
| JDK 17, Docker Desktop, Node, Git, Python, IntelliJ | 설치됨(11-14에 확인) |
| UaExpert | 선택 사항. 가입이 필요해 설치하지 않음(asyncua 스크립트로 대체 가능) |
| 영속 계층 | **결정됨: MyBatis** |
| DB | **결정됨: Oracle**, 단 **Docker 메모리 한도(2GB) 때문에 실제 동작 확인 전** → 위 A/B 중 선택 필요 |
| 프로젝트 뼈대 | backend(Spring Boot 3.5.16 + Gradle)·simulator 스텁·frontend(Vite React-TS)·compose는 **PostgreSQL 기준으로** 전체 기동 확인됨(11-14) |

→ **요구사항·ERD 작성은 DB 선택(A/B)과 무관하게 시작할 수 있다**(ERD 논리 설계는 DB 종류에 크게 의존하지 않음). 다만 Oracle로 확정하면 데이터 타입·키 생성 방식을 Oracle 기준으로 쓴다.

## 다음에 할 일 (이어서 할 지점)
1. 사용자에게 해결안 A/B 확인. (A면) `.wslconfig` 변경 승인 → 백업 → 6GB → `wsl --shutdown` → Docker Desktop 재시작 → `docker ps`로 응답 확인 → `docker rm -f ora-test`
2. Oracle 반영: 위 "영향 받는 파일" 전부 수정 → `docker compose up -d --build`로 전체 기동 확인 → `gradlew test`(Testcontainers Oracle, 첫 실행은 이미지 기동에 1~2분 걸릴 수 있음) → 메모리 실측 기록(유휴 합계, 03 문서 "16GB 구동 가능" 추정 재확인)
3. MyBatis 반영: 의존성·설정 추가, `mapper/` 폴더, 간단한 매퍼 1개로 동작 확인
4. 1주차 산출물: `docs/requirements.md` → `docs/erd.md` → `docs/state-transition.md` → `docs/opcua-nodes.md`
5. 기준정보 CRUD → 작업지시 API(상태 전이 강제) + 전이 테스트
6. 첫 커밋(사용자가 요청할 때)

## 알아둘 것
- 새 세션에서 코드를 볼 때 **PostgreSQL 기준 코드와 Oracle 결정이 어긋나 있다**는 점을 먼저 기억할 것. 1번이 끝나기 전에는 compose의 `db`는 PostgreSQL(호스트 포트 5433)이다.
- PC에는 자체 PostgreSQL 17 Windows 서비스가 5432를 쓰고 있다(건드리지 않음). Oracle은 호스트 1521을 쓸 예정.
- 이전 기록: `2026-11-14-기획확정-폴더구조-기록체계.md` (기획 확정, 폴더·compose·README, backend/simulator/frontend 생성, 테스트).

---

## 정정·추가 — 해결안 A 실행, Oracle·MyBatis 반영 완료 (같은 날)
> 위 "막힌 것"과 "코드는 아직 PostgreSQL 기준"은 작성 시점 기준이었고, 아래 작업으로 **해소됨**. 이 절이 최신 상태다.

### 1. Docker 메모리 한도 상향 (사용자 승인 후 실행)
- `C:\Users\dbfla\.wslconfig`: `memory=2GB` → `memory=6GB` (`processors=4`, `swap=0`은 그대로). **원본은 `C:\Users\dbfla\.wslconfig.bak-20261117`에 백업**(되돌리려면 이 파일 내용으로 복원 후 `wsl --shutdown`).
- `wsl --shutdown` 후 Docker Desktop 재시작 → 엔진 29.4.0 응답, `docker info` MemTotal ≈ 6.2GB 확인.
- 멈췄던 `ora-test` 컨테이너는 삭제함.

### 2. Oracle 반영 (확인함)
- `docker-compose.yml` db: `gvenzl/oracle-free:23-slim-faststart`, 호스트 포트 **1521**, 볼륨 `oracle-data`, 헬스체크 `healthcheck.sh`, 계정 `mes`/`mes`(로컬 개발용, `ORACLE_PASSWORD`·`APP_USER`·`APP_USER_PASSWORD`로 변경 가능). 접속: `jdbc:oracle:thin:@//localhost:1521/FREEPDB1`
- `.env.example`, `application.yml`, `V1__init.sql`(`SELECT 1 FROM DUAL;`), `build.gradle`(ojdbc11, flyway-database-oracle, testcontainers oracle-free) 수정.
- 테스트 import 주의: 이 Testcontainers 버전은 `org.testcontainers.oracle.OracleContainer`다(`org.testcontainers.containers.OracleContainer`는 컴파일 안 됨).
- 옛 PostgreSQL 볼륨 `smt-mes_db-data`(Flyway V1 테스트 데이터뿐)는 삭제함.

### 3. MyBatis 반영 (확인함)
- `mybatis-spring-boot-starter:3.0.5`, `application.yml`에 `mapper-locations: classpath:mapper/**/*.xml`, `map-underscore-to-camel-case: true`.
- 연결 확인용 `common/DbPingMapper.java` + `resources/mapper/common/DbPingMapper.xml` (**실제 도메인 매퍼가 생기면 삭제해도 됨**).
- 매퍼 규칙: 인터페이스는 도메인 패키지에, SQL XML은 `resources/mapper/<도메인>/`에.

### 4. 확인한 방법과 결과
- `gradlew test` → **tests=2, failures=0, errors=0** (Oracle Testcontainer에서 Flyway V1 적용 + MyBatis XML 매퍼 호출). 소요 1분 20초(이미지 기동 포함).
- `docker compose up -d --build` → db(healthy)·simulator·backend·frontend 모두 Up, `localhost:8080/actuator/health` → `{"status":"UP"}`(= 컨테이너 안 앱이 Oracle에 붙고 Flyway 통과), `localhost:5173` → HTTP 200.
- **메모리 실측(유휴)**: db(Oracle) 2.10GiB, backend 258MiB, frontend 178MiB, simulator 90MiB → **합계 약 2.6GiB / Docker 한도 5.79GiB**. Milo 수집기·부하가 붙으면 다시 측정할 것. 확인 후 `docker compose stop`.
- 문서 갱신: `README.md`, `03_확정안`의 DB·영속 계층·배포 행과 일정 문구, `CLAUDE.md`, 메모리.

### 점검표 (요구사항·ERD 시작 전) — 최신
| 항목 | 상태 |
|---|---|
| JDK 17 · Docker Desktop(메모리 6GB) · Node · Git · Python · IntelliJ | 준비됨 |
| UaExpert | 선택 사항, 미설치(가입 필요. asyncua 스크립트로 대체 가능) |
| DB | **Oracle Free 23ai — 동작 확인됨** |
| 영속 계층 | **MyBatis — 동작 확인됨** |
| 전체 기동(compose) | 확인됨 |
| 미확인 | IntelliJ에서 프로젝트를 열어 본 것, Oracle 부하 상태의 메모리 |
→ **결정·설치 모두 완료. 요구사항·ERD를 시작할 수 있다.**

### 다음에 할 일 (최신 — 위 목록 대체)
1. 1주차 산출물 순서: `docs/requirements.md` → `docs/erd.md`(Oracle 데이터 타입·키 생성은 `GENERATED AS IDENTITY`) → `docs/state-transition.md` → `docs/opcua-nodes.md`
2. ERD 확정 후 Flyway `V1__init.sql`을 실제 DDL로 교체(Oracle 문법: `NUMBER`, `VARCHAR2`, `TIMESTAMP`, `FETCH FIRST n ROWS ONLY`)
3. 기준정보 CRUD → 작업지시 API(상태 전이 강제) + 전이 테스트
4. 첫 커밋(사용자가 요청할 때)

### 알아둘 것 (최신)
- 주소: 화면 5173 · API 8080 · OPC UA `opc.tcp://localhost:4840` · **Oracle `localhost:1521/FREEPDB1` (mes/mes)**. (PC 자체 PostgreSQL 17 서비스는 5432이고 이 프로젝트와 무관)
- 전체 기동 `docker compose up -d --build`, 종료 `docker compose stop`. Oracle 첫 기동은 느릴 수 있음(이미지는 이미 받아 둠).
- Oracle 습관: 식별자 대문자, 빈 문자열=NULL, `LIMIT` 없음. Flyway 이력 테이블은 소문자 따옴표 이름(`"flyway_schema_history"`).
- 백엔드 테스트: `backend`에서 `gradlew.bat test` (Docker 켜짐 + JAVA_HOME=JDK 17 필요).
