<div align="center">

# TodayEng Backend

### 오늘의 나를 아는 질문으로 시작하는 영어 스피킹

오늘의 기록과 일상을 바탕으로 나만의 영어 질문을 만들고,
영어로 하루를 회고할 수 있도록 돕는 **TodayEng의 Backend Repository**입니다.

<br/>

<a href="https://todayeng-fe.vercel.app/">
  <img src="https://img.shields.io/badge/TodayEng-Visit_Service-7C4DCC?style=flat-square"/>
</a>
<a href="https://github.com/Today-Eng">
  <img src="https://img.shields.io/badge/GitHub-Today--Eng-181717?style=flat-square&logo=github&logoColor=white"/>
</a>

</div>

<br/>

## 🛠 Tech Stack

### Backend

<p>
  <img src="https://img.shields.io/badge/Java_17-007396?style=flat-square&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring_Boot_3.3.4-6DB33F?style=flat-square&logo=springboot&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat-square&logo=spring&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white"/>
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white"/>
  <img src="https://img.shields.io/badge/JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white"/>
  <img src="https://img.shields.io/badge/OAuth_2.0-4285F4?style=flat-square&logo=google&logoColor=white"/>
  <img src="https://img.shields.io/badge/ShedLock-6DB33F?style=flat-square"/>
</p>

### AI & External Services

<p>
  <img src="https://img.shields.io/badge/Gemini_2.5_Flash-8E75B2?style=flat-square&logo=googlegemini&logoColor=white"/>
  <img src="https://img.shields.io/badge/Google_Cloud_STT/TTS-4285F4?style=flat-square&logo=googlecloud&logoColor=white"/>
  <img src="https://img.shields.io/badge/Google_Calendar_API-4285F4?style=flat-square&logo=googlecalendar&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spotify_Web_API-1ED760?style=flat-square&logo=spotify&logoColor=white"/>
  <img src="https://img.shields.io/badge/Open--Meteo-5B9BD5?style=flat-square"/>
</p>

### Infrastructure & DevOps

<p>
  <img src="https://img.shields.io/badge/Google_Compute_Engine-4285F4?style=flat-square&logo=googlecloud&logoColor=white"/>
  <img src="https://img.shields.io/badge/Cloud_Load_Balancing-4285F4?style=flat-square&logo=googlecloud&logoColor=white"/>
  <img src="https://img.shields.io/badge/Cloud_SQL-4285F4?style=flat-square&logo=googlecloud&logoColor=white"/>
  <img src="https://img.shields.io/badge/Amazon_S3-569A31?style=flat-square&logo=amazons3&logoColor=white"/>
  <img src="https://img.shields.io/badge/Nginx-009639?style=flat-square&logo=nginx&logoColor=white"/>
  <img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white"/>
  <img src="https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white"/>
  <img src="https://img.shields.io/badge/Cloud_Logging-4285F4?style=flat-square&logo=googlecloud&logoColor=white"/>
</p>

<br/>

## ✨ Features

|     | Feature                    | Description                                            |
| --- | -------------------------- | ------------------------------------------------------ |
| 🧠  | **Personalized Questions** | 오늘의 기록과 과거 회고를 활용해 개인화된 영어 질문을 생성합니다.                  |
| 🎙️ | **AI Speaking Reflection** | 음성·텍스트 답변을 분석하고 자연스러운 영어 표현과 교정 이유를 제공합니다.             |
| 🔗  | **External Context**       | Google Calendar, Spotify, 날씨, 사진 등의 데이터를 회고 소재로 활용합니다. |
| 📖  | **Diary & Memory**         | 완성된 회고를 기록하고 과거 회고에서 추출한 사용자 기억을 다음 질문 생성에 활용합니다.      |
| 🔔  | **Web Push**               | 작성 기록과 리마인더를 통해 꾸준한 영어 회고를 지원합니다.                      |

<br/>

## 🏗 System Architecture

<div align="center">

<img width="100%" alt="system-architecture" src="https://github.com/user-attachments/assets/0a5bd6a7-f972-44f2-a042-b712318cb6eb" />

</div>

Backend는 Google Compute Engine의 **Blue / Green 두 환경**에서 운영되며, GCP Load Balancer를 통해 활성 환경으로 트래픽을 전달합니다.

정형 데이터는 **Cloud SQL for MySQL**, 사진과 음성 등의 비정형 데이터는 **Amazon S3**에 저장합니다.

<br/>

## 👩‍💻 Backend Team

## 👩‍💻 Backend Team

<table>
  <tr>
    <td align="center">
      <div style="width:220px">
        <a href="https://github.com/riveryunny">
          <img src="https://github.com/riveryunny.png" width="110px;" alt="이가윤"/>
          <br/>
          <b>이가윤</b>
        </a>
      </div>
    </td>
    <td align="center">
      <div style="width:220px">
        <a href="https://github.com/ownue">
          <img src="https://github.com/ownue.png" width="110px;" alt="이은우"/>
          <br/>
          <b>이은우</b>
        </a>
      </div>
    </td>
    <td align="center">
      <div style="width:220px">
        <a href="https://github.com/hyeonky0w0">
          <img src="https://github.com/hyeonky0w0.png" width="110px;" alt="이현경"/>
          <br/>
          <b>이현경</b>
        </a>
      </div>
    </td>
  </tr>
  <tr>
    <td align="center">
      홈 · 회고 아카이브<br/>
      웹 푸시 알림
    </td>
    <td align="center">
      회고 컨텍스트 · 사용자 메모리<br/>
      외부 서비스 연동 · CI/CD
    </td>
    <td align="center">
      인증 · 마이페이지<br/>
      음성 회고 · 실시간 응답
    </td>
  </tr>
</table>

<br/>

---

## 🔥 Convention

### 🛠 Build Info

* Language : Java 17
* Framework : Spring Boot 3.3.4
* Database : MySQL

### 🔐 Environment

> `src/main` 하위에 `resources` 디렉토리와 `application.yml`을 생성해주세요.

#### 인증 환경 변수

* `JWT_SECRET`: HS256 서명용 32바이트 이상의 비밀키
* `GOOGLE_CLIENT_ID`: Google OAuth 웹 클라이언트 ID
* 로컬 실행 시 프로젝트 루트의 `.env`를 자동으로 읽습니다.
* 인증 API는 `POST /api/auth/google` 요청 바디로 `{"idToken":"..."}`을 받습니다.

<details>
<summary><b>오디오 저장소 설정</b></summary>

운영 환경에서는 private S3 버킷을 사용합니다.

```env
AUDIO_STORAGE_TYPE=s3
AUDIO_S3_BUCKET=todayeng-prod-media
AWS_REGION=ap-northeast-2
AWS_ACCESS_KEY_ID=발급받은-access-key-id
AWS_SECRET_ACCESS_KEY=발급받은-secret-access-key
AUDIO_S3_TTS_PREFIX=tts
AUDIO_S3_STT_PREFIX=stt
AUDIO_PLAYBACK_URL_EXPIRATION=30m
```

운영 서버는 GCP Compute Engine을 사용하므로 AWS 자격 증명은 GCP VM의 `/opt/todayeng/.env`에 주입하며 저장소나 Docker 이미지에는 포함하지 않습니다.

S3 버킷은 Block Public Access가 활성화된 private 상태로 유지하며, TTS 재생 시 Presigned URL을 생성합니다.

</details>

### 📋 Commit Convention

| type       | branch                | description       |
| ---------- | --------------------- | ----------------- |
| `feat`     | `feat/#ISSUE_NUM`     | ⚡️ 새로운 기능 추가      |
| `fix`      | `fix/#ISSUE_NUM`      | 🐛 버그 수정          |
| `docs`     | `docs/#ISSUE_NUM`     | 📝 문서 수정          |
| `refactor` | `refactor/#ISSUE_NUM` | ♻️ 리팩토링           |
| `test`     | `test/#ISSUE_NUM`     | 🧪 테스트 코드 작성      |
| `chore`    | `chore/#ISSUE_NUM`    | 🛠️ 빌드, 패키지 관련 수정 |
| `perf`     | `perf/#ISSUE_NUM`     | 🪄 성능 개선          |
| `ci`       | `ci/#ISSUE_NUM`       | 🔄 CI 관련 수정       |
| `cd`       | `cd/#ISSUE_NUM`       | 🔄 CD 관련 수정       |
| `revert`   | `revert/#ISSUE_NUM`   | ⚠️ 특정 커밋으로 되돌리기   |

### 📌 Git Branch Strategy

| branch    | role                                   |
| --------- | -------------------------------------- |
| `main`    | 최종 배포용 브랜치<br/>`develop`에서 안정화된 버전만 병합 |
| `develop` | 개발용 브랜치                                |
