package utils;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import data.ProjectData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;

public class JavaParserHelper {

    private final Logger logger = LoggerFactory.getLogger(JavaParserHelper.class);

    private final CombinedTypeSolver combinedTypeSolver;

    public JavaParserHelper() {
        this.combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver()); // For java standard library types
        if(ProjectData.getInstance() != null) {
            combinedTypeSolver.add(new JavaParserTypeSolver(new File(ProjectData.getInstance().getSourceDirRel()))); // For source code
        }
        // NOTE: in case libraries are required for the source code add
        // combinedTypeSolver.add(new JarTypeSolver("lib/your-library.jar")); // For library types

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
    }

    public CompilationUnit parse(File file) throws FileNotFoundException {
       return StaticJavaParser.parse(file);
    }

//    public String buildJvmAsmTypeDesc(Set<ResolvedType> resolvedTypes,
//                                  Map<String, String> nestedTypeMap,
//                                  String jvmDesc,
//                                  Set<ResolvedType> toAdd,
//                                  Set<ResolvedType> toRemove) {
//
//        // TODO: addToEveryListEl: (ILList<Integer>;)LList<Integer>;
//        //       addToEveryListEl: (ILjava/util/List<Ljava/lang/Integer;>;)Ljava/util/List<Ljava/lang/Integer;>;
//
//        // TODO  getTestData: ()Ljava/util/Map<String,Integer>;
//        //       getTestData: ()Ljava/util/Map<Ljava/lang/String;Ljava/lang/Integer;>;
//
//        if(jvmDesc.contains("Map<String,Integer>")) {
//            System.err.println();
//        }
//
//        for(ResolvedType resolvedType : resolvedTypes) {
//            try {
//                if (resolvedType.isReferenceType()) {
//                    Optional<ResolvedReferenceTypeDeclaration> typeDeclaration = resolvedType.asReferenceType().getTypeDeclaration();
//                    if (typeDeclaration.isPresent()) {
//                        ResolvedReferenceTypeDeclaration rrtd = typeDeclaration.get();
//
//                        // Non-Generic types
//                        if (rrtd.isClass() || rrtd.isInterface()) {
//                            if(nestedTypeMap.containsKey(rrtd.getName())) {
//                                // inner or nested class
//                                String replacePattern = "L" + rrtd.getName() + ";";
//                                String replacement = "L" + this.escapeDollar(nestedTypeMap.get(rrtd.getName())) + ";";
//                                jvmDesc = jvmDesc.replaceAll(replacePattern, replacement);
//                                toRemove.add(resolvedType);
//                            } else {
//                                // java native class
//                                if (isException(resolvedType)) {
//                                    // exception
//                                    String newName = rrtd.getQualifiedName().replace(".", "/");
//                                    String replacePattern = rrtd.getName();
//                                    jvmDesc = jvmDesc.replaceAll(replacePattern, newName);
//                                    toRemove.add(resolvedType);
//                                } else {
//                                    ResolvedReferenceTypeDeclaration classDecl = combinedTypeSolver.solveType("java.lang.Class");
//                                    if(classDecl.isAssignableBy(rrtd)) {
//                                        String newName = rrtd.getQualifiedName().replace(".", "/");
//                                        if (jvmDesc.contains(String.format("L%s<", rrtd.getName()))
//                                                || jvmDesc.contains(String.format("L%s;", rrtd.getName()))
//                                                || jvmDesc.contains(String.format("<%s", rrtd.getName()))) {
//                                            // Class<?> or Class
//                                            if(jvmDesc.contains(String.format("L%s<*>;", rrtd.getName()))) {
//                                                String replacePattern = "L" + rrtd.getName() + "<\\*>;";
//                                                String replacement = "L" + newName + "<*>;";
//                                                jvmDesc = jvmDesc.replaceAll(replacePattern, replacement);
//                                            } else if(jvmDesc.contains(String.format("L%s<?>;", rrtd.getName()))) {
//                                                String replacePattern = "L" + rrtd.getName() + "<\\?>;";
//                                                String replacement = "L" + newName + "<?>;";
//                                                jvmDesc = jvmDesc.replaceAll(replacePattern, replacement);
//                                            } else {
//                                                String replacePattern = "L" + rrtd.getName();
//                                                String replacement = "L" + newName;
//                                                jvmDesc = jvmDesc.replaceAll(replacePattern, replacement);
//                                            }
//
//
//                                            toRemove.add(resolvedType);
//                                        } else {
//                                            throw new IllegalArgumentException("Unknown type descriptor: " + rrtd.getQualifiedName());
//                                        }
//                                    } else {
//                                        // normal class or interface
//                                        ResolvedReferenceTypeDeclaration listDecl = combinedTypeSolver.solveType("java.util.List");
//                                        ResolvedReferenceTypeDeclaration setDecl = combinedTypeSolver.solveType("java.util.Set");
//                                        ResolvedReferenceTypeDeclaration mapDecl = combinedTypeSolver.solveType("java.util.Map");
//                                        ResolvedReferenceTypeDeclaration collectionDecl = combinedTypeSolver.solveType("java.util.Collection");
//                                        ResolvedReferenceTypeDeclaration comparatorDecl = combinedTypeSolver.solveType("java.util.Comparator");
//                                        ResolvedReferenceTypeDeclaration appendableDecl = combinedTypeSolver.solveType("java.lang.Appendable");
//                                        if(listDecl.isAssignableBy(rrtd)) {
//                                            // List
//                                            String newName = listDecl.getQualifiedName().replace(".", "/");
//                                            String replacePattern = "L" + listDecl.getName();
//                                            String replacement = "L" + newName;
//                                            jvmDesc = jvmDesc.replaceAll(replacePattern, replacement);
//                                            ResolvedReferenceType resolvedReferenceType = resolvedType.asReferenceType();
//                                            ResolvedTypeParametersMap typeParametersMap = resolvedReferenceType.typeParametersMap();
//                                            if(!typeParametersMap.isEmpty()) {
//                                                toAdd.addAll(typeParametersMap.getTypes());
//                                            }
//                                            toRemove.add(resolvedType);
//                                        } else if (setDecl.isAssignableBy(rrtd)) {
//                                            // Set
//                                            String newName = setDecl.getQualifiedName().replace(".", "/");
//                                            String replacePattern = "L" + setDecl.getName();
//                                            String replacement = "L" + newName;
//                                            jvmDesc = jvmDesc.replaceAll(replacePattern, replacement);
//                                            ResolvedReferenceType resolvedReferenceType = resolvedType.asReferenceType();
//                                            ResolvedTypeParametersMap typeParametersMap = resolvedReferenceType.typeParametersMap();
//                                            if(!typeParametersMap.isEmpty()) {
//                                                toAdd.addAll(typeParametersMap.getTypes());
//                                            }
//                                            toRemove.add(resolvedType);
//                                        } else if (mapDecl.isAssignableBy(rrtd)) {
//                                            // Map
//                                            String newName = mapDecl.getQualifiedName().replace(".", "/");
//                                            String replacePattern = "L" + mapDecl.getName();
//                                            String replacement = "L" + newName;
//                                            jvmDesc = jvmDesc.replaceAll(replacePattern, replacement);
//                                            ResolvedReferenceType resolvedReferenceType = resolvedType.asReferenceType();
//                                            ResolvedTypeParametersMap typeParametersMap = resolvedReferenceType.typeParametersMap();
//                                            if(!typeParametersMap.isEmpty()) {
//                                                toAdd.addAll(typeParametersMap.getTypes());
//                                            }
//                                            toRemove.add(resolvedType);
//                                        } else if (collectionDecl.isAssignableBy(rrtd)){
//                                            // Collection
//                                            String newName = collectionDecl.getQualifiedName().replace(".", "/");
//                                            String replacePattern = "L" + collectionDecl.getName();
//                                            String replacement = "L" + newName;
//                                            jvmDesc = jvmDesc.replaceAll(replacePattern, replacement);
//                                            ResolvedReferenceType resolvedReferenceType = resolvedType.asReferenceType();
//                                            ResolvedTypeParametersMap typeParametersMap = resolvedReferenceType.typeParametersMap();
//                                            if(!typeParametersMap.isEmpty()) {
//                                                toAdd.addAll(typeParametersMap.getTypes());
//                                            }
//                                            toRemove.add(resolvedType);
//                                        } else if (comparatorDecl.isAssignableBy(rrtd)) {
//                                            //
//                                            String newName = comparatorDecl.getQualifiedName().replace(".", "/");
//                                            String replacePattern = "L" + comparatorDecl.getName();
//                                            String replacement = "L" + newName;
//                                            jvmDesc = jvmDesc.replaceAll(replacePattern, replacement);
//                                            ResolvedReferenceType resolvedReferenceType = resolvedType.asReferenceType();
//                                            ResolvedTypeParametersMap typeParametersMap = resolvedReferenceType.typeParametersMap();
//                                            if(!typeParametersMap.isEmpty()) {
//                                                toAdd.addAll(typeParametersMap.getTypes());
//                                            }
//                                            toRemove.add(resolvedType);
//                                        } else if (appendableDecl.isAssignableBy(rrtd)) {
//                                            String newName = appendableDecl.getQualifiedName().replace(".", "/");
//                                            String replacePattern = "L" + appendableDecl.getName();
//                                            String replacement = "L" + newName;
//                                            jvmDesc = jvmDesc.replaceAll(replacePattern, replacement);
//                                            ResolvedReferenceType resolvedReferenceType = resolvedType.asReferenceType();
//                                            ResolvedTypeParametersMap typeParametersMap = resolvedReferenceType.typeParametersMap();
//                                            if(!typeParametersMap.isEmpty()) {
//                                                toAdd.addAll(typeParametersMap.getTypes());
//                                            }
//                                            toRemove.add(resolvedType);
//                                        } else {
//                                            String newName = rrtd.getQualifiedName().replace(".", "/");
//                                            String replacePattern ;
//                                            if(jvmDesc.contains(String.format("<%s>", rrtd.getName()))) {
//                                                replacePattern = rrtd.getName();
//                                            } else if (jvmDesc.contains(String.format("<%s,", rrtd.getName()))) {
//                                                replacePattern = rrtd.getName() + ',';
//                                            } else if (jvmDesc.contains(String.format("%s,", rrtd.getName()))) {
//                                                replacePattern = rrtd.getName() + ',';
//                                            } else if (jvmDesc.contains(String.format("%s>", rrtd.getName()))) {
//                                                replacePattern = rrtd.getName();
//                                            } else {
//                                                replacePattern = "L" + rrtd.getName() + ";";
//                                            }
//                                            String replacement = "L" + newName + ";";
//                                            jvmDesc = jvmDesc.replaceAll(replacePattern, replacement);
//                                            toRemove.add(resolvedType);
//                                        }
//                                    }
//                                }
//                            }
//                        } else if (rrtd.isEnum()) {
//                            String newName = rrtd.getQualifiedName().replace(".", "/");
//                            String replacePattern = rrtd.getName();
//                            jvmDesc = jvmDesc.replaceAll(replacePattern, newName);
//                            toRemove.add(resolvedType);
//                        } else {
//                            ResolvedReferenceTypeDeclaration listDecl = combinedTypeSolver.solveType("java.util.List");
//                            ResolvedReferenceTypeDeclaration collectionDecl = combinedTypeSolver.solveType("java.util.Collection");
//                            ResolvedReferenceTypeDeclaration comparatorDecl = combinedTypeSolver.solveType("java.util.Comparator");
//                            ResolvedReferenceTypeDeclaration appendableDecl = combinedTypeSolver.solveType("java.lang.Appendable");
//                            if(listDecl.isAssignableBy(rrtd)) {
//                                String newName = listDecl.getQualifiedName().replace(".", "/");
//                                String replacePattern = "L" + listDecl.getName();
//                                String replacement = "L" + newName;
//                                jvmDesc = jvmDesc.replaceAll(replacePattern, replacement);
//                                ResolvedReferenceType resolvedReferenceType = resolvedType.asReferenceType();
//                                ResolvedTypeParametersMap typeParametersMap = resolvedReferenceType.typeParametersMap();
//                                if(!typeParametersMap.isEmpty()) {
//                                    toAdd.addAll(typeParametersMap.getTypes());
//                                }
//                                toRemove.add(resolvedType);
//                            } else if (collectionDecl.isAssignableBy(rrtd)){
//                                String newName = collectionDecl.getQualifiedName().replace(".", "/");
//                                String replacePattern = "L" + collectionDecl.getName();
//                                String replacement = "L" + newName;
//                                jvmDesc = jvmDesc.replaceAll(replacePattern, replacement);
//                                ResolvedReferenceType resolvedReferenceType = resolvedType.asReferenceType();
//                                ResolvedTypeParametersMap typeParametersMap = resolvedReferenceType.typeParametersMap();
//                                if(!typeParametersMap.isEmpty()) {
//                                    toAdd.addAll(typeParametersMap.getTypes());
//                                }
//                                toRemove.add(resolvedType);
//                            } else if (comparatorDecl.isAssignableBy(rrtd)) {
//                                String newName = comparatorDecl.getQualifiedName().replace(".", "/");
//                                String replacePattern = "L" + comparatorDecl.getName();
//                                String replacement = "L" + newName;
//                                jvmDesc = jvmDesc.replaceAll(replacePattern, replacement);
//                                ResolvedReferenceType resolvedReferenceType = resolvedType.asReferenceType();
//                                ResolvedTypeParametersMap typeParametersMap = resolvedReferenceType.typeParametersMap();
//                                if(!typeParametersMap.isEmpty()) {
//                                    toAdd.addAll(typeParametersMap.getTypes());
//                                }
//                                toRemove.add(resolvedType);
//                            } else if (appendableDecl.isAssignableBy(rrtd)) {
//                                String newName = appendableDecl.getQualifiedName().replace(".", "/");
//                                String replacePattern = "L" + appendableDecl.getName();
//                                String replacement = "L" + newName;
//                                jvmDesc = jvmDesc.replaceAll(replacePattern, replacement);
//                                ResolvedReferenceType resolvedReferenceType = resolvedType.asReferenceType();
//                                ResolvedTypeParametersMap typeParametersMap = resolvedReferenceType.typeParametersMap();
//                                if(!typeParametersMap.isEmpty()) {
//                                    toAdd.addAll(typeParametersMap.getTypes());
//                                }
//                                toRemove.add(resolvedType);
//                            }
//                        }
//                    }
//                } else if (resolvedType.isArray()) {
//                    ResolvedArrayType rat = resolvedType.asArrayType();
//                    ResolvedType rt = rat.getComponentType();
//                    toAdd.add(rt);
//                    toRemove.add(resolvedType);
//                } else {
//                    toRemove.add(resolvedType);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        resolvedTypes.addAll(toAdd);
//        resolvedTypes.removeAll(toRemove);
//
//        if (!toAdd.isEmpty()) {
//            jvmDesc = buildJvmAsmTypeDesc(resolvedTypes, nestedTypeMap, jvmDesc, new HashSet<>(), new HashSet<>());
//        }
//
//        return jvmDesc;
//    }
//
//    public String buildJvmAsmExcDesc(Set<ResolvedType> resolvedTypes,
//                                  Map<String, String> nestedTypeMap,
//                                  String jvmDesc,
//                                  Set<ResolvedType> toAdd,
//                                  Set<ResolvedType> toRemove) {
//
//        // TODO: register: (LClass;)V;
//        //       register: (Ljava/lang/Class;)V;
//
////        if(jvmDesc.contains("(LClass;)V;")) {
////            System.out.println("asdf");
////        }
//        for(ResolvedType resolvedType : resolvedTypes) {
//            try {
//                if (resolvedType.isReferenceType()) {
//                    Optional<ResolvedReferenceTypeDeclaration> typeDeclaration = resolvedType.asReferenceType().getTypeDeclaration();
//                    if (typeDeclaration.isPresent()) {
//                        ResolvedReferenceTypeDeclaration rrtd = typeDeclaration.get();
//
//                        // Non-Generic Classes
//                        if (rrtd.isClass() || rrtd.isInterface()) {
//                            if(nestedTypeMap.containsKey(rrtd.getName())) {
//                                // inner or nested class
//                                String replacePattern = "L" + rrtd.getName() + ";";
//                                String replacement = "L" + this.escapeDollar(nestedTypeMap.get(rrtd.getName())) + ";";
//                                jvmDesc = jvmDesc.replaceAll(replacePattern, replacement);
//                                toRemove.add(resolvedType);
//                            } else {
//                                // java native class
//                                if (isException(resolvedType)) {
//                                    // exception
//                                    String newName = rrtd.getQualifiedName().replace(".", "/");
//                                    String replacePattern = rrtd.getName();
//                                    jvmDesc = jvmDesc.replaceAll(replacePattern, newName);
//                                    toRemove.add(resolvedType);
//                                } else {
//                                    ResolvedReferenceTypeDeclaration classDecl = combinedTypeSolver.solveType("java.lang.Class");
//                                    if(classDecl.isAssignableBy(rrtd)) {
//                                        String newName = rrtd.getQualifiedName().replace(".", "/");
//                                        if (jvmDesc.contains(String.format("L%s<", rrtd.getName()))
//                                                || jvmDesc.contains(String.format("L%s;", rrtd.getName()))) {
//                                            // Class<?> or Class
//                                            String replacePattern = "L" + rrtd.getName();
//                                            String replacement = "L" + newName;
//                                            jvmDesc = jvmDesc.replaceAll(replacePattern, replacement);
//                                            toRemove.add(resolvedType);
//                                        } else {
//                                            throw new IllegalArgumentException("Unknown type descriptor: " + rrtd.getQualifiedName());
//                                        }
//                                    } else {
//                                        // normal class or interface
//                                        // TODO: Handle with care here if Throwable
//                                        if (rrtd.getName().equals("Throwable")) {
//                                            String newName = rrtd.getQualifiedName().replace(".", "/");
//                                            String replacePattern = "L" + rrtd.getName() + ";";
//                                            jvmDesc = jvmDesc.replaceAll(replacePattern, newName);
//                                            toRemove.add(resolvedType);
//                                        } else {
//                                            String newName = rrtd.getQualifiedName().replace(".", "/");
//                                            String replacePattern = "L" + rrtd.getName() + ";";
//                                            String replacement = "L" + newName + ";";
//                                            jvmDesc = jvmDesc.replaceAll(replacePattern, replacement);
//                                            toRemove.add(resolvedType);
//                                        }
//                                    }
//                                }
//                            }
//                        } else {
//                            // Lists
//                            ResolvedReferenceTypeDeclaration listDecl = combinedTypeSolver.solveType("java.util.List");
//                            ResolvedReferenceTypeDeclaration collectionDecl = combinedTypeSolver.solveType("java.util.Collection");
//                            ResolvedReferenceTypeDeclaration comparatorDecl = combinedTypeSolver.solveType("java.util.Comparator");
//                            ResolvedReferenceTypeDeclaration appendableDecl = combinedTypeSolver.solveType("java.lang.Appendable");
//                            if(listDecl.isAssignableBy(rrtd)) {
//                                String newName = listDecl.getQualifiedName().replace(".", "/");
//                                String replacePattern = "L" + listDecl.getName();
//                                String replacement = "L" + newName;
//                                jvmDesc = jvmDesc.replaceAll(replacePattern, replacement);
//                                ResolvedReferenceType resolvedReferenceType = resolvedType.asReferenceType();
//                                ResolvedTypeParametersMap typeParametersMap = resolvedReferenceType.typeParametersMap();
//                                if(!typeParametersMap.isEmpty()) {
//                                    toAdd.addAll(typeParametersMap.getTypes());
//                                }
//                                toRemove.add(resolvedType);
//                            } else if (collectionDecl.isAssignableBy(rrtd)){
//                                String newName = collectionDecl.getQualifiedName().replace(".", "/");
//                                String replacePattern = "L" + collectionDecl.getName();
//                                String replacement = "L" + newName;
//                                jvmDesc = jvmDesc.replaceAll(replacePattern, replacement);
//                                ResolvedReferenceType resolvedReferenceType = resolvedType.asReferenceType();
//                                ResolvedTypeParametersMap typeParametersMap = resolvedReferenceType.typeParametersMap();
//                                if(!typeParametersMap.isEmpty()) {
//                                    toAdd.addAll(typeParametersMap.getTypes());
//                                }
//                                toRemove.add(resolvedType);
//                            } else if (comparatorDecl.isAssignableBy(rrtd)) {
//                                String newName = comparatorDecl.getQualifiedName().replace(".", "/");
//                                String replacePattern = "L" + comparatorDecl.getName();
//                                String replacement = "L" + newName;
//                                jvmDesc = jvmDesc.replaceAll(replacePattern, replacement);
//                                ResolvedReferenceType resolvedReferenceType = resolvedType.asReferenceType();
//                                ResolvedTypeParametersMap typeParametersMap = resolvedReferenceType.typeParametersMap();
//                                if(!typeParametersMap.isEmpty()) {
//                                    toAdd.addAll(typeParametersMap.getTypes());
//                                }
//                                toRemove.add(resolvedType);
//                            } else if (appendableDecl.isAssignableBy(rrtd)) {
//                                String newName = appendableDecl.getQualifiedName().replace(".", "/");
//                                String replacePattern = "L" + appendableDecl.getName();
//                                String replacement = "L" + newName;
//                                jvmDesc = jvmDesc.replaceAll(replacePattern, replacement);
//                                ResolvedReferenceType resolvedReferenceType = resolvedType.asReferenceType();
//                                ResolvedTypeParametersMap typeParametersMap = resolvedReferenceType.typeParametersMap();
//                                if(!typeParametersMap.isEmpty()) {
//                                    toAdd.addAll(typeParametersMap.getTypes());
//                                }
//                                toRemove.add(resolvedType);
//                            }
//                        }
//                    }
//                } else if (resolvedType.isArray()) {
//                    ResolvedArrayType rat = resolvedType.asArrayType();
//                    ResolvedType rt = rat.getComponentType();
//                    toAdd.add(rt);
//                    toRemove.add(resolvedType);
//                } else {
//                    toRemove.add(resolvedType);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        resolvedTypes.addAll(toAdd);
//        resolvedTypes.removeAll(toRemove);
//
//        if (!toAdd.isEmpty()) {
//            jvmDesc = buildJvmAsmExcDesc(resolvedTypes, nestedTypeMap, jvmDesc, new HashSet<>(), new HashSet<>());
//        }
//
//        return jvmDesc;
//    }
//    public String toJvmTypeDescriptor(MethodDeclaration method) {
//        StringBuilder descriptor = new StringBuilder();
//
//        // Type params
////        descriptor.append('<');
////        for (TypeParameter typePar : method.getTypeParameters()) {
////            System.out.println();
////        }
////        descriptor.append('>');
//
//        // Types
//        descriptor.append('(');
//        for (Parameter parameter : method.getParameters()) {
//            descriptor.append(toJvmType(parameter.getType()));
//        }
//        descriptor.append(')');
//
//        // Return Type
//        String returnType = toJvmType(method.getType());
//        descriptor.append(returnType);
//
//        if (!returnType.endsWith(";")) {
//            descriptor.append(";");
//        }
//
//
//        return this.finalizeComponentTypes(descriptor.toString());
//    }
//
//    public String toJvmExcDescriptor(MethodDeclaration method) {
//        StringBuilder descriptor = new StringBuilder();
//
//        // Exception Types
//        List<String> exceptionDescriptors = new ArrayList<>();
//        for (ReferenceType exception : method.getThrownExceptions()) {
//            exceptionDescriptors.add(toJvmType(exception));
//        }
//
//        if (!exceptionDescriptors.isEmpty()) {
//            descriptor.append(exceptionDescriptors);
//        }
//
//        return this.finalizeComponentTypes(descriptor.toString());
//    }
//
//    public String toJvmTypeDescriptor(ConstructorDeclaration constructor) {
//        StringBuilder descriptor = new StringBuilder();
//
//        // Param Types
//        descriptor.append('(');
//        for (Parameter parameter : constructor.getParameters()) {
//            descriptor.append(toJvmType(parameter.getType()));
//        }
//        descriptor.append(')');
//
//        // Return Type
//        String returnType = toJvmType(new VoidType());
//        descriptor.append(returnType);
//
//        if (!returnType.endsWith(";")) {
//            descriptor.append(";");
//        }
//
//        return this.finalizeComponentTypes(descriptor.toString());
//    }
//
//    public String toJvmExcDescriptor(ConstructorDeclaration constructor) {
//        StringBuilder descriptor = new StringBuilder();
//
//        // Exception Types
//        List<String> exceptionDescriptors = new ArrayList<>();
//        for (ReferenceType exception : constructor.getThrownExceptions()) {
//            exceptionDescriptors.add(toJvmType(exception));
//        }
//
//        if (!exceptionDescriptors.isEmpty()) {
//            descriptor.append(exceptionDescriptors);
//        }
//
//        return this.finalizeComponentTypes(descriptor.toString());
//    }

//    public String finalizeComponentTypes(String descriptor) {
//        if(descriptor.contains("?")) {
//            String[] parts = descriptor.split("[<>]");
//            StringBuilder result = new StringBuilder();
//
//            if (parts.length % 3 == 0) {
//                for(int i = 0; i < parts.length / 3; i++) {
//                    int x = i * 3;
//                    int y = x + 1;
//                    int z = x + 2;
//                    if(!parts[y].equals("?")) {
//                        result.append(parts[x]).append("<L").append(parts[y]).append(";>").append(parts[z]);
//                    } else {
//                        result.append(parts[x]).append("<").append("*").append(">").append(parts[z]);
//                    }
//                }
//                return result.toString();
//            }
////            else {
////                throw new RuntimeException("Invalid type descriptor: " + descriptor);
////            }
//        }
//        return descriptor;
//    }
//
//    public String escapeDollar(String str) {
//        return str.replace("$", "\\$");
//    }
//
//    /**
//     * Parses a JavaParser type representation to the JVM internal string representation.
//     *
//     * @param type
//     * @return
//     */
//    private String toJvmType(Type type) {
//        if (type.isArrayType()) {
//            return "[" + toJvmType(((ArrayType) type).getComponentType());
//        } else if (type.isPrimitiveType()) {
//            switch (type.asString()) {
//                case "byte":
//                    return "B";
//                case "char":
//                    return "C";
//                case "double":
//                    return "D";
//                case "float":
//                    return "F";
//                case "int":
//                    return "I";
//                case "long":
//                    return "J";
//                case "short":
//                    return "S";
//                case "boolean":
//                    return "Z";
//                default:
//                    throw new IllegalArgumentException("Unknown primitive type: " + type.asString());
//            }
//        } else if (type.isVoidType()) {
//            return "V";
//        } else if (this.isException(type.resolve())) {
//            return type.asString().replace('.', '/');
//        } else if (type.isClassOrInterfaceType()) {
//            return "L" + type.asString().replace('.', '/') + ";";
//        } else {
//            throw new IllegalArgumentException("Unsupported type: " + type);
//        }
//    }

//    private boolean isException(ResolvedType type) {
//        if (type.isReferenceType()) {
//            ResolvedReferenceType RRT = type.asReferenceType();
//            List<ResolvedReferenceType> ancestorList = RRT.getAllAncestors();
//            return ancestorList.stream()
//                    .anyMatch(x -> x.getQualifiedName().equals("java.lang.Exception") || x.getQualifiedName().equals("java.lang.RuntimeException"));
//        }
//        return false;
//    }
//
//    public boolean isInnerClass(String name) {
//        return name.contains("$");
//    }
//
//    public boolean isAnonymousInnerClass(String name) {
//        Pattern pattern = Pattern.compile("[_a-zA-Z$][_a-zA-Z0-9$]*\\$\\d+");
//        Matcher matcher = pattern.matcher(name.replace(".class", ""));
//
//        return matcher.matches();
//    }
//
//    public String innerClassFqnToJVMInternal(String fqn) {
//        String[] parts = fqn.split("\\.");
//        if (parts.length > 1) {
//            String lastPart = parts[parts.length - 1];
//            return String.join("/", Arrays.copyOf(parts, parts.length - 1)) + '$' + lastPart;
//        } else {
//            return fqn;
//        }
//    }
}