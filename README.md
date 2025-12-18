# 🚶 RoadMate Backend

서울시 보행 네트워크 공공데이터를 수집하여
**자체 보행 그래프를 구축하고, 최단 경로 및 루프형 산책 경로를 생성하는 백엔드 서버**입니다.

본 프로젝트는 공공 API를 실시간으로 중계하지 않고,
**대규모 보행 데이터를 사전에 수집·정규화하여 DB에 저장한 뒤
그래프 기반 경로 연산을 수행**하는 구조로 설계되었습니다.

---

## 🧩 What This Backend Does

* 서울시 **보행 노드(Node) / 링크(Link)** 데이터 수집
* 수집된 데이터를 기반으로 **보행 그래프 구성**
* 최단 경로 및 **루프형 산책 경로 생성**
* REST API 형태로 경로 결과 제공
* 날씨 API 연동을 통한 부가 정보 제공

---

## 🔄 Overall Flow

```
서울시 보행 데이터 API
        ↓
[Data Ingestion]
        ↓
MySQL (Node / Link / Route)
        ↓
In-memory Graph
        ↓
경로 탐색 / 루프 경로 생성
        ↓
REST API 응답
```

---

## 📥 Data Ingestion (보행 데이터 수집)

* 서울시 `TbTraficWlkNet` API 사용
* **자치구(District) 단위**로 데이터 수집
* 노드(Node)와 링크(Link)를 분리 저장
* 링크에 대응되는 노드가 존재하지 않는 경우 **가상 노드 생성**
* 수집 상태 및 이력은 `import_log` 테이블로 관리

👉 한 번 수집된 데이터는 재사용되며,
API 호출에 의존하지 않고 안정적으로 경로 연산이 가능합니다.

---

## 🧠 Path Finding

### Shortest Path

* 그래프 기반 **Dijkstra 알고리즘** 사용
* 실제 보행 거리(meter)를 가중치로 사용
* 결과는 좌표 리스트 형태로 반환

### Loop Route (산책 경로)

* 출발 지점을 기준으로 **루프 형태 경로 생성**
* 목표 거리 대비 우회 정도를 자동 조절
* 단순 왕복이 아닌 **순환형 산책 경로** 제공

---

## 🗄 Database Design (요약)

| Table        | Description                    |
| ------------ | ------------------------------ |
| `node`       | 보행 네트워크의 노드 정보                 |
| `link`       | 노드 간 연결 정보 및 거리                |
| `import_log` | 구별 데이터 수집 상태 관리                |
| `walk_route` | 사용자 경로 저장 (`JSON` 기반 path 데이터) |
| `user`       | 사용자 정보                         |

* `walk_route.path_data`는 **MySQL JSON 타입**을 사용하여
  복잡한 좌표 리스트를 단일 컬럼으로 관리합니다.

---

## 🧱 Project Structure

```
src/main/java
 └─ com.trm.roadmate_backend
    ├─ controller     # REST API
    ├─ service        # 경로 탐색, 루프 생성, 데이터 수집
    ├─ entity         # JPA Entity
    ├─ repository     # Spring Data JPA
    ├─ dto            # Request / Response DTO
    └─ config         # 보안, WebClient, Swagger 설정
```

---

## ⚙️ Tech Stack

* **Java 21**
* **Spring Boot 3.x**
* **Spring Data JPA**
* **Spring WebFlux (WebClient)**
* **MySQL**
* **Swagger (SpringDoc OpenAPI)**

---

## 🚀 Running Locally

### 1. Environment Variables

```text
DB_URL
DB_USER
DB_PASSWORD
WALK_API_KEY
WEATHER_API_KEY
```

### 2. Run

```bash
./gradlew bootRun
```

### 3. API Documentation

```
http://localhost:8080/swagger-ui/index.html
```

---

## 📌 Notes

* 대용량 보행 데이터 기반 실험용/학습용 프로젝트입니다.
* 실시간 API 의존성을 줄이고, **데이터 중심 설계**에 초점을 맞췄습니다.
* 경로 생성 로직은 실제 서비스 적용을 고려하여 확장 가능하게 구성되어 있습니다.

---

## 📄 License

This project is for educational and portfolio purposes.
