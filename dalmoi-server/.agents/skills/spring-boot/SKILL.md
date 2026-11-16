---
name: spring-boot
description: Java Spring Boot 백엔드의 기능 구현, 설계, 수정, 테스트와 코드 리뷰에 프로젝트 컨벤션과 하네스 절차를 적용한다. Spring Boot 서버 작업에 사용하며 Android 또는 프런트엔드 전용 작업에는 적용하지 않는다.
---

# Spring Boot 백엔드 작업

이 스킬은 프로젝트에 함께 설치된 하네스의 진입점이다. 아래 상대 링크는 이 파일 기준이며, 문서 본문의 `docs/`, `.agents/`, `.github/` 등 경로는 별도 표시가 없으면 하네스가 설치된 백엔드 루트 기준이다. GitHub 템플릿은 Git 저장소 루트에서 찾는다.

## 프로젝트 확인

- [AGENTS.md](../../../AGENTS.md)와 작업 대상 하위 경로의 지침을 확인한다.
- `build.gradle`, `build.gradle.kts` 또는 `pom.xml`에서 Java·Spring Boot 버전, 빌드 도구 및 의존성을 확인한다. 저장소에 wrapper가 있으면 우선 사용한다.
- 애플리케이션 클래스와 기존 소스에서 기본 패키지, 계층 구조 및 관련 도메인을 파악한다.
- 애플리케이션 설정과 기존 테스트에서 DB, 인증 주체, 예외 응답, 마이그레이션 및 테스트 지원 구성을 확인한다.
- 특정 프로젝트명, 도메인, 패키지 경로, DB 엔진·버전, principal 타입 또는 클라우드 환경을 가정하지 않는다.

## 작업별 지침

요청에 필요한 지침만 읽는다.

- 구현·설계·버그 수정: [코드 컨벤션](../../../docs/conventions/code-convention.md)을 따른다.
- 테스트 작성·보완·실패 분석: [Test Skill](../test/SKILL.md)과 [테스트 컨벤션](../../../docs/conventions/test-convention.md)을 따른다.
- 코드·PR 리뷰: [Review Skill](../review/SKILL.md)을 따른다.
- 커밋 메시지 작성: [Commit Skill](../../../../.agents/skills/commit/SKILL.md)을 따른다.
- 이슈 작성: [Issue Skill](../../../../.agents/skills/issue/SKILL.md)을 따른다.
- PR 작성: [PR Skill](../../../../.agents/skills/pr/SKILL.md)을 따른다.

기존 프로젝트 규칙을 우선하며, 함께 제공된 스타일은 별도 규칙이 없는 경우의 기본안으로 사용한다. 기술별 규칙은 해당 기술이 있는 경우에 적용하며, 하네스만을 이유로 패키지·의존성·인프라를 추가하지 않는다.

## 검증과 보고

변경과 관련된 테스트 및 필요한 빌드 검사를 실행한다. 실제 수행한 명령과 결과를 보고하고, 환경 문제와 코드 결함을 구분한다. 실행하지 못한 검증은 명시한다. 보조 에이전트 역할 설정은 저장소 루트의 `.codex/agents/`에 있으며 위임이 허용되고 필요한 경우에 사용한다.
