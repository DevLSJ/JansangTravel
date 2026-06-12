
# ✈️ Jansang Travel

> 여행의 추억을 기록하고, 지도에서 다시 확인하는 Android 여행 기록 앱

`Jansang Travel`은 사용자가 다녀온 여행지를 기록하고, 사진과 메모를 함께 저장하며, 지도에서 여행 위치를 확인할 수 있도록 제작 중인 Android 여행 다이어리 앱입니다.

현재 모바일 프로그래밍 기말 프로젝트 요구사항을 기반으로 기능을 하나씩 구현하고 있으며, 여행 기록의 추가/조회/수정/삭제, 이미지 표시, Google Maps 연동, 마커 표시 기능을 중심으로 개발하고 있습니다.

---

## 📌 프로젝트 개요

| 항목     | 내용                        |
| ------ | ------------------------- |
| 프로젝트명  | Jansang Travel            |
| 개발 목적  | 모바일 프로그래밍 기말 텀프로젝트        |
| 개발 언어  | Kotlin                    |
| 개발 환경  | Android Studio            |
| 주요 기능  | 여행 기록 관리, 사진 기록, 지도 위치 확인 |
| 데이터 저장 | SQLite 기반 로컬 저장소          |
| 지도 기능  | Google Maps API           |

---

## 🚧 현재 개발 상태

| 구분                | 상태   |
| ----------------- | ---- |
| 메인 화면 UI          | 구현 중 |
| 여행 기록 목록          | 개선 중 |
| 여행 상세 화면          | 구현 중 |
| 이미지 출력            | 해결 중 |
| Google Maps 지도 표시 | 해결 중 |
| 마커 생성             | 작업 중 |
| SQLite CRUD       | 작업 중 |
| Room 제거           | 진행 중 |
| 삭제 기능             | 개선 중 |
| APK 빌드            | 검증 중 |

---

## 🧭 주요 기능

### 1. 여행 기록 목록

여행 기록을 목록 형태로 확인할 수 있도록 구현 중입니다.

* 여행지명 표시
* 방문 날짜 표시
* 대표 이미지 또는 썸네일 표시
* 항목 클릭 시 상세 화면 이동
* 목록 갱신 기능 개선 중

---

### 2. 여행 기록 상세 화면

각 여행 기록을 클릭하면 상세 정보를 확인할 수 있도록 구성하고 있습니다.

* 여행지 이미지 표시
* 방문 날짜 표시
* 여행 메모 표시
* 지도 위치 표시
* 수정 화면 이동 기능 작업 중

---

### 3. 여행 기록 추가 및 수정

사용자가 직접 여행 기록을 작성하고 수정할 수 있도록 기능을 구현 중입니다.

* 여행지명 입력
* 방문 날짜 입력
* 메모 입력
* 사진 선택 기능
* 저장 후 목록 반영
* 수정 후 SQLite DB 반영 작업 중

---

### 4. 이미지 리소스 출력

초기에는 이미지 파일명이 한글과 괄호를 포함하고 있어 Android 리소스 참조 문제가 발생했습니다.
현재는 Android 리소스 규칙에 맞게 파일명을 정리하고 있습니다.

| 여행지 | 리소스 파일                  |
| --- | ----------------------- |
| 해운대 | `img_haeundae.jpg`      |
| 한라산 | `img_hallasan.jpg`      |
| 경복궁 | `img_gyeongbokgung.jpg` |

이미지는 `app/src/main/res/drawable-nodpi/`에 배치하여 원본 비율을 안정적으로 유지하도록 개선 중입니다.

---

### 5. Google Maps API 연동

Google Maps API를 활용하여 여행지의 위치를 지도에서 확인할 수 있도록 개발 중입니다.

현재 작업 중인 내용:

* 지도 화면 표시
* 상세 화면 내 지도 표시
* 해운대, 한라산, 경복궁 마커 표시
* 마커 클릭 시 장소 정보 표시
* 지도 흰 화면 문제 해결 중
* API Key 보안 처리 개선 중

```kotlin
val position = LatLng(latitude, longitude)

googleMap.addMarker(
    MarkerOptions()
        .position(position)
        .title(title)
        .snippet(location)
)
```

---

## 🗺️ 기본 여행지 데이터

현재 테스트 및 기본 화면 구성을 위해 아래 여행지를 기준으로 개발하고 있습니다.

| 여행지 | 위치         | 위도      | 경도       |
| --- | ---------- | ------- | -------- |
| 해운대 | 부산광역시 해운대구 | 35.1587 | 129.1604 |
| 한라산 | 제주특별자치도    | 33.3617 | 126.5292 |
| 경복궁 | 서울특별시 종로구  | 37.5796 | 126.9770 |

---

## 🛠️ 기술 스택

| 구분       | 기술                                 |
| -------- | ---------------------------------- |
| Language | Kotlin                             |
| IDE      | Android Studio                     |
| UI       | Android View / Fragment 기반 구조 개선 중 |
| List     | RecyclerView                       |
| Database | SQLiteOpenHelper                   |
| Map      | Google Maps API                    |
| Build    | Gradle                             |

---

## 🧩 구현 포인트

### SQLiteOpenHelper 기반 CRUD 전환

기말 프로젝트 요구사항에 맞추기 위해 기존 DB 구조를 `SQLiteOpenHelper` 기반으로 전환 중입니다.

현재 목표:

* Room 의존성 제거
* `TravelDbHelper` 직접 구현
* 여행 기록 생성, 조회, 수정, 삭제 처리
* 앱 재실행 후에도 데이터 유지

```kotlin
insertTravel()
getAllTravels()
getTravelById()
updateTravel()
deleteTravel()
deleteAllTravels()
```

---

### RecyclerView 기반 목록 구현

여행 기록 목록은 `RecyclerView`를 활용하도록 개선 중입니다.

* Adapter 직접 구현
* ViewHolder 직접 구현
* 항목 클릭 이벤트 처리
* 길게 누르기 메뉴 작업 중

---

### Fragment 전환 및 백스택 관리

앱은 여러 화면을 Fragment 기반으로 전환할 수 있도록 구성하고 있습니다.

현재 개선 중인 부분:

* 최소 2개 이상의 Fragment 구성
* BottomNavigationView 또는 버튼을 통한 화면 전환
* 상세 화면 이동 시 백스택 관리
* 뒤로가기 동작 안정화

---

### 삭제 기능 개선

삭제 기능은 사용자가 실수로 기록을 삭제하지 않도록 확인 과정을 포함해 개선 중입니다.

구현 예정 또는 작업 중인 기능:

* 목록 항목 길게 누르기 삭제
* 톱니바퀴 메뉴에서 기록 선택 삭제
* 수정 화면 내 “이 기록 삭제” 버튼
* 삭제 전 AlertDialog 확인
* 삭제 후 목록, 지도 마커, 카운트 갱신

---

### 카메라/갤러리 사진 기록

여행 기록에 사진을 함께 저장할 수 있도록 카메라 또는 갤러리 Intent 기능을 작업 중입니다.

* 갤러리 이미지 선택
* 카메라 촬영 권한 흐름 확인
* 선택한 이미지 URI 저장
* 상세 화면에서 사진 표시
* 권한 거부 시 예외 처리 개선 중

---

## 🔐 API Key 관리

Google Maps API Key는 공개 저장소에 노출되지 않도록 관리할 예정입니다.

* `local.properties` 사용
* `.gitignore`에 `local.properties` 포함
* README에 실제 API Key 작성 금지
* GitHub 커밋 전 Key 노출 여부 확인

예시:

```properties
MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY
```

---

## 🖼️ 화면 미리보기

> 현재 스크린샷 경로 및 이미지 커밋을 정리 중입니다.

| 메인 화면 | 상세 화면 |
| ----- | ----- |
| 작업 중  | 작업 중  |

| 지도 탭 | 마커 개선 화면 |
| ---- | -------- |
| 해결 중 | 작업 중     |

---

## ⚙️ 실행 방법

1. Android Studio에서 프로젝트 열기

```text
C:\Users\PC\Desktop\모바일프로그래밍_텀프로젝트\jansang-travel-diary
```

2. Gradle Sync 실행

3. `local.properties`에 Google Maps API Key 설정

```properties
MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY
```

4. 에뮬레이터 또는 실제 기기에서 실행

5. APK 빌드

```bash
gradlew.bat clean
gradlew.bat assembleDebug
```

APK 생성 위치:

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## ✅ 요구사항 반영 예정 체크리스트

| 요구사항                     | 상태     |
| ------------------------ | ------ |
| Fragment 2개 이상           | 작업 중   |
| Fragment 백스택 관리          | 개선 중   |
| RecyclerView 사용          | 작업 중   |
| Adapter/ViewHolder 직접 구현 | 작업 중   |
| SQLiteOpenHelper 직접 구현   | 진행 중   |
| CRUD 전체 동작               | 재검증 예정 |
| 추가/수정 Activity           | 확인 중   |
| 옵션 메뉴 2개 이상              | 개선 중   |
| 컨텍스트 메뉴                  | 작업 중   |
| AlertDialog 삭제 확인        | 작업 중   |
| 카메라/갤러리 Intent           | 확인 중   |
| 상세 화면 사진 표시              | 작업 중   |
| 지도 API 활용                | 해결 중   |
| 지도 마커 생성                 | 작업 중   |
| APK 생성                   | 검증 중   |

---

## 📌 향후 개선 사항

* 사용자 직접 여행지 추가 기능 안정화
* 사진 GPS 정보 추출 후 지도 마커 생성
* 지도 화면 UI 개선
* 기록 정렬 및 검색 기능 추가
* 전체 삭제 및 선택 삭제 기능 강화
* 앱 정보 Dialog 문구 정리
* APK 제출 전 전체 CRUD 재검증

---

## 👨‍💻 Developer

| 항목         | 내용                 |
| ---------- | ------------------ |
| 개발자        | DevLSJ             |
| 프로젝트       | Jansang Travel     |
| 목적         | 모바일 프로그래밍 기말 텀프로젝트 |
| Repository | JansangTravel      |

---

## 📝 개발 메모

이 프로젝트는 모바일 프로그래밍 수업에서 학습한 Android 핵심 기능을 실제 앱으로 구현하기 위한 개인 기말 프로젝트입니다.

현재는 요구사항을 기준으로 기능을 보완하는 단계이며, 최종적으로는 SQLite 기반 CRUD, RecyclerView 목록, Fragment 전환, 사진 기록, 지도 마커 기능이 모두 정상 동작하는 APK 제출을 목표로 하고 있습니다.


