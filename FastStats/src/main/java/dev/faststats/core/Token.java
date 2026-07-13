package dev.faststats.core;

import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NonNls;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

@NonNls
@Pattern(Token.PATTERN)
@Retention(RetentionPolicy.CLASS)
@Target({METHOD, FIELD, PARAMETER, LOCAL_VARIABLE})
public @interface Token {
    String PATTERN = "[a-z0-9]{32}";
}
