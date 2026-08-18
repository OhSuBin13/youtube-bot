# 영상 주제 기반 댓글을 위한 정보 수집 설계

검증일: 2026-08-18 (Asia/Seoul)

## 결론

공개된 타인 영상에서 안정적이고 정책에 맞게 수집할 수 있는 핵심 문맥은 `영상 메타데이터 + 채널 문맥 + 공개 댓글 표본`입니다. 영상 대본은 가장 강한 신호지만, 공식 API에서 자막 본문 다운로드는 영상 편집 권한이 있는 사용자에게만 허용됩니다. 따라서 대본은 본인 소유 영상의 OAuth 승인 자막 또는 사용자가 직접 제공한 파일만 받아야 합니다.

댓글 게시 기능은 수집 기능과 분리합니다. YouTube Developer Policies는 사용자의 사전·구체적·명시적 동의 없이 댓글을 자동 실행하는 것을 금지합니다. 한 번의 포괄적 동의로 여러 영상에 연속 게시하는 방식이 아니라, 게시할 영상·채널·댓글 문안을 화면에 보여주고 각 실행 직전에 승인받는 흐름이 안전합니다.

## 우선순위와 용도

| 우선순위 | 정보 | 공식 수집 경로 | 댓글 초안에서의 용도 | 주의점 |
|---|---|---|---|---|
| 1 | 제목, 설명, 태그 | `videos.list(part=snippet)` | 핵심 주제, 고유명사, 제작자가 강조한 관점 | 설명의 링크·홍보 문구를 그대로 댓글에 복사하지 않음 |
| 1 | 대본/자막 본문 | 사용자 파일 또는 편집 권한이 있는 영상의 `captions.download` | 실제 주장, 사례, 세부 내용 확인 | 공개 타인 영상은 공식 API로 다운로드 불가; 스크래핑 금지 |
| 2 | 카테고리와 YouTube 제공 채널 주제 | `videoCategories.list`, `channels.list(part=topicDetails)` | 넓은 분야와 어조 결정 | 자체 카테고리 점수나 YouTube가 주지 않은 지표로 표시하지 않음 |
| 2 | 관련도순 공개 댓글 | `commentThreads.list(order=relevance)` | 시청자가 주목한 부분·이미 반복된 표현 파악 | 댓글의 주장을 영상 사실로 취급하지 않음; 문구 표절 금지 |
| 2 | 최신순 공개 댓글 | `commentThreads.list(order=time)` | 최근 논의와 중복 댓글 회피 | 짧은 유행어·스팸에 과적합하지 않음 |
| 3 | 채널명·설명·키워드 | `channels.list` | 채널의 전문 분야와 통상 어조 보조 | 채널 전체의 성향을 개인 속성으로 추정하지 않음 |
| 3 | 기본 언어·오디오 언어·게시 시각·길이 | `videos.list` | 댓글 언어와 적절한 구체성 선택 | 언어가 없으면 자동 단정하지 말고 사용자 설정 사용 |
| 4 | 썸네일 URL | `videos.list(part=snippet)` | 사람이 검토 화면에서 대상 영상 확인 | 썸네일만 보고 영상 내용을 사실로 단정하지 않음 |

조회수, 구독자 수, 전체 좋아요 수는 댓글의 내용 적합도를 높이는 신호가 아니므로 기본 수집에서 제외합니다. 댓글 좋아요 수는 관련도순 표본 안에서 검토 우선순위를 보조하기 위해 원값 그대로만 저장합니다.

## 공식 API 요청 묶음

한 영상 기준 기본 요청은 다음과 같습니다.

1. `videos.list(part=snippet,contentDetails,status&id=...)`: 영상과 채널 ID 확보. 비용 1 unit.
2. `channels.list(part=snippet,brandingSettings,topicDetails&id=...)`: 채널 문맥 확보. 비용 1 unit.
3. `videoCategories.list(part=snippet&id=...)`: 카테고리 표시명 확보. 비용 1 unit.
4. `commentThreads.list(part=snippet&videoId=...&order=relevance&textFormat=plainText)`: 관련 댓글 표본. 비용 1 unit.
5. `commentThreads.list(...&order=time)`: 최신 댓글 표본. 비용 1 unit.

댓글이 비활성화된 영상은 `commentsDisabled`를 정상적인 수집 결과로 기록하면 됩니다. `commentThread`의 `replies` 부분에는 모든 답글이 보장되지 않으므로, 전체 답글이 꼭 필요할 때만 `comments.list`를 추가 호출하고 일반적인 주제 파악에는 최상위 댓글과 답글 수만 활용하는 편이 효율적입니다.

자막은 별도 권한 경로입니다. `captions.list`는 50 units이고 OAuth 승인이 필요하며 실제 자막 본문을 반환하지 않습니다. `captions.download`는 200 units이고 영상 편집 권한이 필요합니다. 공개 영상의 `contentDetails.caption=true`는 자막 존재 여부일 뿐 다운로드 권한을 뜻하지 않습니다.

## 생성기에 전달할 때의 규칙

수집 JSON은 원자료이지 사실 판정 결과가 아닙니다. 다음 규칙을 초안 생성 단계에 별도로 적용해야 합니다.

- 영상 제목·설명·대본을 1차 근거로 쓰고, 공개 댓글은 “시청자 반응”으로만 분리한다.
- 영상에 없는 구체적 사실, 시청한 척하는 타임스탬프, 개인 경험을 만들어내지 않는다.
- 기존 댓글의 문장을 복사하거나 가벼운 변형으로 재사용하지 않는다.
- 같은 계정이 반복해서 사용할 고정 문구를 만들지 않는다.
- 칭찬만 있는 범용 문장보다, 확인 가능한 한 가지 지점에 대한 짧은 감상이나 질문을 제안한다.
- 정치·건강·금융 등 고위험 주제는 자동 게시하지 않고 강화된 사람 검토로 보낸다.
- 대본이 없고 제목·설명이 빈약하면 “문맥 부족”으로 판단해 게시하지 않는다.

예시 품질 게이트:

```json
{
  "targetVideoId": "확정된 영상 ID",
  "draftText": "사용자에게 보여줄 정확한 댓글 문안",
  "evidenceFields": ["video.title", "video.description"],
  "duplicateCheck": "passed",
  "safetyReview": "passed",
  "approvedByUser": true,
  "approvedAt": "게시 직전 시각"
}
```

`approvedByUser=true`는 서버가 임의로 채우는 값이 아니라, 사용자가 대상 영상과 최종 문안을 본 뒤 수행한 명시적 승인 이벤트에서만 생성해야 합니다.

## 저장·개인정보·정책 가드

- API key와 OAuth token은 결과 JSON, 로그, 저장소에 기록하지 않는다.
- 공개 댓글 작성자의 이름, 프로필 이미지, 채널 ID는 수집하지 않는다.
- 공개 API 데이터는 최대 30일까지만 임시 저장하고 그 전에 API로 갱신하거나 삭제한다.
- 사용자 승인으로 얻은 데이터는 동의 범위 안에서만 사용하고, 철회·삭제 요청 경로를 제공한다.
- YouTube 웹 페이지나 비공식 endpoint를 스크래핑하지 않는다.
- 댓글 게시 endpoint는 OAuth `youtube.force-ssl`이 필요하고 `commentThreads.insert` 1회 비용은 50 units이다.
- UI에는 데이터 출처가 YouTube임을 명확히 표시하고, 모델이 만든 초안은 YouTube 제공 데이터로 오인되지 않게 구분한다.

## 공식 근거

- [Videos: list](https://developers.google.com/youtube/v3/docs/videos/list) — `snippet`, `contentDetails`, `status`와 1-unit 조회 비용
- [Video resource](https://developers.google.com/youtube/v3/docs/videos) — 제목, 설명, 태그, 언어, 길이, 자막 존재 여부 등 필드 정의
- [CommentThreads: list](https://developers.google.com/youtube/v3/docs/commentThreads/list) — 관련도/최신순, plain text, 최대 100개, 1-unit 비용, 댓글 비활성 오류
- [Comments implementation guide](https://developers.google.com/youtube/v3/guides/implementation/comments) — thread에 모든 답글이 포함되지 않을 수 있음
- [Channels resource](https://developers.google.com/youtube/v3/docs/channels) — 채널 설명, 키워드, YouTube 제공 주제
- [Captions: list](https://developers.google.com/youtube/v3/docs/captions/list) — OAuth 필요, 본문 미포함, 50-unit 비용
- [Captions: download](https://developers.google.com/youtube/v3/docs/captions/download) — 영상 편집 권한 필요, 200-unit 비용
- [CommentThreads: insert](https://developers.google.com/youtube/v3/docs/commentThreads/insert) — OAuth 범위와 50-unit 비용
- [YouTube API Services Developer Policies](https://developers.google.com/youtube/terms/developer-policies) — 명시적 동의, 스크래핑 금지, 30일 저장 제한, 출처 표시
