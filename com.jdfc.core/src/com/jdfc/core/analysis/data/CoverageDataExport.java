package com.jdfc.core.analysis.data;

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

        Map<ProgramVariable, ProgramVariable> interProceduralMatches = pClassExecutionData.getInterProceduralMatches();
        classTag.appendChild(createInterProceduralMatchList(doc, interProceduralMatches));

        Map<String, Integer> methodFirstLine = pClassExecutionData.getMethodFirstLine();
        Map<String, Integer> methodLastLine = pClassExecutionData.getMethodLastLine();
        TreeMap<String, List<DefUsePair>> defUsePairs = pClassExecutionData.getDefUsePairs();
        Map<String, Set<ProgramVariable>> variablesCovered = pClassExecutionData.getVariablesCovered();
        for (Map.Entry<String, List<DefUsePair>> methodEntry : defUsePairs.entrySet()) {
            if (methodEntry.getValue().size() != 0) {
                String methodName = methodEntry.getKey();
                classTag.appendChild(createMethod(doc, methodName,
                        methodFirstLine.get(methodName), methodLastLine.get(methodName),
                        methodEntry.getValue(), variablesCovered.get(methodName)));
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

    private static Element createInterProceduralMatchList(Document pDoc, Map<ProgramVariable, ProgramVariable> pParameterMatches) {
        Element interProceduralMatches = pDoc.createElement("interProceduralMatchList");
        for(Map.Entry<ProgramVariable, ProgramVariable> element : pParameterMatches.entrySet()) {
            Element match = pDoc.createElement("match");
            interProceduralMatches.appendChild(match);
            Element firstVar = creatProgramVariable(pDoc, element.getKey());
            match.appendChild(firstVar);
            Element secondVar = creatProgramVariable(pDoc, element.getValue());
            match.appendChild(secondVar);
        }
        return interProceduralMatches;
    }

    private static Element createMethod(Document pDoc,
                                        String pMethodName,
                                        Integer pMethodFirstLine,
                                        Integer pMethodLastLine,
                                        List<DefUsePair> pDefUsePairs,
                                        Set<ProgramVariable> pDefUseCovered) {
        Element methodTag = pDoc.createElement("method");
        methodTag.setAttribute("name", pMethodName);
        methodTag.setAttribute("firstLine", String.valueOf(pMethodFirstLine));
        methodTag.setAttribute("lastLine", String.valueOf(pMethodLastLine));
        methodTag.appendChild(createDefUsePairs(pDoc, pDefUsePairs));
        if (pDefUseCovered != null) {
            methodTag.appendChild(createVariablesCovered(pDoc, pDefUseCovered));
        }
        return methodTag;
    }

    private static Element createDefUsePairs(Document pDoc, List<DefUsePair> pDefUsePairList) {
        Element defUsePairList = pDoc.createElement("defUsePairList");
        for (DefUsePair element : pDefUsePairList) {
            Element defUsePair = pDoc.createElement("defUsePair");
            defUsePairList.appendChild(defUsePair);
            defUsePair.setAttribute("dOwner", element.getDefinition().getOwner());
            defUsePair.setAttribute("dName", element.getDefinition().getName());
            defUsePair.setAttribute("dType", element.getDefinition().getDescriptor());
            defUsePair.setAttribute("dIndex", Integer.toString(element.getDefinition().getInstructionIndex()));
            defUsePair.setAttribute("dLineNumber", Integer.toString(element.getDefinition().getLineNumber()));

            defUsePair.setAttribute("uOwner", element.getUsage().getOwner());
            defUsePair.setAttribute("uName", element.getUsage().getName());
            defUsePair.setAttribute("uType", element.getUsage().getDescriptor());
            defUsePair.setAttribute("uIndex", Integer.toString(element.getUsage().getInstructionIndex()));
            defUsePair.setAttribute("uLineNumber", Integer.toString(element.getUsage().getLineNumber()));
        }
        return defUsePairList;
    }

    private static Element createVariablesCovered(Document pDoc, Set<ProgramVariable> pProgramVarsCovered) {
        Element variablesCoveredList = pDoc.createElement("variablesCoveredList");
        for (ProgramVariable element : pProgramVarsCovered) {
            Element programVariableTag = creatProgramVariable(pDoc, element);
            variablesCoveredList.appendChild(programVariableTag);
        }
        return variablesCoveredList;
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
