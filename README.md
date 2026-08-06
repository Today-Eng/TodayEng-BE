# 🔥 Project Convention
> `src/main` 하위에 `resources` 디렉토리와 `application.yml`를 생성해주세요!'
> 
## 🛠️ Build Info
- **Language** : Java 17
- **Framework** : Spring boot 3.3.4
- **Database** : MySQL

## 인증 환경 변수

- `JWT_SECRET`: HS256 서명용 32바이트 이상의 비밀키
- `GOOGLE_CLIENT_ID`: Google OAuth 웹 클라이언트 ID
- 로컬 실행 시 프로젝트 루트의 `.env`를 자동으로 읽습니다. 값이 없으면 애플리케이션이 시작되지 않습니다.
- 인증 API는 `POST /api/auth/google` 요청 바디로 `{"idToken":"..."}`을 받습니다.
- 

## 오디오 저장소

기본값은 로컬 저장소이며 운영 환경에서는 private S3 버킷 사용을 권장합니다.

```dotenv
AUDIO_STORAGE_TYPE=s3
AUDIO_S3_BUCKET=todayeng-prod-media
AWS_REGION=ap-northeast-2
AUDIO_S3_TTS_PREFIX=tts
AUDIO_S3_STT_PREFIX=stt
AUDIO_PLAYBACK_URL_EXPIRATION=30m
```

AWS 인증 정보는 설정 파일에 저장하지 않고 EC2 Instance Profile, ECS Task Role,
EKS IRSA 또는 로컬 AWS profile 등 AWS SDK 기본 자격 증명 체인을 사용합니다.
애플리케이션 역할에는 버킷의 `tts/*`, `stt/*`에 대한
`s3:PutObject`, `s3:GetObject`, `s3:DeleteObject` 권한이 필요합니다.

버킷은 Block Public Access를 활성화한 private 상태로 유지합니다. TTS 재생 URL은
API 응답 시 presigned URL로 생성되며 DB에는 객체 키만 저장됩니다. STT 원본은 변환
성공 후 삭제되고, 장애로 남은 파일을 정리하도록 `stt/` prefix에 1~7일 Lifecycle
만료 정책을 추가하는 것을 권장합니다.

## 📋 Commit Convention
| type       | name                    | description     |
|------------|-------------------------|-----------------|
| `feat`     | `feat/￼#ISSUE_NUM￼`     | ⚡️ 새로운 기능 추가     |
| `fix`      | `fix/￼#ISSUE_NUM￼`      | 🐛 버그 수정         |
| `docs`     | `docs/￼#ISSUE_NUM￼`     | 📝 문서 수정         |
| `refactor` | `refactor/￼#ISSUE_NUM￼` | ♻️ 리팩토링          |
| `test`     | `test/￼#ISSUE_NUM￼`     | 🧪 테스트 코드 작성     |
| `chore`    | `chore/￼#ISSUE_NUM￼`    | 🛠️ 빌드, 패키지 관련 수정 |
| `perf`     | `perf/￼#ISSUE_NUM￼`     | 🪄 성능 개선         |
| `ci`       | `ci/￼#ISSUE_NUM￼`       | 🔄 CI 관련 수정      |
| `cd`       | `cd/￼#ISSUE_NUM￼`       | 🔄 CD 관련 수정      |
| `revert`   | `revert/￼#ISSUE_NUM￼`   | ⚠️ 특정 커밋으로 되돌리기  |

## 📌 Git Branch Strategy
| branch    | role                                   |
|-----------|----------------------------------------|
| `main`    | - 최종 배포용 브랜치<br>- dev 브랜치에서 안정화 버전만 병합 |
| `develop` | - 개발용 브랜치<br>- 자유롭게 병합                 |
