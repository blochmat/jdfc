import data.ProjectData;
import lombok.extern.slf4j.Slf4j;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.List;

@Slf4j
public final class JDFCAgent {

    public static void premain(final String agentArgs, final Instrumentation inst) {
        List<String> args = Arrays.asList(agentArgs.split(","));
        ProjectData.getInstance().saveProjectInfo(args.get(0), args.get(1), args.get(2), args.get(3));
        JDFCClassTransformer jdfcClassTransformer = new JDFCClassTransformer(args.get(0), args.get(1), args.get(2), args.get(4).equals("inter"));
        inst.addTransformer(jdfcClassTransformer);
    }
}
