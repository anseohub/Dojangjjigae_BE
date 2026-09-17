# 도장찍개 백엔드 (Spring Boot)

천안 반려동물 동반 여행 서비스 '도장찍개'의 백엔드. React 프론트(`dojangjjigae-react`)와 짝을 이룹니다.

## 1. 로컬 실행 (Docker 없이, H2 파일DB)

```bash

# 2) 실행 (로컬에 Maven 설치되어 있어야 함 — IntelliJ 쓰면 그냥 main 메서드 실행해도 됨)
mvn spring-boot:run
```

> Maven Wrapper(`./mvnw`)는 포함하지 않았습니다. IntelliJ에서 열면 내장 Maven으로 바로 실행되고,
> 터미널에서 하시려면 로컬에 Maven이 설치돼 있어야 합니다 (`brew install maven`).

- 기본 포트: `http://localhost:8085
- H2 콘솔: `http://localhost:8085/h2-console`
  (JDBC URL: `jdbc:h2:file:./data/dojangjjigae`, user: `sa`, password: 없음)
- 최초 실행시 `places-seed.json` 이 있으면 자동으로 DB에 적재됩니다 (`app.seed.enabled=true`, local 프로필 기본값).
- 재실행해도 이미 데이터가 있으면 다시 적재하지 않습니다.

### 확인

```bash
curl http://localhost:8085/api/places
curl http://localhost:8085/api/places/1
```

## 2. 장소 데이터 수동 재분류 (중요)

TourAPI 데이터에는 Explore 화면의 5개 카테고리(문화·역사/공원·산책로/실내/카페·식당/반려견놀이터)나
반려동물 동반 가능 여부가 없습니다. `DataSeeder` 가 대략적으로 카테고리를 추측하지만,
**스탬프 랠리에 실제로 쓸 10~15개 장소는 H2 콘솔에서 직접 수정**하세요:

```sql
UPDATE places
SET category = 'PARK', pet_allowed = true
WHERE name = '각원사';

INSERT INTO place_condition_tags (place_id, tag) VALUES (12, '리드줄 필수');
```

## 3. 배포

### 백엔드 → Render

1. GitHub에 이 프로젝트 push
2. Render 대시보드 → New → Web Service → 레포지토리 연결
3. Build Command: `./mvnw clean package -DskipTests`
4. Start Command: `java -jar target/*.jar`
5. Environment: New → PostgreSQL 추가 (무료 티어) → 생성된 정보를 아래 환경변수로 Web Service에 연결
   - `SPRING_PROFILES_ACTIVE=prod`
   - `SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/<db>`
   - `SPRING_DATASOURCE_USERNAME=<user>`
   - `SPRING_DATASOURCE_PASSWORD=<password>`
   - `CORS_ALLOWED_ORIGINS=https://<your-vercel-domain>.vercel.app`
   - `SEED_ON_STARTUP=true` (최초 배포 시 한 번만 true로 해서 데이터 적재 후, 다시 false로 바꿔도 됨 — 안전장치로 이미 데이터 있으면 어차피 스킵)
6. `places-seed.json` 도 함께 커밋해서 push해야 배포 환경에서도 시드가 됩니다.

### 프론트 → Vercel

1. `dojangjjigae-react` 레포를 Vercel에 Import
2. Framework Preset: Vite
3. 환경변수로 백엔드 URL 등록 후, 프론트 코드에서 `import.meta.env.VITE_API_BASE_URL` 로 API 호출 (아직 프론트에 fetch 코드가 없으므로 연동 시 추가 필요)

## 4. 진행 상황

- [x] Place 엔티티 + `/api/places`, `/api/places/{id}` (Day 1)
- [x] Pet 엔티티 + `/api/pets` (Day 2)
- [x] Course, CourseSpot + `/api/courses/*`, `/api/weather/today` (Day 2)
- [x] StampRecord + `/api/stamps/certify` (Haversine 거리 계산) (Day 3)
- [ ] Reward, ExchangeLocation (Day 4)

자세한 API 명세는 `api-spec.md` 참고 (별도 전달).

## 5. Day 2 추가 API

| 화면 | Method & URL |
|---|---|
| Main - 오늘의 추천 코스 | `GET /api/courses/today` |
| Main - 오늘의 날씨 | `GET /api/weather/today` |
| Profile - 내 반려동물 목록 | `GET /api/pets` |
| Profile - 반려동물 등록 | `POST /api/pets` (body: PetCreateRequest) |
| Profile - 반려동물 수정 | `PUT /api/pets/{id}` |
| Course - 추천 코스 상세 | `GET /api/courses/recommend?petId=` |
| Course - 코스 상세 | `GET /api/courses/{id}` |
| Course - 다른 추천 코스 | `GET /api/courses?excludeId=` |

### 데모 코스 자동 생성

`CourseSeeder` 가 앱 최초 실행시 `places` 테이블에서 아무 장소나 가져와 데모 코스 3개를 자동으로 만듭니다
(장소가 4개 이상 있어야 동작). **이건 API가 비어있지 않게 하려는 임시 데이터**라 실제 발표에 쓸 코스는
H2 콘솔(`/h2-console`)에서 `courses`, `course_spots` 테이블을 직접 다시 구성하는 것을 추천합니다.

## 6. Day 3 - 스탬프 GPS 인증

| 용도 | Method & URL |
|---|---|
| 코스 시작 | `POST /api/course-progress/start` (body: `{userId, courseId}`) |
| 진행중인 코스 조회 | `GET /api/users/{userId}/course-progress/current?lat=&lng=` |
| 스탬프 지점 상태 | `GET /api/courses/{courseId}/spots?userId=&lat=&lng=` |
| **GPS 인증(스탬프 찍기)** | `POST /api/stamps/certify` |

인증 반경은 `StampService.CERTIFY_RADIUS_M` (기본 100m) 에서 조정할 수 있습니다.

### 전체 흐름 테스트 (courseId=2, 장소 5·6번 코스 기준으로 예시)

```bash
# 1) 코스 시작
curl -X POST http://localhost:8085/api/course-progress/start \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"courseId":2}'

# 2) 진행 상황 확인 (아직 0/2)
curl http://localhost:8085/api/users/1/course-progress/current

# 3) 스탬프 지점 목록 확인 (place id는 /api/courses/2 로 미리 확인)
curl "http://localhost:8085/api/courses/2/spots?userId=1"

# 4) 장소의 실제 좌표로 인증 시도 (성공 케이스 - place 5의 lat/lng 그대로 사용)
curl -X POST http://localhost:8085/api/stamps/certify \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"courseId":2,"placeId":5,"lat":36.824260594850166,"lng":127.16294469433556}'

# 5) 반경 밖 좌표로 인증 시도 (실패 케이스 - 서울시청 좌표)
curl -X POST http://localhost:8085/api/stamps/certify \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"courseId":2,"placeId":6,"lat":37.5665,"lng":126.9780}'
```
