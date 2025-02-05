package at.redeye.FrameWork.utilities.calendar;

public class Easterformular {

    private final int jahr;

    public Easterformular(int jahr) {
        this.jahr = jahr;
    }

    /*
     * @return Wenn der return Wert < 31 ist, dann ist es der Tag im März.
     * Ansonsten 31 abziehen und es ist der April
     */
    public int easterday() {
        int a, b, c, k, p, q, M, N, d, e;

        a = jahr % 19;
        b = jahr % 4;
        c = jahr % 7;
        k = jahr / 100;
        p = (8 * k + 13) / 25;
        q = k / 4;
        M = (15 + k - p - q) % 30;
        N = (4 + k - q) % 7;
        d = (19 * a + M) % 30;
        e = (2 * b + 4 * c + 6 * d + N) % 7;

        if (d + e == 35)
            return 50;
        else if (d == 28 && e == 6 && (11 * M + 11) % 30 < 19)
            return 49;
        else
            return 22 + d + e;
    }
}
