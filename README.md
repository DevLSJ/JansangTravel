# Jansang Travel

> 여행의 추억을 기록하고 지도에서 위치를 확인하는 Android 여행 다이어리 앱

## 프로젝트 소개

Jansang Travel은 모바일 프로그래밍 기말 프로젝트로 제작한 Android 앱입니다. 사용자는 여행지명, 방문 날짜, 메모, 대표 사진, 위치 좌표를 저장하고, 기록된 여행지를 목록과 지도에서 확인할 수 있습니다.

대표 여행지 데이터로 해운대, 한라산, 경복궁을 기본 제공하며, Google Maps 지도와 마커를 통해 여행 위치를 직관적으로 확인할 수 있습니다.

## 주요 기능

- Fragment 2개 이상 구성: 여행 기록 목록, 추억 지도
- BottomNavigationView 기반 Fragment 전환 및 백스택 처리
- RecyclerView 기반 여행 기록 목록
- Adapter/ViewHolder 직접 구현
- 여행 기록 추가, 상세 조회, 수정, 단일 삭제, 전체 삭제
- SQLiteOpenHelper 기반 로컬 DB 저장
- 카메라 촬영 및 갤러리 이미지 선택
- 사진 EXIF GPS 정보 추출
- Google Maps 지도 표시 및 마커 생성
- 옵션 메뉴: 최신순, 오래된순, 제목순, 전체 삭제, 앱 정보
- 컨텍스트 메뉴: 항목 길게 누르기 후 수정/삭제

## 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| Language | Kotlin |
| IDE | Android Studio |
| UI | XML Layout, ViewBinding, Material Components |
| List | RecyclerView |
| Fragment | AndroidX Fragment |
| Database | SQLiteOpenHelper |
| Async | Kotlin Coroutines |
| Map | Google Maps Android SDK |
| Image | Camera Intent, Gallery Intent, FileProvider, ExifInterface |
| Build | Gradle Kotlin DSL |

## 화면 미리보기

| 메인 화면 | 지도 탭 | 상세 화면 |
| --- | --- | --- |
| <img src="./verification_main.png" width="240" alt="메인 화면" /> | <img src="./verification_map.png" width="240" alt="지도 탭" /> | <img src="./verification_detail.png" width="240" alt="상세 화면" /> |

## 프로젝트 구조

```text
JansangTravel/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/
│       │   ├── MainActivity.kt
│       │   ├── AddEditActivity.kt
│       │   ├── DetailActivity.kt
│       │   ├── adapter/TravelAdapter.kt
│       │   ├── db/
│       │   │   ├── TravelDbHelper.kt
│       │   │   ├── RecordEntity.kt
│       │   │   ├── RecordRepository.kt
│       │   │   └── RecordViewModel.kt
│       │   ├── fragment/
│       │   │   ├── TravelListFragment.kt
│       │   │   └── TravelMapFragment.kt
│       │   └── util/
│       │       ├── ExifGpsExtractor.kt
│       │       ├── MapsApiKeyValidator.kt
│       │       └── RecordImageLoader.kt
│       └── res/
│           ├── drawable-nodpi/
│           │   ├── img_haeundae.jpg
│           │   ├── img_hallasan.jpg
│           │   └── img_gyeongbokgung.jpg
│           ├── layout/
│           └── menu/
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Google Maps API Key 설정

API Key는 GitHub에 커밋하지 않는 `local.properties` 또는 `.env`에 설정합니다.

```properties
sdk.dir=C\:\\Users\\PC\\AppData\\Local\\Android\\Sdk
MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY
```

`AndroidManifest.xml`은 Gradle에서 생성한 `@string/google_maps_key`를 참조합니다.

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="@string/google_maps_key" />
```

## 실행 방법

1. Android Studio에서 프로젝트 폴더를 엽니다.
2. `local.properties`에 Android SDK 경로와 `MAPS_API_KEY`를 설정합니다.
3. Gradle Sync를 실행합니다.
4. Google Play services가 포함된 에뮬레이터 또는 실제 기기에서 실행합니다.

## APK 빌드 방법

```powershell
cd C:\Users\PC\Desktop\모바일프로그래밍_텀프로젝트\jansang-travel-diary
.\gradlew.bat clean assembleDebug
```

생성 위치:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 제출 전 주의사항

- `local.properties`, `.env`, `debug.keystore`, `build/`, `.gradle/`, `.idea/`는 GitHub에 올리지 않습니다.
- Google Maps API Key가 이미 공개 저장소에 올라갔다면 키를 재발급하고 기존 키를 제한 또는 폐기하는 것을 권장합니다.
- 다른 PC에서 다시 빌드할 경우 해당 PC의 debug SHA-1과 package name을 Google Cloud Console API Key 제한에 등록해야 합니다.
