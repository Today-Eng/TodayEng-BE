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
- 인증 API는 `POST /auth/google` 요청 바디로 `{"idToken":"..."}`을 받습니다.
- 
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
