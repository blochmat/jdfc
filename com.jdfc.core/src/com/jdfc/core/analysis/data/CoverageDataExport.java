package com.jdfc.core.analysis.data;

import com.jdfc.commons.data.Pair;
import com.jdfc.core.analysis.ifg.data.DefUsePair;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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

        Set<InstanceVariable> instanceVariables = pClassExecutionData.getInstanceVariables();
        classTag.appendChild(createInstanceVariables(doc, instanceVariables));

        Set<InstanceVariable> instanceVarOccurrences = pClassExecutionData.getInstanceVariablesOccurrences();
        classTag.appendChild(createInstanceVarOccurrences(doc, instanceVarOccurrences));

        Set<Pair<ProgramVariable, ProgramVariable>> parameterMatching = pClassExecutionData.getParameterMatching();
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

    private static Element createInstanceVariables(Document pDoc, Set<InstanceVariable> pInstanceVariables) {
        Element instanceVariablesTag = pDoc.createElement("instanceVariables");
        for (InstanceVariable instVar : pInstanceVariables) {
            Element instanceVariableTag = pDoc.createElement("instanceVariable");
            instanceVariablesTag.appendChild(instanceVariableTag);
            instanceVariableTag.setAttribute("owner", instVar.getOwner());
            instanceVariableTag.setAttribute("holder", "null");
            instanceVariableTag.setAttribute("access", String.valueOf(instVar.getAccess()));
            instanceVariableTag.setAttribute("name", instVar.getName());
            instanceVariableTag.setAttribute("descriptor", instVar.getDescriptor());
            instanceVariableTag.setAttribute("signature", instVar.getSignature());
            instanceVariableTag.setAttribute("lineNumber", String.valueOf(instVar.getLineNumber()));
        }
        return instanceVariablesTag;
    }

    private static Element createInstanceVarOccurrences(Document pDoc, Set<InstanceVariable> pInstanceVarOccurrences) {
        Element instanceVarOccurrencesTag = pDoc.createElement("instanceVarOccurrences");
        for(InstanceVariable occ : pInstanceVarOccurrences) {
            Element instanceVarTag = pDoc.createElement("instanceVariable");
            instanceVarOccurrencesTag.appendChild(instanceVarTag);
            instanceVarTag.setAttribute("owner", occ.getOwner());
            instanceVarTag.setAttribute("holder", occ.getHolder().getName());
            Element holder = pDoc.createElement("programVariable");
            instanceVarTag.appendChild(holder);
            holder.setAttribute("owner", occ.getHolder().getOwner());
            holder.setAttribute("name", occ.getHolder().getName());
            holder.setAttribute("type", occ.getHolder().getType());
            holder.setAttribute("instructionIndex",
                    Integer.toString(occ.getHolder().getInstructionIndex()));
            holder.setAttribute("lineNumber",
                    Integer.toString(occ.getHolder().getInstructionIndex()));
            instanceVarTag.setAttribute("access", String.valueOf(occ.getAccess()));
            instanceVarTag.setAttribute("name", occ.getName());
            instanceVarTag.setAttribute("descriptor", occ.getDescriptor());
            instanceVarTag.setAttribute("signature", occ.getSignature());
            instanceVarTag.setAttribute("lineNumber", String.valueOf(occ.getLineNumber()));
        }
        return instanceVarOccurrencesTag;
    }

    private static Element createParameterMatching(Document pDoc, Set<Pair<ProgramVariable, ProgramVariable>> pParameterMatching) {
        Element parameterMatchingTag = pDoc.createElement("parameterMatching");
        for(Pair<ProgramVariable, ProgramVariable> matching : pParameterMatching) {
            Element match = pDoc.createElement("match");
            parameterMatchingTag.appendChild(match);
            Element firstVar = pDoc.createElement("programVariable");
            match.appendChild(firstVar);
            Element secondVar = pDoc.createElement("programVariable");
            match.appendChild(secondVar);
            firstVar.setAttribute("owner", matching.fst.getOwner());
            firstVar.setAttribute("name", matching.fst.getName());
            firstVar.setAttribute("type", matching.fst.getType());
            firstVar.setAttribute("instructionIndex",
                    Integer.toString(matching.fst.getInstructionIndex()));
            firstVar.setAttribute("lineNumber",
                    Integer.toString(matching.fst.getLineNumber()));
            secondVar.setAttribute("owner", matching.snd.getOwner());
            secondVar.setAttribute("name", matching.snd.getName());
            secondVar.setAttribute("type", matching.snd.getType());
            secondVar.setAttribute("instructionIndex",
                    Integer.toString(matching.snd.getInstructionIndex()));
            secondVar.setAttribute("lineNumber",
                    Integer.toString(matching.snd.getLineNumber()));
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
            defUsePairTag.setAttribute("dType", duPair.getDefinition().getType());
            defUsePairTag.setAttribute("dIndex", Integer.toString(duPair.getDefinition().getInstructionIndex()));
            defUsePairTag.setAttribute("dLineNumber", Integer.toString(duPair.getDefinition().getLineNumber()));

            defUsePairTag.setAttribute("uOwner", duPair.getUsage().getOwner());
            defUsePairTag.setAttribute("uName", duPair.getUsage().getName());
            defUsePairTag.setAttribute("uType", duPair.getUsage().getType());
            defUsePairTag.setAttribute("uIndex", Integer.toString(duPair.getUsage().getInstructionIndex()));
            defUsePairTag.setAttribute("uLineNumber", Integer.toString(duPair.getUsage().getLineNumber()));
        }
        return defUsePairsTag;
    }

    private static Element createDefUseCovered(Document pDoc, Set<ProgramVariable> pProgramVarsCovered) {
        Element defUseCoveredTag = pDoc.createElement("defUseCovered");
        for (ProgramVariable programVariable : pProgramVarsCovered) {
            Element programVariableTag = pDoc.createElement("programVariable");
            defUseCoveredTag.appendChild(programVariableTag);
            programVariableTag.setAttribute("owner", programVariable.getOwner());
            programVariableTag.setAttribute("name", programVariable.getName());
            programVariableTag.setAttribute("type", programVariable.getType());
            programVariableTag.setAttribute("instructionIndex",
                    Integer.toString(programVariable.getInstructionIndex()));
            programVariableTag.setAttribute("lineNumber",
                    Integer.toString(programVariable.getLineNumber()));
        }
        return defUseCoveredTag;
    }
}
