package pt.ulisboa.tecnico.cnv.javassist.tools;

import javassist.*;
import java.util.*;

public class GetClassAttributes extends AbstractJavassistTool {

    public GetClassAttributes(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    private final String BlurImageHandlerDeclaringClass = "pt.ulisboa.tecnico.cnv.imageproc.BlurImageHandler";
    private final String EnhanceImageHandlerDeclaringClass = "pt.ulisboa.tecnico.cnv.imageproc.EnhanceImageHandler";
    private final String RaytracerHandlerDeclaringClass = "pt.ulisboa.tecnico.cnv.raytracer.RaytracerHandler";

    private final String HandleRequestMethodName = "handleRequest";
    private final String ProcessMethodName = "process";

    public static double getCurrentRAMUsage() {
        long currentMemoryUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        return (double) currentMemoryUsed / (1024 * 1024); // Convert to MB
    }

    /**
     * Need to define the correct method signature for the raytracer
     * Good candidates:
     * ```
     * RayTracer rayTracer = new RayTracer(scols, srows, wcols, wrows, coff, roff);
     * rayTracer.readScene(input, texmap);
     * BufferedImage image = rayTracer.draw();
     * ```
     * Then when some of these methods are called we insert the metrics logic for
     * the ray tracer
     */

    private void setImageSizeAndMetricType(CtBehavior behavior, String metricType) throws Exception {
        // Use the BufferedImage parameter ($1) to get the image size
        behavior.insertBefore(
                "pt.ulisboa.tecnico.cnv.webserver.MetricsContext.setImageSize($1.getWidth() * $1.getHeight());");

        // Set the metric type to metricType
        behavior.insertBefore(
                "pt.ulisboa.tecnico.cnv.webserver.MetricsContext.setMetricType(pt.ulisboa.tecnico.cnv.webserver.RequestMetrics.MetricType."
                        + metricType + ");");
    }

    private void handleBlurImageHandler(CtBehavior behavior) throws Exception {
        String methodName = behavior.getName();

        // Will handle the `process` method
        if (methodName.equals(ProcessMethodName)) {
            setImageSizeAndMetricType(behavior, "BLUR");
        }
        // Add other methods here for the BlurImageHandler
    }

    private void handleEnhanceImageHandler(CtBehavior behavior) throws Exception {
        String methodName = behavior.getName();

        // Will handle the `process` method
        if (methodName.equals(ProcessMethodName)) {
            setImageSizeAndMetricType(behavior, "ENHANCE");
        }

        // Add other methods here for the EnhanceImageHandler
    }

    private void handleRaytracerHandler(CtBehavior behavior) throws Exception {
        String methodName = behavior.getName();

        // Will handle the `handleRequest` method
        if (methodName.equals(HandleRequestMethodName)) {
            System.out.println("Handling RaytracerHandler handleRequest method");
        }

        // Add other methods here for the RaytracerHandler
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

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        CtBehavior behavior = block.behavior;
        String declaringClassName = behavior.getDeclaringClass().getName();
        String methodName = behavior.getName();

        if (declaringClassName.equals(BlurImageHandlerDeclaringClass) && methodName.equals(ProcessMethodName)) {
            // Update ram usage for the blur image handler using the
            // MetricsContext.updateMaxRamUsage method
            block.behavior.insertAt(block.line,
                    "pt.ulisboa.tecnico.cnv.webserver.MetricsContext.updateMaxRamUsage(pt.ulisboa.tecnico.cnv.javassist.tools.GetClassAttributes.getCurrentRAMUsage());");
        }
    }
}
