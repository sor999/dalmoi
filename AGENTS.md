# 달모이(Dalmoi) Monorepo Harness

## 프로젝트 구성과 작업 라우팅

이 저장소는 Android 앱과 Spring Boot 서버를 함께 관리합니다. 빌드와 배포는 프로젝트별로 독립적으로 수행합니다.

- 서버 구현·설계·테스트·리뷰: 먼저 [서버 지침](backend/AGENTS.md)을 읽고 [Spring Boot Skill](backend/.agents/skills/spring-boot/SKILL.md)을 사용합니다.
- Android 작업: [Android 지침](android/AGENTS.md)을 따릅니다. Spring Boot 컨벤션을 적용하지 않습니다.
- UI 디자인·구현 작업: [디자인 가이드](DESIGN.md)의 색상·타이포그래피·간격·형태·공통 스타일 원칙을 참고합니다.
- 커밋 작성: [Commit Skill](.agents/skills/commit/SKILL.md)을 따릅니다.
- 이슈 작성: [Issue Skill](.agents/skills/issue/SKILL.md)과 [이슈 템플릿](.github/ISSUE_TEMPLATE/custom.md)을 따릅니다.
- PR 작성: [PR Skill](.agents/skills/pr/SKILL.md)과 [PR 템플릿](.github/pull_request_template.md)을 따릅니다.
- PR 머지: [Merge 에이전트](.codex/agents/merge.toml)의 스쿼시 머지 규칙을 따릅니다.

## 공통 작업 원칙

- 변경 대상 폴더의 지침, 기존 구현 및 테스트를 먼저 확인합니다. 세부 기술 규칙은 각 프로젝트 지침을 따릅니다.
- 요청 범위 밖의 코드와 사용자의 기존 변경사항을 보존합니다.
- API 계약을 변경하면 서버와 앱의 관련 호출부, 요청·응답 모델 및 호환성 영향을 함께 확인합니다. 필요한 경우 양쪽 변경을 같은 PR에 포함합니다.
- 현재 요구사항에 불필요한 계층, 의존성 및 인프라를 추가하지 않습니다.
- 자격 증명, 로컬 환경 설정 및 서명 키를 저장소에 추가하지 않습니다. 공유용 환경 설정 예시는 실제 비밀값 없이 작성합니다.
- 커밋, push, 이슈·PR 생성 및 외부 코멘트 게시는 사용자가 요청한 범위에서 수행합니다.

## 검증과 완료 기준

- 빌드 명령은 해당 프로젝트 디렉터리에서 실행합니다. 저장소 전체를 하나의 Gradle 프로젝트로 가정하지 않습니다.
- 변경과 관련된 테스트를 우선 실행하고, 필요한 경우 해당 프로젝트의 빌드·정적 검사를 수행합니다.
- 서버와 앱을 함께 변경하면 각각 검증합니다. 한쪽 검증 결과로 다른 쪽의 성공을 대신하지 않습니다.
- 실패를 숨기거나 검증 조건을 완화하지 않습니다. 실행하지 못한 검증은 원인과 함께 명시합니다.
- 완료 시 변경 내용, 실제 실행 명령과 결과 및 미검증 사항을 한국어로 간결하게 보고합니다.

## 보조 에이전트

[Code Reviewer](.codex/agents/code-reviewer.toml)와 [Tester](.codex/agents/tester.toml)는 Spring Boot 서버 전용 역할입니다. 실행 환경과 사용자 요청에서 위임이 허용되고 필요한 경우에 사용합니다. Android 검증을 서버 전용 역할로 대체하지 않습니다.

[Merge](.codex/agents/merge.toml)는 저장소 공통 PR 머지 역할입니다. PR은 기본적으로 **스쿼시 머지**하며, 사용자가 다른 방식을 명시적으로 지정한 경우에만 변경합니다. 스쿼시 머지가 불가능하면 원인을 보고하고 다른 방식으로 임의 전환하지 않습니다. 스쿼시 커밋 메시지는 공통 커밋 스킬을 따릅니다.

## 출처

[sor999/agent-harness](https://github.com/sor999/agent-harness/tree/a87d2613fc6db14d06c9f109977b41fbd52b5cb5)를 기반으로 프로젝트 전용 내용을 범용화하고, 공통 규칙과 기술별 규칙을 분리했습니다.
