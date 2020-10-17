package com.jdfc.core.analysis.data;

import com.jdfc.commons.data.Pair;
import com.jdfc.core.analysis.ifg.DefUsePair;
import com.jdfc.core.analysis.ifg.InstanceVariable;
import com.jdfc.core.analysis.ifg.ProgramVariable;
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
        File jdfcDir = new File(outPath);
        if (!jdfcDir.exists()) {
            jdfcDir.mkdirs();
        }

        String classXMLPath = String.format("%s/%s.xml", outPath, pClassName);
        TreeMap<String, List<DefUsePair>> defUsePairs = pClassExecutionData.getDefUsePairs();
        Map<String, Set<ProgramVariable>> defUseCovered = pClassExecutionData.getDefUseCovered();

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

        Element rootTag = doc.createElement("root");
        doc.appendChild(rootTag);

        Element instanceVariablesTag = doc.createElement("instanceVariables");
        rootTag.appendChild(instanceVariablesTag);
        for (InstanceVariable instanceVariable : pClassExecutionData.getInstanceVariables()) {
            Element instanceVarTag = doc.createElement("instanceVariable");
            instanceVarTag.setAttribute("owner", instanceVariable.getOwner());
            instanceVarTag.setAttribute("access", String.valueOf(instanceVariable.getAccess()));
            instanceVarTag.setAttribute("name", instanceVariable.getName());
            instanceVarTag.setAttribute("descriptor", instanceVariable.getDescriptor());
            instanceVarTag.setAttribute("signature", instanceVariable.getSignature());
            instanceVarTag.setAttribute("lineNumber", String.valueOf(instanceVariable.getLineNumber()));
            instanceVariablesTag.appendChild(instanceVarTag);
        }

        Element parameterMatching = doc.createElement("parameterMatching");
        rootTag.appendChild(parameterMatching);
        for(Pair<ProgramVariable, ProgramVariable> matching : pClassExecutionData.getParameterMatching()) {
            Element match = doc.createElement("match");
            parameterMatching.appendChild(match);
            Element var1 = doc.createElement("programVariable");
            match.appendChild(var1);
            Element var2 = doc.createElement("programVariable");
            match.appendChild(var2);
            var1.setAttribute("owner", matching.fst.getOwner());
            var1.setAttribute("name", matching.fst.getName());
            var1.setAttribute("type", matching.fst.getType());
            var1.setAttribute("instructionIndex",
                    Integer.toString(matching.fst.getInstructionIndex()));
            var1.setAttribute("lineNumber",
                    Integer.toString(matching.fst.getLineNumber()));
            var2.setAttribute("owner", matching.snd.getOwner());
            var2.setAttribute("name", matching.snd.getName());
            var2.setAttribute("type", matching.snd.getType());
            var2.setAttribute("instructionIndex",
                    Integer.toString(matching.snd.getInstructionIndex()));
            var2.setAttribute("lineNumber",
                    Integer.toString(matching.snd.getLineNumber()));
        }

        for (Map.Entry<String, List<DefUsePair>> methodEntry : defUsePairs.entrySet()) {
            if (methodEntry.getValue().size() == 0) {
                continue;
            }
            Element methodTag = doc.createElement("method");
            methodTag.setAttribute("name", methodEntry.getKey());
            methodTag.setAttribute("firstLine", String.valueOf(pClassExecutionData.getMethodFirstLine().get(methodEntry.getKey())));
            rootTag.appendChild(methodTag);

            Element defUsePairListTag = doc.createElement("defUsePairs");
            methodTag.appendChild(defUsePairListTag);

            for (DefUsePair duPair : methodEntry.getValue()) {
                Element defUsePairTag = doc.createElement("defUsePair");
                defUsePairListTag.appendChild(defUsePairTag);
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

            Element defUseCoveredTag = doc.createElement("defUseCovered");
            methodTag.appendChild(defUseCoveredTag);

            if (defUseCovered.get(methodEntry.getKey()) != null) {
                for (ProgramVariable programVariable : defUseCovered.get(methodEntry.getKey())) {
                    Element programVariableTag = doc.createElement("programVariable");
                    programVariableTag.setAttribute("owner", programVariable.getOwner());
                    programVariableTag.setAttribute("name", programVariable.getName());
                    programVariableTag.setAttribute("type", programVariable.getType());
                    programVariableTag.setAttribute("instructionIndex",
                            Integer.toString(programVariable.getInstructionIndex()));
                    programVariableTag.setAttribute("lineNumber",
                            Integer.toString(programVariable.getLineNumber()));
                    defUseCoveredTag.appendChild(programVariableTag);
                }
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
}
