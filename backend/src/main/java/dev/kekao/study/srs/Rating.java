package dev.kekao.study.srs;

/**
 * Four-grade answer quality scale used by FSRS-compatible algorithms.
 */
public enum Rating {
    AGAIN(1),
    HARD(2),
    GOOD(3),
    EASY(4);

    private final int value;

    Rating(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
