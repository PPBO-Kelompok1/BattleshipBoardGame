package input;

public interface GameCallback {
    void requestCoordinates(String prompt, CoordConsumer consumer, CoordTarget target);
    void showMessage(String message);

    default int getAttacksLeft() {
        return Integer.MAX_VALUE;
    }

    default boolean canUseAttack() {
        return getAttacksLeft() > 0;
    }

    default boolean consumeAttack() {
        return true;
    }

    default boolean isGameOver() {
        return false;
    }
}
