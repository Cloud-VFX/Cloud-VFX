package pt.ulisboa.tecnico.cnv.javassist.tools;

import javassist.*;
import java.util.*;

public class GetClassAttributes extends AbstractJavassistTool {

    public GetClassAttributes(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        // Get the class attributes
        String formatted = getClassAttributes(block);

        block.behavior.insertAt(block.line, String.format("System.out.println(\"%s\");", formatted));

        super.transform(block);
    }

    private String getClassAttributes(BasicBlock block) {
        String formatted = String.format("Class %s: Method %s, Line %d, Position %d",
                block.behavior.getDeclaringClass().getName(), block.behavior.getName(), block.line, block.position);

        return formatted;
    }

}
