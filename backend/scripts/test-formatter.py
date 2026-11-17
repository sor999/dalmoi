"""Exercise the real Spotless profile on representative formatting regressions."""

import shutil
import subprocess
import tempfile
from pathlib import Path


BACKEND = Path(__file__).resolve().parents[1]


with tempfile.TemporaryDirectory(prefix="dalmoi-formatter-test-") as directory:
    project = Path(directory)
    for relative in ("build.gradle", "settings.gradle", "gradlew",
                     "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties",
                     "config/formatter/eclipse.xml"):
        target = project / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(BACKEND / relative, target)
    probe = project / "src/main/java/LayoutExample.java"
    probe.parent.mkdir(parents=True)
    probe.write_text('''class LayoutExample {
    enum ErrorCode {
        INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "올바르지 않은 요청입니다."),
        UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 요청 형식입니다.");
    }

    @Deprecated
    private Object member;

    Object create(Object member, String passwordHash) {
        return builder()
                .member(member)
                .passwordHash(passwordHash)
                .build();
    }

    void handleExceptionInternal_invalidBody_returnsInvalidRequest(String body) throws Exception {
        client.perform(post("/test/errors/body")
                        .content(body))
                .expect(body);
    }
}
''')
    for iteration in range(2):
        subprocess.run([str(project / "gradlew"), "--console=plain", "spotlessApply", "spotlessCheck"],
                       cwd=project, check=True)
        output = probe.read_text()
        assert 'UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 요청 형식입니다.");' in output
        assert '\n        UNSUPPORTED_MEDIA_TYPE(HttpStatus.' in output
        assert '), UNSUPPORTED_MEDIA_TYPE' not in output
        assert 'return builder()\n' in output
        assert '.member(member)\n' in output
        assert '.passwordHash(passwordHash)\n' in output
        assert '@Deprecated\n    private Object member;' in output
        assert 'void handleExceptionInternal_invalidBody_returnsInvalidRequest(String body) throws Exception {' in output
        assert 'client.perform(post("/test/errors/body")' in output
        if iteration:
            assert output == previous, "Repeated formatting changed the result"
        previous = output
    print("PASS: enum entries, builder chains, annotations, throws and first arguments remain readable and stable.")
