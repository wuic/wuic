package com.github.wuic.engine;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * This annotation allows a {@link LineInspector} to tell a {@link com.github.wuic.engine.core.TextInspectorEngine} to
 * wrap it should inside a {@link ScriptLineInspector}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WithScriptLineInspector {

    /**
     * <p>
     * The {@link com.github.wuic.engine.ScriptLineInspector.ScriptMatchCondition} used by the {@link ScriptLineInspector}.
     * </p>
     *
     * @return the condition
     */
    ScriptLineInspector.ScriptMatchCondition condition();
}
