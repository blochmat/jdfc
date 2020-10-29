package com.jdfc.core.analysis.data;

import com.jdfc.commons.data.Pair;
import com.jdfc.core.analysis.ifg.data.DefUsePair;
import com.jdfc.core.analysis.ifg.data.Field;
import com.jdfc.core.analysis.ifg.data.InstanceVariable;
import com.jdfc.core.analysis.ifg.data.ProgramVariable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.*;

public class CoverageDataExport {

    public static void dumpClassExecutionDataToFile(final String pClassName,
                                                    final ClassExecutionData pClassExecutionData) throws ParserConfigurationException, TransformerException {
        String outPath = String.format("%s/target/jdfc", System.getProperty("user.dir"));
        File JDFCDir = new File(outPath);
        if (!JDFCDir.exists()) {
            JDFCDir.mkdirs();
        }

        String classXMLPath = String.format("%s/%s.xml", outPath, pClassName);

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element classTag = doc.createElement("class");
        doc.appendChild(classTag);

        Set<Field> fields = pClassExecutionData.getFields();
        classTag.appendChild(createFields(doc, fields));

        Set<InstanceVariable> instanceVariables = pClassExecutionData.getInstanceVariables();
        classTag.appendChild(createInstanceVariables(doc, instanceVariables));

        Map<ProgramVariable, ProgramVariable> parameterMatching = pClassExecutionData.getInterProceduralMatches();
        classTag.appendChild(createParameterMatching(doc, parameterMatching));

        Map<String, Integer> methodFirstLine = pClassExecutionData.getMethodFirstLine();
        TreeMap<String, List<DefUsePair>> defUsePairs = pClassExecutionData.getDefUsePairs();
        Map<String, Set<ProgramVariable>> defUseCovered = pClassExecutionData.getDefUseCovered();
        for (Map.Entry<String, List<DefUsePair>> methodEntry : defUsePairs.entrySet()) {
            if (methodEntry.getValue().size() != 0) {
                String methodName = methodEntry.getKey();
                classTag.appendChild(createMethod(doc, methodName,
                        methodFirstLine.get(methodName), methodEntry.getValue(), defUseCovered.get(methodName)));
            }
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource domSource = new DOMSource(doc);
        File file = new File(classXMLPath);
        file.getParentFile().mkdirs();
        StreamResult streamResult = new StreamResult(file);
        transformer.transform(domSource, streamResult);
    }

    private static Element createFields(Document pDoc, Set<Field> pFields) {
        Element fieldList = pDoc.createElement("fieldList");
        for (Field element : pFields) {
            Element field = pDoc.createElement("field");
            fieldList.appendChild(field);
            field.setAttribute("owner", element.getOwner());
            field.setAttribute("access", String.valueOf(element.getAccess()));
            field.setAttribute("name", element.getName());
            field.setAttribute("descriptor", element.getDescriptor());
            field.setAttribute("signature", element.getSignature());
        }
        return fieldList;
    }

    private static Element createInstanceVariables(Document pDoc, Set<InstanceVariable> pInstanceVariables) {
        Element instanceVarList = pDoc.createElement("instanceVariableList");
        for(InstanceVariable element : pInstanceVariables) {
            Element instanceVar = pDoc.createElement("instanceVariable");
            instanceVarList.appendChild(instanceVar);
            instanceVar.setAttribute("owner", element.getOwner());
            instanceVar.setAttribute("holder", ProgramVariable.encode(element.getHolder()));
            instanceVar.setAttribute("access", String.valueOf(element.getAccess()));
            instanceVar.setAttribute("name", element.getName());
            instanceVar.setAttribute("descriptor", element.getDescriptor());
            instanceVar.setAttribute("signature", element.getSignature());
            instanceVar.setAttribute("instructionIndex", String.valueOf(element.getInstructionIndex()));
            instanceVar.setAttribute("lineNumber", String.valueOf(element.getLineNumber()));
        }
        return instanceVarList;
    }

    private static Element createParameterMatching(Document pDoc, Map<ProgramVariable, ProgramVariable> pParameterMatching) {
        Element parameterMatchingTag = pDoc.createElement("parameterMatching");
        for(Map.Entry<ProgramVariable, ProgramVariable> element : pParameterMatching.entrySet()) {
            Element match = pDoc.createElement("match");
            parameterMatchingTag.appendChild(match);
            Element firstVar = creatProgramVariable(pDoc, element.getKey());
            match.appendChild(firstVar);
            Element secondVar = creatProgramVariable(pDoc, element.getValue());
            match.appendChild(secondVar);
        }
        return parameterMatchingTag;
    }

    private static Element createMethod(Document pDoc,
                                        String pMethodName,
                                        Integer pMethodFirstLine,
                                        List<DefUsePair> pDefUsePairs,
                                        Set<ProgramVariable> pDefUseCovered) {
        Element methodTag = pDoc.createElement("method");
        methodTag.setAttribute("name", pMethodName);
        methodTag.setAttribute("firstLine", String.valueOf(pMethodFirstLine));
        methodTag.appendChild(createDefUsePairs(pDoc, pDefUsePairs));
        if (pDefUseCovered != null) {
            methodTag.appendChild(createDefUseCovered(pDoc, pDefUseCovered));
        }
        return methodTag;
    }

    private static Element createDefUsePairs(Document pDoc, List<DefUsePair> pDefUsePairList) {
        Element defUsePairsTag = pDoc.createElement("defUsePairs");
        for (DefUsePair duPair : pDefUsePairList) {
            Element defUsePairTag = pDoc.createElement("defUsePair");
            defUsePairsTag.appendChild(defUsePairTag);
            defUsePairTag.setAttribute("dOwner", duPair.getDefinition().getOwner());
            defUsePairTag.setAttribute("dName", duPair.getDefinition().getName());
            defUsePairTag.setAttribute("dType", duPair.getDefinition().getDescriptor());
            defUsePairTag.setAttribute("dIndex", Integer.toString(duPair.getDefinition().getInstructionIndex()));
            defUsePairTag.setAttribute("dLineNumber", Integer.toString(duPair.getDefinition().getLineNumber()));

            defUsePairTag.setAttribute("uOwner", duPair.getUsage().getOwner());
            defUsePairTag.setAttribute("uName", duPair.getUsage().getName());
            defUsePairTag.setAttribute("uType", duPair.getUsage().getDescriptor());
            defUsePairTag.setAttribute("uIndex", Integer.toString(duPair.getUsage().getInstructionIndex()));
            defUsePairTag.setAttribute("uLineNumber", Integer.toString(duPair.getUsage().getLineNumber()));
        }
        return defUsePairsTag;
    }

    private static Element createDefUseCovered(Document pDoc, Set<ProgramVariable> pProgramVarsCovered) {
        Element defUseCoveredTag = pDoc.createElement("defUseCovered");
        for (ProgramVariable programVariable : pProgramVarsCovered) {
            Element programVariableTag = creatProgramVariable(pDoc, programVariable);
            defUseCoveredTag.appendChild(programVariableTag);
        }
        return defUseCoveredTag;
    }

    private static Element creatProgramVariable(Document pDoc, ProgramVariable pProgramVariable) {
        Element programVariable = pDoc.createElement("programVariable");
        programVariable.setAttribute("owner", pProgramVariable.getOwner());
        programVariable.setAttribute("name", pProgramVariable.getName());
        programVariable.setAttribute("descriptor", pProgramVariable.getDescriptor());
        programVariable.setAttribute("instructionIndex",
                Integer.toString(pProgramVariable.getInstructionIndex()));
        programVariable.setAttribute("lineNumber",
                Integer.toString(pProgramVariable.getLineNumber()));
        return programVariable;
    }
}
