package pt.ulisboa.tecnico.cnv.javassist.tools;

import javassist.*;
import java.util.*;

public class GetClassAttributes extends AbstractJavassistTool {

    public GetClassAttributes(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    private final String ImageProcessingHandlerDeclaringClass = "pt.ulisboa.tecnico.cnv.imageproc.ImageProcessingHandler";
    private final String BlurImageHandlerDeclaringClass = "pt.ulisboa.tecnico.cnv.imageproc.BlurImageHandler";
    private final String EnhanceImageHandlerDeclaringClass = "pt.ulisboa.tecnico.cnv.imageproc.EnhanceImageHandler";
    private final String RaytracerHandlerDeclaringClass = "pt.ulisboa.tecnico.cnv.raytracer.RaytracerHandler";

    private final String HandleRequestMethodName = "handleRequest";
    private final String ProcessMethodName = "process";

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

    private void handleImageProcessingHandleRequestMethod(CtBehavior behavior) throws Exception {
        // This will insert the Image Width and Height metrics logic
        behavior.insertAfter("{"
                + "java.awt.image.BufferedImage bi = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(java.util.Base64.getDecoder().decode($1)));"
                + "System.out.println(\"Class: \" + getClass().getName() + \", Method: handleRequest, Image Width: \" + bi.getWidth() + \", Image Height: \" + bi.getHeight());"
                + "}", true);
    }

    private void handleImageProcessingHandler(CtBehavior behavior) throws Exception {
        String methodName = behavior.getName();

        // Will handle the `handleRequest` method
        if (methodName.equals(HandleRequestMethodName)) {
            if (behavior.getSignature().equals("(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;")) {
                handleImageProcessingHandleRequestMethod(behavior);
            }
        }
        // Add other methods here for the ImageProcessingHandler
    }

    private void handleBlurImageHandler(CtBehavior behavior) throws Exception {
        String methodName = behavior.getName();

        // Will handle the `process` method
        if (methodName.equals(ProcessMethodName)) {
            System.out.println("Handling handleBlurImageHandler process method");
        }
        // Add other methods here for the BlurImageHandler
    }

    private void handleEnhanceImageHandler(CtBehavior behavior) throws Exception {
        String methodName = behavior.getName();

        // Will handle the `process` method
        if (methodName.equals(ProcessMethodName)) {
            System.out.println("Handling EnchangeImageHandler process method");
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
            case ImageProcessingHandlerDeclaringClass:
                handleImageProcessingHandler(behavior);
                break;
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
}
