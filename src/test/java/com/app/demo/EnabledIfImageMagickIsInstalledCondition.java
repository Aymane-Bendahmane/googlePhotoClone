package com.app.demo;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

public class EnabledIfImageMagickIsInstalledCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return findAnnotation(context.getElement(), EnabledIfImageMagickIsInstalled.class) //
                .map((annotation) -> (new ImageMagick().detectVersion() != ImageMagick.Version.NA)
                        ? ConditionEvaluationResult.enabled("ImageMagick installed.")
                        : ConditionEvaluationResult.disabled("No ImageMagick installation found.")) //
                .orElse(ConditionEvaluationResult.disabled("By default, Imagemagick tests are disabled"));
    }

}
