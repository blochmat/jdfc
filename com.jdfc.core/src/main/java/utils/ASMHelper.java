package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class ASMHelper {

    private final Logger logger = LoggerFactory.getLogger(ASMHelper.class);

    /**
     * Computes the internal method name representation from the information provided
     * by {@link org.objectweb.asm.ClassVisitor}
     *
     * @param pMethodName The name of the method
     * @param pDescriptor The method's descriptor
     * @param pSignature  The method's signature
     * @param pExceptions An array of exceptions thrown by the method
     * @return The internal method name representation
     */
    public String computeInternalMethodName(
            final String pMethodName,
            final String pDescriptor,
            final String pSignature,
            final String[] pExceptions) {
        this.logger.debug("computeInternalMethodName");
        final StringBuilder result = new StringBuilder();
        result.append(pMethodName);
        result.append(": ");

        if (pSignature != null) {
            result.append(pSignature);
            if (!pSignature.endsWith(";")){
                result.append(";");
            }
        } else {
            result.append(pDescriptor);
            if(!pDescriptor.endsWith(";")) {
                result.append(";");
            }
        }

        if (pExceptions != null && pExceptions.length != 0) {
            result.append(" ").append(Arrays.toString(pExceptions));
        }
        return result.toString();
    }
}
