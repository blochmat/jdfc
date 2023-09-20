package instr;

import lombok.Data;

import java.io.File;
import java.io.Serializable;
import java.util.UUID;

@Data
public class ClassMetaData implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean isInnerClass;
    private String classFileAbs;
    private File classFile;
    private String classFileRel;
    private String classFileRelNoType;
    private String classFilePackageRel;
    private String classNodeName;
    private String sourceFileAbs;
    private String sourceFileRel;
    private File sourceFile;
    private String fqn;
    private String outerFqn;
    private String outerName;
    private String name;
    private UUID classDataId;

    public ClassMetaData(String classesDirAbs, String sourceDirAbs, String classFileAbs) {
        this.isInnerClass = classFileAbs.contains("$");
        this.classFileAbs = classFileAbs;
        this.classFile = new File(classFileAbs);
        this.classFileRel = classFileAbs.replace(classesDirAbs, "");
        this.classFilePackageRel = classFileRel.replace(classFile.getName(), "").replaceAll("/$", "");
        this.classFileRelNoType = classFileRel.split("\\.")[0].replace(File.separator, "/");
        this.classNodeName = classFileRel.split("\\.")[0].replace(File.separator, "/").substring(1);
        if (isInnerClass) {
            this.sourceFileRel = convertPath(classFileRel.replace(".class", ".java"));
        } else {
            this.sourceFileRel = classFileRel.replace(".class", ".java");
        }
        this.sourceFileAbs = sourceDirAbs.concat(sourceFileRel);
        this.sourceFile = new File(sourceFileAbs);
        this.fqn = classFileRelNoType.replace(File.separator, ".").substring(1);
        if (classFileAbs.contains("$")) {
            this.outerFqn = fqn.split("\\$")[0];
            this.outerName = outerFqn.split("\\.")[outerFqn.split("\\.").length - 1];
            this.name = fqn.split("\\$")[fqn.split("\\$").length - 1];
        } else {
            this.outerFqn = "";
            this.outerName = "";
            this.name = fqn.split("\\.")[fqn.split("\\.").length - 1];
        }
    }

    public static String convertPath(String input) {
        int dollarIndex = input.lastIndexOf('$');
        int slashIndex = input.lastIndexOf('/');

        // Check if '$' and '/' exist in the string
        if (dollarIndex != -1 && slashIndex != -1) {
            String prefix = input.substring(0, slashIndex + 1);
            String fileName = input.substring(slashIndex + 1, dollarIndex);
            String extension = input.substring(input.lastIndexOf('.'));

            return prefix + fileName + extension;
        } else {
            // Return the original input if '$' or '/' is not found
            return input;
        }
    }

}
