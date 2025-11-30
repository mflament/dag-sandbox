package org.yah.test.marshall;

import com.sun.jna.FromNativeContext;
import com.sun.jna.NativeMapped;

public interface NativeEnum extends NativeMapped {

    int nativeValue();

    @Override
    default Class<?> nativeType() {
        return int.class;
    }

    @Override
    default Object fromNative(Object nativeValue, FromNativeContext context) {
        return resolve(this.getClass(), (int) nativeValue);
    }

    @Override
    default Object toNative() {
        return nativeValue();
    }

    @SuppressWarnings("unchecked")
    static <E extends Enum<?> & NativeEnum> E resolve(Class<?> type, int nativeValue) {
        NativeEnum[] enumConstants = (NativeEnum[]) type.getEnumConstants();
        for (NativeEnum enumConstant : enumConstants) {
            if (enumConstant.nativeValue() == nativeValue) return (E) enumConstant;
        }
        throw new IllegalArgumentException("Unresolved enum " + type.getTypeName() + " for value " + nativeValue);
    }
}
