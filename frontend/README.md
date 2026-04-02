# Frontend — React 18 (Vite)

## 개요

Vite + React 18 기반 SPA입니다.  
데이터셋 업로드, 작업 목록, 평가 결과 화면으로 구성되며 SSE로 실시간 진행률을 표시합니다.

## 페이지 구성

| 경로 | 컴포넌트 | 설명 |
|------|----------|------|
| `/` | `UploadPage` | CSV 파일 업로드 → 즉시 작업 제출 → 결과 페이지로 이동 |
| `/jobs` | `JobListPage` | 내 평가 작업 목록 및 상태 |
| `/jobs/:jobId/result` | `ResultPage` | SSE 진행률 + 완료 후 결과 표시 |

## 주요 컴포넌트

### `ProgressBar.jsx`
SSE(`EventSource`)로 `/api/jobs/{jobId}/stream`에 연결합니다.  
상태가 `DONE` 또는 `FAILED`가 되면 자동으로 연결을 종료하고 `onDone` 콜백을 호출합니다.

| status 값 | 표시 텍스트 |
|-----------|------------|
| `PENDING` | 대기 중 |
| `EVALUATING` | 데이터 평가 중 |
| `SCORING` | 점수 계산 중 |
| `GENERATING_REPORT` | 보고서 생성 중 |
| `DONE` | 완료 |
| `FAILED` | 실패 |

### `ReportViewer.jsx`
`GET /api/jobs/{jobId}/result` 응답(평가 결과 목록)을 테이블로 렌더링합니다.

## 로컬 실행

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
```

`vite.config.js`에 API 프록시가 설정되어 있어 `/api/*` 요청이 `localhost:8080`으로 전달됩니다.

## 빌드

```bash
npm run build      # dist/ 디렉토리에 정적 파일 생성
npm run preview    # 빌드 결과물 미리보기
```

## API 통신

`axios`를 사용합니다. 기본 URL 설정 없이 상대 경로(`/api/...`)를 사용하므로  
개발 환경에서는 Vite 프록시, 프로덕션에서는 Nginx가 요청을 백엔드로 전달합니다.

## 향후 작업

- [ ] 인증 구현 후 `userId` 파라미터를 `@AuthenticationPrincipal`로 교체
- [ ] 보고서 뷰어에 LLM 생성 텍스트(요약·강점·약점·제안) 추가 렌더링
- [ ] 에러 상태 화면 처리
