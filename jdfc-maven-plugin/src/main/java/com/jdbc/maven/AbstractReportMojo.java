package com.jdbc.maven;

import main.java.com.jdfc.core.CFGStorage;
import main.java.com.jdfc.core.cfg.DefUsePair;
import main.java.com.jdfc.core.cfg.ProgramVariable;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;

import java.util.*;

public abstract class AbstractReportMojo extends AbstractMavenReport {
    @Override
    public void execute() throws MojoExecutionException {
        try {
            executeReport(Locale.getDefault());
        } catch (MavenReportException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void executeReport(Locale locale) throws MavenReportException {
        TreeMap<String, List<DefUsePair>> defUsePairs = CFGStorage.INSTANCE.getDefUsePairs();
        Map<String, Set<ProgramVariable>> defUseCovered = CFGStorage.INSTANCE.getDefUseCovered();


        for (Map.Entry<String, List<DefUsePair>> entry : defUsePairs.entrySet()) {
            if (entry.getValue().size() == 0) {
                continue;
            }
            int covered = 0;
            String methodName = entry.getKey();
            for (DefUsePair pair : entry.getValue()) {
                if (defUseCovered.get(methodName).contains(pair.getDefinition())
                        && defUseCovered.get(methodName).contains(pair.getUsage())) {
                    covered += 1;
                }
            }
            System.out.println(
                    "Method "
                            + methodName
                            + " "
                            + "("
                            + defUsePairs.get(methodName).size()
                            + " Def-Use-Pairs; "
                            + covered
                            + " covered)");
        }
    }
}
