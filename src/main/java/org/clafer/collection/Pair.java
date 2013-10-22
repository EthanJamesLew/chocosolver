package org.clafer.collection;

import org.clafer.common.Check;
import org.clafer.common.Util;

/**
 * A 2-tuple.
 *
 *
 * @param <A> the type of fst
 * @param <B> the type of snd
 * @author jimmy
 */
public class Pair<A, B> {

    private final A fst;
    private final B snd;

    /**
     * Construct a tuple.
     *
     * @param fst
     * @param snd
     */
    public Pair(A fst, B snd) {
        this.fst = Check.notNull(fst);
        this.snd = Check.notNull(snd);
    }

    public Pair(Pair<? extends A, ? extends B> pair) {
        this(pair.getFst(), pair.getSnd());
    }

    public A getFst() {
        return fst;
    }

    public B getSnd() {
        return snd;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Pair<?, ?>) {
            Pair<?, ?> other = (Pair<?, ?>) obj;
            return Util.equals(fst, other.fst) && Util.equals(snd, other.snd);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Util.hashCode(fst) ^ Util.hashCode(snd.hashCode());
    }

    @Override
    public String toString() {
        return "(" + fst + ", " + snd + ")";
    }
}
