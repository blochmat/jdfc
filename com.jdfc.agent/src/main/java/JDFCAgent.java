import lombok.extern.slf4j.Slf4j;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.List;

@Slf4j
public final class JDFCAgent {

    public static void premain(final String agentArgs, final Instrumentation inst) {
        log.info("Instrumentation started.");

        // handle arguments
        List<String> args = Arrays.asList(agentArgs.split(","));

        // add transformer for classes with tests
        JDFCClassTransformer jdfcClassTransformer = new JDFCClassTransformer(args.get(0), args.get(1), args.get(2), args.subList(3, args.size()));
        inst.addTransformer(jdfcClassTransformer);
    }
}
