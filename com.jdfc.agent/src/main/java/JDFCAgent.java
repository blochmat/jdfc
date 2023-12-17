
import data.ProjectData;
import lombok.extern.slf4j.Slf4j;
import utils.JDFCUtils;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.List;

@Slf4j
public final class JDFCAgent {

    public static void premain(final String agentArgs, final Instrumentation inst) {
        JDFCUtils.logThis("Start: premain", "callStack");
        List<String> args = Arrays.asList(agentArgs.split(","));
        JDFCUtils.logThis(args.toString(), "test");
        ProjectData.getInstance().saveProjectInfo(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4).equals("inter"));
        JDFCClassTransformer jdfcClassTransformer = new JDFCClassTransformer(args.get(0), args.get(2), args.get(3), args.get(4).equals("inter"));
        inst.addTransformer(jdfcClassTransformer);
        JDFCUtils.logThis("End: premain", "callStack");
    }
}
