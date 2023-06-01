package utils;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import data.CoverageDataStore;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaPaserHelper {

    private CombinedTypeSolver combinedTypeSolver;

    public JavaPaserHelper() {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver()); // For java standard library types
        for(String source : CoverageDataStore.getInstance().getSrcDirStrList()) {
            combinedTypeSolver.add(new JavaParserTypeSolver(new File(source))); // For source code
        }
        // NOTE: in case libraries are required for the source code add
        // combinedTypeSolver.add(new JarTypeSolver("lib/your-library.jar")); // For library types

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
    }

    public CompilationUnit parse(File file) throws FileNotFoundException {
       return StaticJavaParser.parse(file);
    }

    public String buildJvmName(MethodDeclaration method) {
        StringBuilder descriptor = new StringBuilder();

        // Param Types
        descriptor.append('(');
        for (Parameter parameter : method.getParameters()) {
            descriptor.append(toJvmType(parameter.getType()));
        }
        descriptor.append(')');

        // Return Type
        String returnType = toJvmType(method.getType());
        descriptor.append(returnType);

        if (!returnType.endsWith(";")) {
            descriptor.append(";");
        }

        // Exception Types
        List<String> exceptionDescriptors = new ArrayList<>();
        for (ReferenceType exception : method.getThrownExceptions()) {
            exceptionDescriptors.add(toJvmType(exception));
        }

        if (!exceptionDescriptors.isEmpty()) {
            descriptor.append(" ");
            descriptor.append(exceptionDescriptors);
        }

        return descriptor.toString();
    }

    /**
     * Parses a JavaParser type representation to the JVM internal string representation.
     *
     * @param type
     * @return
     */
    private String toJvmType(Type type) {
        if (type.isArrayType()) {
            return "[" + toJvmType(((ArrayType) type).getComponentType());
        } else if (type.isPrimitiveType()) {
            switch (type.asString()) {
                case "byte":
                    return "B";
                case "char":
                    return "C";
                case "double":
                    return "D";
                case "float":
                    return "F";
                case "int":
                    return "I";
                case "long":
                    return "J";
                case "short":
                    return "S";
                case "boolean":
                    return "Z";
                default:
                    throw new IllegalArgumentException("Unknown primitive type: " + type.asString());
            }
        } else if (type.isVoidType()) {
            return "V";
        } else if (this.isException(type.resolve())) {
            return type.asString().replace('.', '/');
        } else if (type.isClassOrInterfaceType()) {
            return "L" + type.asString().replace('.', '/') + ";";
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    private boolean isException(ResolvedType type) {
        if (type.isReferenceType()) {
            ResolvedReferenceType RRT = type.asReferenceType();
            List<ResolvedReferenceType> ancestorList = RRT.getAllAncestors();
            return ancestorList.stream()
                    .anyMatch(x -> x.getQualifiedName().equals("java.lang.Exception") || x.getQualifiedName().equals("java.lang.RuntimeException"));
        }
        return false;
    }

    public boolean isInnerClass(String name) {
        return name.contains("$");
    }

    public boolean isAnonymousInnerClass(String name) {
        Pattern pattern = Pattern.compile("[_a-zA-Z$][_a-zA-Z0-9$]*\\$\\d+");
        Matcher matcher = pattern.matcher(name.replace(".class", ""));

        return matcher.matches();
    }

    public String innerClassFqnToJVMInternal(String fqn) {
        String[] parts = fqn.split("\\.");
        if (parts.length > 1) {
            String lastPart = parts[parts.length - 1];
            return String.join("/", Arrays.copyOf(parts, parts.length - 1)) + '$' + lastPart;
        } else {
            return fqn;
        }
    }
}