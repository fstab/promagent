package io.promagent.internal;

public class HookInstance {

    private final Object instance;
    private final boolean isRecursiveCall; // true if we have taken an existing instance from the ThreadLocal

    public HookInstance(Object instance, boolean isRecursiveCall) {
        this.instance = instance;
        this.isRecursiveCall = isRecursiveCall;
    }

    public Object getInstance() {
        return instance;
    }

    public boolean isRecursiveCall() {
        return isRecursiveCall;
    }
}
