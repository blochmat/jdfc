package com.jdfc.core.analysis.ifg.data;

import java.util.Objects;

public class Field {

    protected final String owner;
    protected final int access;
    protected final String name;
    protected final String descriptor;
    protected final String signature;

    protected Field(final String pOwner,
                  final int pAccess,
                  final String pName,
                  final String pDescriptor,
                  final String pSignature) {
        owner = pOwner;
        access = pAccess;
        name = pName;
        descriptor = pDescriptor;
        signature = pSignature;
    }

    public static Field create(final String pOwner,
                               final int pAccess,
                               final String pName,
                               final String pDescriptor,
                               final String pSignature) {
        return new Field(pOwner, pAccess, pName, pDescriptor, pSignature);
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

    @Override
    public int hashCode() {
        return Objects.hash(owner, access, name, descriptor, signature);
    }

    @Override
    public boolean equals(final Object pOther) {
        if (this == pOther) {
            return true;
        }
        if (pOther == null || getClass() != pOther.getClass()) {
            return false;
        }
        final Field that = (Field) pOther;
        return Objects.equals(owner, that.owner)
                && access == that.access
                && Objects.equals(name, that.name)
                && Objects.equals(descriptor, that.descriptor)
                && Objects.equals(signature, that.signature);
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
                + '\''
                + '}';
    }
}
