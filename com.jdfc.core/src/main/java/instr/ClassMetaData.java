package instr;

import lombok.Data;

import java.io.File;

@Data
public class ClassMetaData {

    private String classFileAbs;
    private String classFileRel;
    private String classFileRelNoType;
    private String classFilePackageRel;
    private String sourceFileAbs;
    private String sourceFileRel;
    private String fqn;
    private String name;
    private File classFile;

    public ClassMetaData(String classesDirAbs, String sourceDirAbs, String classFileAbs) {
        this.classFileAbs = classFileAbs;
        this.classFile = new File(classFileAbs);
        this.classFileRel = classFileAbs.replace(classesDirAbs, "");
        this.classFilePackageRel = classFileRel.replace(classFile.getName(), "").replaceAll("/$", "");
        this.classFileRelNoType = classFileRel.split("\\.")[0].replace(File.separator, "/");
        this.sourceFileRel = classFileRel.replace(".class", ".java");
        this.sourceFileAbs = sourceDirAbs.concat(sourceFileRel);
        this.fqn = classFileRelNoType.replace(File.separator, ".").substring(1);
        this.name = fqn.split("\\.")[fqn.split("\\.").length - 1];
    }
}
