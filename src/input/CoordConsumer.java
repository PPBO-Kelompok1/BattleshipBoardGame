package input;

@FunctionalInterface
public interface CoordConsumer {
    void accept(int row, int col);
}
