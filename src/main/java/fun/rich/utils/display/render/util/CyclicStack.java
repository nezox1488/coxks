package fun.rich.utils.display.render.util;

import java.util.Stack;


public class CyclicStack<T> extends Stack<T> {
    int index = 0;

    public T next() {
        index = (index + 1) % size();
        return get(index);
    }
}
