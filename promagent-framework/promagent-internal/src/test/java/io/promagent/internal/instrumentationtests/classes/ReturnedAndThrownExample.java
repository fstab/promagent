package io.promagent.internal.instrumentationtests.classes;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class ReturnedAndThrownExample implements IReturnedAndThrownExample {

    @Override
    public void returnVoid(Fruit f) {}

    @Override
    public int returnPrimitive(Fruit.Orange orange) {
        return 42;
    }

    @Override
    public Fruit returnObject() {
        return new Fruit.Orange();
    }

    @Override
    public int[] returnArray(int... params) {
        return new int[] {23, 42};
    }

    @Override
    public <T extends Fruit> List<T> returnGenerics(T fruit) {
        return Collections.singletonList(fruit);
    }

    @Override
    public String throwsRuntimeException(int a, Fruit.Orange b) {
        Object n = null;
        return n.toString(); // throws NullPointerException
    }

    @Override
    public int throwsCheckedException() throws IOException {
        throw new IOException();
    }
}
