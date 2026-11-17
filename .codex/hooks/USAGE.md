# Codex 작업 종료 전 형식 검사

`.codex/hooks.json`의 `Stop` 이벤트가 `check_style.py`를 실행한다. 현재 Codex CLI의 hooks 기능은 활성화되어 있다. 훅 정의를 추가하는 것과 신뢰 등록은 별개이며, 실제 자동 실행은 프로젝트와 훅 정의가 신뢰된 이후에 가능하다.

## 활성화

1. `dalmoi` 저장소에서 Codex를 열거나 세션을 다시 시작한다.
2. CLI에서 `/hooks`를 열어 프로젝트의 `Stop` 훅을 검토하고 신뢰한다. 프로젝트 자체도 신뢰된 상태여야 한다.
3. 훅 정의가 변경되어 다시 검토 대상으로 표시되면 해당 정의를 확인한다. 신뢰를 우회하는 옵션이나 신뢰 저장소 직접 수정은 사용하지 않는다.

공식 문서: https://learn.chatgpt.com/docs/hooks#review-and-trust-hooks

## 검사 범위

- Git HEAD 대비 스테이징·미스테이징 변경과 Git이 무시하지 않은 새 파일을 확인한다.
- 백엔드 Java·Gradle·검사 설정 변경: `spotlessCheck checkstyleMain checkstyleTest`
- Android Kotlin·Gradle·형식 설정 변경: `spotlessCheck`
- 해당 코드 변경이 없고 문서만 바뀌었다면 Gradle을 실행하지 않는다.
- 세션 시작 전부터 있던 미커밋 코드도 검사한다. 이 훅은 작업 파일을 검사하며 스테이징 상태를 대신 검증하지 않는다.

훅은 포맷 자동 수정·커밋·스테이징을 하지 않는다. Android Lint, 앱 빌드 및 전체 테스트도 대신하지 않는다. 기존 Git 훅과 CI는 계속 유지한다.

## 결과와 반복 제한

검사 성공 시 `{}`를 출력한다. 최초 실패 시 `decision: block`과 오류를 반환하여 수정 작업을 이어가도록 한다. `stop_hook_active`가 참인 재시도에서도 검사는 다시 실행하지만, 실패가 지속되면 추가 재개 요청 대신 `systemMessage`로 미통과 및 미해결 원인을 알린다. 따라서 이 훅은 무한 재시도를 막는 보조 검사이며 머지를 강제로 막는 보안 경계가 아니다.

각 영역의 Gradle 실행 제한은 120초이고 훅 전체 제한은 270초다. 타임아웃·wrapper 누락은 성공으로 처리하지 않는다.

## 검증

```sh
python3 .codex/hooks/test_check_style.py
```

실제 코드에 대한 수동 실행은 다음 입력을 스크립트의 stdin으로 전달한다. 이 실행 성공만으로 Codex의 훅 신뢰 등록이나 라이브 이벤트 전달까지 검증되지는 않는다.

```json
{
  "cwd": "/실제/저장소/경로",
  "hook_event_name": "Stop",
  "stop_hook_active": false
}
```
