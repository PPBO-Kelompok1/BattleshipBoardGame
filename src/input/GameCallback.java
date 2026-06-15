package input;

public interface GameCallback {
    void requestCoordinates(String prompt, CoordConsumer consumer, CoordTarget target);
    void showMessage(String message);
}
