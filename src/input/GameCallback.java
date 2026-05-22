package input;

public interface GameCallback {
    void requestCoordinates(String prompt, CoordConsumer consumer);
    void showMessage(String message);
}
