package com.jdfc.core.analysis.ifg.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class InstanceVariable {

    private final String owner;
    private final ProgramVariable holder;
    private final int access;
    private final String name;
    private final String descriptor;
    private final String signature;
    private final int lineNumber;

    private InstanceVariable(final String pOwner,
                             final ProgramVariable pHolder,
                             final int pAccess,
                             final String pName,
                             final String pDescriptor,
                             final String pSignature,
                             final int pLineNumber) {
        owner = pOwner;
        holder = pHolder;
        access = pAccess;
        name = pName;
        descriptor = pDescriptor;
        signature = pSignature;
        lineNumber = pLineNumber;
    }

    public static InstanceVariable create(final String pOwner,
                                          final ProgramVariable pHolder,
                                          final int pAccess,
                                          final String pName,
                                          final String pDescriptor,
                                          final String pSignature,
                                          final int pLineNumber) {
        return new InstanceVariable(pOwner, pHolder, pAccess, pName, pDescriptor, pSignature, pLineNumber);
    }

    public int getAccess() {
        return access;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public String getSignature() {
        return signature;
    }

    public String getOwner() {
        return owner;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public ProgramVariable getHolder() {
        return holder;
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, access, name, descriptor, signature, lineNumber);
    }

    @Override
    public boolean equals(final Object pOther) {
        if (this == pOther) {
            return true;
        }
        if (pOther == null || getClass() != pOther.getClass()) {
            return false;
        }
        final InstanceVariable that = (InstanceVariable) pOther;
        return Objects.equals(owner, that.owner)
                && access == that.access
                && Objects.equals(name, that.name)
                && Objects.equals(descriptor, that.descriptor)
                && Objects.equals(signature, that.signature)
                && lineNumber == that.lineNumber;
    }

    @Override
    public String toString() {
        return "InstanceVariable{"
                + "owner='"
                + owner
                + '\''
                + ", access='"
                + access
                + '\''
                + ", name='"
                + name
                + '\''
                + ", descriptor='"
                + descriptor
                + '\''
                + ", signature="
                + signature
                + ", lineNumber='"
                + lineNumber
                + '\''
                + ","
                + " holder: {"
                + holder
                + "}"
                + '}';
    }
}
