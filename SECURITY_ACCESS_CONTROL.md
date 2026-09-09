# 인증·인가 및 IDOR 접근 경로

## 신뢰 경계

- 사용자 식별자는 요청 값이 아니라 JWT 인증 후 생성된 `AuthenticationPrincipal`의 `userId`를 사용한다.
- `diaryId`, `questionId`, `answerId`는 신뢰하지 않는 입력이며 인증 사용자 및 상위 리소스와의 관계를 함께 검증한다.
- 다른 사용자의 Diary는 `A004 ACCESS_DENIED`로 거부한다. Question 또는 Answer가 요청 Diary에 속하지 않으면 `D018 QUESTION_NOT_FOUND` 또는 `D025 ANSWER_NOT_FOUND`로 처리한다.

## Diary 리소스 접근 경로

| HTTP 경로 | 소유권 검증 위치 |
|---|---|
| `POST /api/diaries` | `DiaryService`가 Principal 사용자 기준으로 생성·재개 |
| `GET /api/diaries` | `DiaryQueryService`의 사용자 조건 조회 |
| `GET /api/diaries/{diaryId}` | `DiaryQueryService.findByIdAndUserIdAndStatus` |
| `POST /api/diaries/{diaryId}/contexts` | `DiaryContextService.findByIdAndUserId` 및 persistence claim |
| `POST /api/diaries/{diaryId}/reflection-sessions` | `ReflectionQuestionPersistenceService.findByIdAndUserId` |
| `GET /api/diaries/{diaryId}/questions`, `questions/next` | `DiaryQuestionQueryService.ownedDiary` |
| `POST /api/diaries/{diaryId}/questions/{questionId}/answers` | `AnswerPersistenceService.findByIdAndDiaryIdAndDiaryUserId` |
| `GET /api/diaries/{diaryId}/answers` | `DiaryAnswerQueryService.validateOwner` |
| `GET /api/diaries/{diaryId}/answers/{answerId}` | Diary 소유권 및 Answer-Diary 결속 확인 |
| `GET /api/diaries/{diaryId}/subscribe` | `DiarySubscriptionService.existsByIdAndUserId` |
| `PATCH /api/diaries/{diaryId}/complete` | `DiaryCompletionService`의 잠금 조회 후 사용자 비교 |
| `PATCH /api/diaries/{diaryId}/memo` | `DiaryMemoService`의 잠금 조회 후 사용자 비교 |
| `PATCH /api/diaries/{diaryId}/pause` | `DiaryPauseService`의 잠금 조회 후 사용자 비교 |
| `DELETE /api/diaries/{diaryId}` | `DiaryDeletionService`의 잠금 조회 후 사용자 비교 |

비동기 STT·교정은 persistence 단계에서 `userId`, `diaryId`, `questionId`, `answerId` 결속을 다시 확인한다.

## 공개 Endpoint

- `GET /health`
- `/files/audio/**`
- `POST /api/auth/google`, `POST /api/auth/refresh`
- `POST /api/auth/test`: `local` 프로필에만 컨트롤러 Bean 존재
- `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`
- `GET /api/external-accounts/*/callback`
- `POST /api/external-accounts/*/authorization`: 관련 설정이 true인 환경만 허용
- CORS preflight `OPTIONS /**`, 비동기·오류 dispatcher

그 외 Endpoint는 `anyRequest().authenticated()` 정책으로 인증이 필요하다.
