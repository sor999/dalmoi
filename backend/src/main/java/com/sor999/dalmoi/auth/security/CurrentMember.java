package com.sor999.dalmoi.auth.security;

import io.swagger.v3.oas.annotations.Parameter;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/** 인증된 Access JWT의 회원 ID를 Long 파라미터에 주입한다. */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "T(Long).valueOf(subject)", errorOnInvalidType = true)
@Parameter(hidden = true)
public @interface CurrentMember {
}
