package instr;

import lombok.Data;

import java.io.*;
import java.nio.charset.StandardCharsets;
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

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeBoolean(isInnerClass);
        writeString(out, classFileAbs);
        writeString(out, classFile != null ? classFile.getAbsolutePath() : "");
        writeString(out, classFileRel);
        writeString(out, classFileRelNoType);
        writeString(out, classFilePackageRel);
        writeString(out, classNodeName);
        writeString(out, sourceFileAbs);
        writeString(out, sourceFileRel);
        writeString(out, sourceFile != null ? sourceFile.getAbsolutePath() : "");
        writeString(out, fqn);
        writeString(out, outerFqn);
        writeString(out, outerName);
        writeString(out, name);
        out.writeLong(classDataId.getMostSignificantBits());
        out.writeLong(classDataId.getLeastSignificantBits());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        isInnerClass = in.readBoolean();
        classFileAbs = readString(in);
        String classFilePath = readString(in);
        classFile = classFilePath.isEmpty() ? null : new File(classFilePath);
        classFileRel = readString(in);
        classFileRelNoType = readString(in);
        classFilePackageRel = readString(in);
        classNodeName = readString(in);
        sourceFileAbs = readString(in);
        sourceFileRel = readString(in);
        String sourceFilePath = readString(in);
        sourceFile = sourceFilePath.isEmpty() ? null : new File(sourceFilePath);
        fqn = readString(in);
        outerFqn = readString(in);
        outerName = readString(in);
        name = readString(in);
        long mostSigBits = in.readLong();
        long leastSigBits = in.readLong();
        classDataId = new UUID(mostSigBits, leastSigBits);
    }

    private void writeString(ObjectOutputStream out, String str) throws IOException {
        byte[] bytes = str != null ? str.getBytes(StandardCharsets.UTF_8) : new byte[0];
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private String readString(ObjectInputStream in) throws IOException {
        int length = in.readInt();
        if (length == 0) return "";
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
