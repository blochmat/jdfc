package com.jdfc.commons.utils;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;

import java.io.InputStream;
import java.io.OutputStream;

public class Files {
    public static void copy(InputStream input, OutputStream output){
        // Write input to output
    }

    public static void dumpFile(){
        Element root = new Element("root");
        Document doc = new Document(root);
        doc.setRootElement(root);

        Element pkg = new Element("package");

        // Class
        Element testClass = new Element("class");
        Attribute className = new Attribute("name", "className");

        // Method
        Element testMethod = new Element("method");
        Attribute methodName = new Attribute("name", "testMethod");
        Attribute totalPairs = new Attribute("total", "3");
        Attribute coveredPairs = new Attribute("covered", "2");
        testMethod.setAttribute(methodName);
        testMethod.setAttribute(totalPairs);
        testMethod.setAttribute(coveredPairs);


        // DUList
        Element dupList = new Element("defUsePairs");

        // DUPair
        Element dup = new Element("defUsePair");
        Attribute variableName = new Attribute("variableName", "someValue");
        Attribute defIndex = new Attribute("defIndex", "1");
        Attribute useIndex = new Attribute("useIndex", "2");
        Attribute covered = new Attribute("covered", "true");
        dup.setAttribute(variableName);
        dup.setAttribute(defIndex);
        dup.setAttribute(useIndex);
        dup.setAttribute(covered);


        root.addContent(pkg);
        pkg.addContent(testClass);
        testClass.addContent(testMethod);
        testClass.addContent(dupList);
        dupList.addContent(dup);

    }
}
