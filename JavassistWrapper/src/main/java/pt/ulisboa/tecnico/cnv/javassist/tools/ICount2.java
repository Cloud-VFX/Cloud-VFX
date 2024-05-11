package pt.ulisboa.tecnico.cnv.javassist.tools;

import javassist.CtBehavior;
import javassist.CtClass;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ICount2 extends CodeDumper {

    private static PrintWriter out;
    private static long nblocks = 0;
    private static long nmethods = 0;
    private static long ninsts = 0;

    // Static initializer to open the file
    static {
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter("output_metrics.txt", true)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ICount2(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    public static void incBasicBlock(int position, int length) {
        nblocks++;
        ninsts += length;
    }

    public static void incBehavior(String name) {
        nmethods++;
    }

    public static void logMetrics() {
        out.println(String.format("[%s] Number of executed methods: %d, Number of executed basic blocks: %d, Number of executed instructions: %d", 
                                  Thread.currentThread().getName(), nmethods, nblocks, ninsts));
        out.flush(); // Flush after every write to ensure data is written immediately
        resetCounters();
    }

    private static void resetCounters() {
        nblocks = 0;
        nmethods = 0;
        ninsts = 0;
    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        super.transform(behavior);
        if (behavior.getDeclaringClass().getName().endsWith("Handler") && behavior.getName().equals("handle")) {
            behavior.addLocalVariable("startTime", CtClass.longType); // Declare start time variable
            behavior.insertBefore("startTime = System.nanoTime();"); // Set start time
    
            // Ensure correct insertion of end time calculations
            behavior.addLocalVariable("endTime", CtClass.longType);
            behavior.addLocalVariable("opTime", CtClass.longType);
    
            StringBuilder builder = new StringBuilder();
            builder.append("endTime = System.nanoTime();");
            builder.append("opTime = endTime - startTime;");
            builder.append("pt.ulisboa.tecnico.cnv.javassist.tools.ICount2.logMetrics();");
    
            behavior.insertAfter(builder.toString(), true);
        }
    }    

    public static void close() {
        if (out != null) {
            out.close();
        }
    }
}
