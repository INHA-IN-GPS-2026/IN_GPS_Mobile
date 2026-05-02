# IN_GPS - IoT 온도 센서 모니터링 앱

> Android 기반 IoT 서미스터 센서 실시간 모니터링 애플리케이션

---

## 목차

- [프로젝트 개요](#프로젝트-개요)
- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [아키텍처: MVVM 패턴](#아키텍처-mvvm-패턴)
- [프로젝트 구조](#프로젝트-구조)
- [패키지 구조](#패키지-구조)
- [API 명세](#api-명세)
- [데이터 모델](#데이터-모델)
- [화면 구성](#화면-구성)
- [개발 환경 설정](#개발-환경-설정)

---

## 프로젝트 개요

**IN_GPS**는 IoT 장치에 부착된 서미스터(온도 센서)의 데이터를 실시간으로 수집하고 시각화하는 Android 애플리케이션입니다.
백엔드 REST API를 통해 센서 데이터를 폴링하여 온도 변화 추이, 장치 상태, 3D 자세 시각화 기능을 제공합니다.

| 항목 | 내용 |
|------|------|
| 언어 | Java |
| 최소 SDK | API 24 (Android 7.0) |
| 타겟 SDK | API 36 (Android 15) |
| 아키텍처 | MVVM + Repository Pattern |
| UI 프레임워크 | Material Design 3 |

---

## 주요 기능

### 1. 실시간 장치 목록 모니터링
- 등록된 IoT 장치 목록 표시
- 장치 상태 색상 코드 (정상 / 경고 / 위험)
- 30초 간격 자동 폴링

### 2. 온도 데이터 시각화
- 2채널 온도 라인 차트 (코어 온도 / 주변 온도)
- 기간 선택: 1일 / 7일 / 30일 / 1년
- Catmull-Rom 스플라인 보간법 적용
- 이벤트 마커 (경고, 연결 끊김)
- 5초 간격 자동 폴링

### 3. 시스템 전체 현황
- 전체 장치 상태 집계 바 차트
- 상태별 카운트 (정상 / 경고 / 위험 / 전체)
- 30초 간격 자동 폴링

### 4. 폴링 주기 설정
- NumberPicker를 통한 폴링 주기 사용자 설정

---

## 기술 스택

| 분류 | 라이브러리 / 기술 |
|------|-----------------|
| 네트워크 | Retrofit 2.9.0, OkHttp 4.11.0 |
| 직렬화 | GSON |
| 차트 | MPAndroidChart v3.1.0 |
| 아키텍처 컴포넌트 | AndroidX Lifecycle (LiveData, ViewModel) |
| UI | Material Design 3, RecyclerView, Fragment |
| 비동기 처리 | Handler / Looper (Main Thread Polling) |
| 빌드 도구 | Gradle 9.3.1 |

---

## 아키텍처: MVVM 패턴

본 프로젝트는 **MVVM(Model-View-ViewModel)** 아키텍처 패턴과 **Repository 패턴**을 결합하여 구현되었습니다.

### MVVM 개념

```
┌──────────────────────────────────────────────────────────┐
│                        View Layer                        │
│         (Activity / Fragment - UI 표시 & 사용자 이벤트)        │
│                  LiveData 관찰 (observe)                   │
└────────────────────────┬─────────────────────────────────┘
                         │ 데이터 바인딩 (단방향)
┌────────────────────────▼─────────────────────────────────┐
│                    ViewModel Layer                       │
│        (UI 상태 관리, 비즈니스 로직, LiveData 노출)           │
│              Repository 호출 & 폴링 관리                    │
└────────────────────────┬─────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────┐
│                   Repository Layer                       │
│           (데이터 소스 추상화, API 호출 캡슐화)                │
└────────────────────────┬─────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────┐
│                     Model Layer                          │
│        (Data Class / POJO - 순수 데이터 구조 정의)            │
│              + Retrofit API Interface                    │
└──────────────────────────────────────────────────────────┘
```

### 각 레이어 역할

#### View (Fragment / Activity)
- UI 렌더링 및 사용자 입력 처리만 담당
- ViewModel의 LiveData를 `observe()`로 관찰하여 UI 갱신
- ViewModel에 직접 데이터 요청하지 않음 (단방향 데이터 흐름)

```
DeviceListFragment  → DeviceListViewModel
SensorDetailFragment → SensorDetailViewModel
SystemHealthFragment → SystemHealthViewModel
SettingsFragment     → SettingsViewModel
```

#### ViewModel
- UI와 데이터 레이어 사이의 중재자
- `LiveData`를 통해 UI에 상태 노출 (생명주기 인식)
- `Handler + Runnable`로 주기적 폴링 관리
- Activity/Fragment 재생성 시에도 데이터 유지

```java
// 예시: SensorDetailViewModel 폴링 구조
private final Handler handler = new Handler(Looper.getMainLooper());
private final Runnable pollRunnable = new Runnable() {
    @Override
    public void run() {
        loadData();
        handler.postDelayed(this, POLL_INTERVAL_MS); // 5초
    }
};
```

#### Repository
- 데이터 출처(API, DB 등)를 ViewModel로부터 숨김
- Retrofit 콜백 → LiveData 변환 처리
- 현재 구현: Remote API 전용 (로컬 캐시 없음)

#### Model
- 순수 Java POJO (Plain Old Java Object)
- GSON 자동 직렬화/역직렬화
- 비즈니스 로직 없음

### MVVM 데이터 흐름 예시 (센서 상세 화면)

```
1. SensorDetailFragment.onViewCreated()
      │
      ▼
2. SensorDetailViewModel.startPolling(deviceId)
      │
      ▼
3. TemperatureRepository.getLatestTemperature(deviceId)
      │
      ▼
4. ApiService.getTemperature(device_id, limit)  ← Retrofit HTTP GET
      │
      ▼
5. TemperatureResponse (GSON 역직렬화)
      │
      ▼
6. MutableLiveData<TemperatureModel>.postValue(data)
      │
      ▼
7. SensorDetailFragment.observe() → 차트 UI 업데이트
```

---

## 프로젝트 구조

```
IN_GPS/
├── app/
│   └── src/main/
│       ├── java/com/example/in_gps/
│       │   ├── adapter/
│       │   │   └── DeviceAdapter.java          # RecyclerView 어댑터
│       │   ├── api/
│       │   │   ├── ApiService.java             # Retrofit 인터페이스
│       │   │   └── RetrofitClient.java         # Retrofit 싱글톤
│       │   ├── fragment/
│       │   │   ├── DeviceListFragment.java     # 장치 목록 화면
│       │   │   ├── SensorDetailFragment.java   # 센서 상세 차트 화면
│       │   │   ├── SystemHealthFragment.java   # 시스템 전체 현황 화면
│       │   │   └── SettingsFragment.java       # 설정 화면
│       │   ├── model/
│       │   │   ├── DeviceModel.java            # 장치 엔티티
│       │   │   ├── DeviceModelResponse.java    # 장치 목록 API 응답 래퍼
│       │   │   ├── TemperatureModel.java       # 온도 데이터 엔티티
│       │   │   ├── TemperatureResponse.java    # 온도 API 응답 래퍼
│       │   │   ├── DeviceLogModel.java         # 장치 로그 엔티티
│       │   │   └── DeviceLogResponse.java      # 로그 API 응답 래퍼
│       │   ├── repository/
│       │   │   ├── DeviceRepository.java       # 장치 데이터 접근
│       │   │   └── TemperatureRepository.java  # 온도 데이터 접근
│       │   ├── screen/
│       │   │   ├── MainActivity.java           # 메인 액티비티 (하단 내비게이션)
│       │   ├── utils/                          # 유틸리티 (예정)
│       │   └── viewmodel/
│       │       ├── DeviceListViewModel.java    # 장치 목록 ViewModel
│       │       ├── SensorDetailViewModel.java  # 센서 상세 ViewModel
│       │       ├── SystemHealthViewModel.java  # 시스템 현황 ViewModel
│       │       └── SettingsViewModel.java      # 설정 ViewModel
│       └── res/
│           ├── drawable/                       # 벡터 아이콘 & 도형
│           ├── layout/                         # XML 레이아웃 7개
│           ├── menu/                           # 하단 내비게이션 메뉴
│           └── values/                         # colors, strings, themes
├── build.gradle                                # 루트 Gradle
├── settings.gradle                             # Gradle 설정
└── README.md
```

---

## 패키지 구조

### `adapter`
RecyclerView에서 장치 목록을 렌더링하는 어댑터.
상태에 따른 색상 코드 처리 포함.

### `api`
- `ApiService` : Retrofit `@GET` 인터페이스 정의
- `RetrofitClient` : 싱글톤 Retrofit 인스턴스. BaseURL 및 OkHttp 인터셉터 설정

### `fragment`
각 탭/화면에 대응하는 Fragment 클래스.
ViewModel의 LiveData를 observe하여 UI를 갱신함.

### `model`
API 응답을 매핑하는 POJO 클래스.
GSON 어노테이션(`@SerializedName`) 사용.

### `repository`
API 호출을 추상화하여 ViewModel에 데이터를 제공.
콜백 패턴 또는 LiveData 반환 방식 사용.

### `screen`
Activity 클래스 및 OpenGL 렌더러.
Fragment 컨테이너 역할의 MainActivity 포함.

### `viewmodel`
UI 상태와 비즈니스 로직을 관리.
Handler 기반 폴링 시작/중지 메서드 제공.

---

## API 명세

**Base URL** : `http://13.209.92.219:8000/`

| Method | Endpoint | Parameters | 설명 |
|--------|----------|------------|------|
| GET | `/devices` | - | 전체 장치 목록 조회 |
| GET | `/temperature` | `device_id`, `limit` | 최신 N개 온도 데이터 조회 |
| GET | `/temperature/chart` | `device_id`, `days` | 기간별 집계 온도 데이터 조회 |
| GET | `/devices/{device_id}/logs` | - | 장치 이벤트 로그 조회 |

### 데이터 집계 기준

| 기간 | 집계 방식 |
|------|---------|
| 1일 (1D) | 시간별 raw 데이터 |
| 7일 (7D) | 일별 집계 |
| 30일 (30D) | 일별 평균+최대 |
| 1년 (1Y) | 월별 평균+최대 |

---

## 데이터 모델

### DeviceModel
```java
String device_id       // 장치 고유 ID
String equipment_id    // 설비 ID
String status          // "normal" | "warning" | "disconnect"
String installed_on    // 설치일
String last_seen_at    // 마지막 통신 시각
String created_at
String updated_at
```

### TemperatureModel
```java
int    id
String device_id
double temp1           // 코어 온도 (°C)
double temp2           // 주변 온도 (°C)
double angle_x         // Roll  (-180° ~ +180°)
double angle_y         // Pitch (-90° ~ +90°)
double angle_z         // Tilt  (-90° ~ +90°)
String event           // "normal" | "warning" | "critical" | "disconnected"
String created_at
// 집계 필드 (chart API)
double temp1_max
double temp2_max
```

### DeviceLogModel
```java
int    log_id
String device_id
int    reboot_count
double temp_out_c
double temp_core_c
String fault_grade
String created_at
```

---

## 화면 구성

### MainActivity
하단 내비게이션 바를 통해 3개 탭 전환:

| 탭 | Fragment | 설명 |
|----|----------|------|
| 기기상태 | DeviceListFragment | 장치 목록 + 상태 표시 |
| 전체현황 | SystemHealthFragment | 시스템 집계 현황 |
| 설정 | SettingsFragment | 폴링 주기 설정 |

### SensorDetailFragment
DeviceListFragment에서 장치 선택 시 이동하는 상세 화면:
- 2채널 라인 차트 (MPAndroidChart)
- 1D / 7D / 30D / 1Y 기간 선택 칩
- 현재 온도 및 이벤트 상태 표시

---

## 개발 환경 설정

### 요구 사항
- Android Studio Hedgehog 이상
- JDK 11 이상
- Android SDK API 36

### 빌드 방법

```bash
# 프로젝트 클론
git clone <repository-url>
cd IN_GPS

# Gradle 빌드
./gradlew assembleDebug

# 또는 Android Studio에서 직접 실행
```

### 네트워크 설정
`res/xml/network_security_config.xml`에서 HTTP 허용 도메인을 관리합니다.
현재 API 서버(`13.209.92.219`)에 대한 cleartext 트래픽이 허용되어 있습니다.

> **주의**: 운영 환경에서는 HTTPS 전환을 권장합니다.

---

## 기여 방법

1. `feature/기능명` 브랜치 생성
2. 변경사항 커밋
3. Pull Request 생성

---

## 라이선스

본 프로젝트는 사내 IoT 모니터링 시스템을 위해 개발되었습니다.
