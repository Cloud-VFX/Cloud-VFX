package pt.ulisboa.tecnico.cnv.javassist.tools;

import javassist.*;
import java.util.*;

public class GenerateMetrics extends AbstractJavassistTool {

    public GenerateMetrics(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    private final String MetricsContextDeclaringClass = "pt.ulisboa.tecnico.cnv.webserver.MetricsContext";
    private final String RequestMetricsDeclaringClass = "pt.ulisboa.tecnico.cnv.metrics.RequestMetrics";
    private final String MetricsMiddlewareDeclaringClass = "pt.ulisboa.tecnico.cnv.webserver.MetricsMiddleware";

    private final String BlurImageHandlerDeclaringClass = "pt.ulisboa.tecnico.cnv.imageproc.BlurImageHandler";
    private final String EnhanceImageHandlerDeclaringClass = "pt.ulisboa.tecnico.cnv.imageproc.EnhanceImageHandler";
    private final String RaytracerHandlerDeclaringClass = "pt.ulisboa.tecnico.cnv.raytracer.RaytracerHandler";

    private final String HandleRequestMethodName = "handleRequest";
    private final String ProcessMethodName = "process";
    private final String ReadSceneMethodName = "readScene";
    private final String DrawMethodName = "draw";
    private final String HandleMethodName = "handle";
    private final String InstrumentRaytracerInputMethodName = "instrumentRaytracerInput";

    private void setImageSizeAndMetricType(CtBehavior behavior, String metricType) throws Exception {
        // Set the start processing time of the request
        behavior.insertBefore(
                String.format("%s.setStartProcessingTime();", MetricsContextDeclaringClass));

        // Use the BufferedImage parameter ($1) to get the image size
        behavior.insertBefore(
                String.format("%s.setImageSize($1.getWidth() * $1.getHeight());", MetricsContextDeclaringClass));

        // Set the metric type to metricType
        behavior.insertBefore(
                String.format("%s.setMetricType(%s.MetricType.%s);", MetricsContextDeclaringClass,
                        RequestMetricsDeclaringClass, metricType));

        // Set the end processing time of the request
        behavior.insertAfter(
                String.format("%s.setEndProcessingTime();", MetricsContextDeclaringClass), true);
    }

    private void handleBlurImageHandler(CtBehavior behavior) throws Exception {
        String methodName = behavior.getName();

        // Will handle the `process` method
        if (methodName.equals(ProcessMethodName)) {
            setImageSizeAndMetricType(behavior, "BLUR");
        }
    }

    private void handleEnhanceImageHandler(CtBehavior behavior) throws Exception {
        String methodName = behavior.getName();

        // Will handle the `process` method
        if (methodName.equals(ProcessMethodName)) {
            setImageSizeAndMetricType(behavior, "ENHANCE");
        }
    }

    private void handleRaytracerHandler(CtBehavior behavior) throws Exception {
        String methodName = behavior.getName();

        // Will handle the `handleRequest` method
        if (methodName.equals(HandleRequestMethodName)) {
            behavior.insertBefore(
                    String.format("%s.setStartProcessingTime();", MetricsContextDeclaringClass));
            behavior.insertBefore(
                    String.format("%s.setMetricType(%s.MetricType.RAYTRACER);", MetricsContextDeclaringClass,
                            RequestMetricsDeclaringClass));
            behavior.insertAfter(
                    String.format("%s.setEndProcessingTime();", MetricsContextDeclaringClass), true);
        }

        if (methodName.equals(InstrumentRaytracerInputMethodName)) {
            behavior.insertAfter(
                    String.format("%s.createRaytracerInput($1, $2, $3, $4);", MetricsContextDeclaringClass), true);
        }
    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        String declaringClassName = behavior.getDeclaringClass().getName();

        switch (declaringClassName) {
            case BlurImageHandlerDeclaringClass:
                handleBlurImageHandler(behavior);
                break;
            case EnhanceImageHandlerDeclaringClass:
                handleEnhanceImageHandler(behavior);
                break;
            case RaytracerHandlerDeclaringClass:
                handleRaytracerHandler(behavior);
                break;
            default:
                break;
        }
        super.transform(behavior);
    }

    @Override
    protected void transform(CtClass clazz) throws Exception {
        super.transform(clazz);
    }

    private boolean checkAllMethodsNames(String methodName) {
        return methodName.equals(HandleRequestMethodName) || methodName.equals(ProcessMethodName)
                || methodName.equals(HandleMethodName) || methodName.equals(ReadSceneMethodName)
                || methodName.equals(DrawMethodName) || methodName.equals("read");
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        CtBehavior behavior = block.behavior;
        String declaringClassName = behavior.getDeclaringClass().getName();
        String methodName = behavior.getName();

        if (declaringClassName.equals(MetricsMiddlewareDeclaringClass))
            return;

        if (checkAllMethodsNames(methodName)) {
            behavior.insertAt(block.line,
                    String.format("%s.updateNumberOfInstructions(%d);", MetricsContextDeclaringClass,
                            block.getLength()));
        }
    }
}
