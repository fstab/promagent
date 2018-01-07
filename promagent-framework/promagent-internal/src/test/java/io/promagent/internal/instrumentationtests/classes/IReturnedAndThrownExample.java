package io.promagent.internal.instrumentationtests.classes;

import java.io.IOException;
import java.util.List;

public interface IReturnedAndThrownExample {

    // TODO: Some more return types that should be tested: Enums, Lambdas

    void returnVoid(Fruit f);

    int returnPrimitive(Fruit.Orange orange);

    Fruit returnObject();

    int[] returnArray(int... params);

    <T extends Fruit> List<T> returnGenerics(T fruit);

    String throwsRuntimeException(int a, Fruit.Orange b);

    int throwsCheckedException() throws IOException;
}
