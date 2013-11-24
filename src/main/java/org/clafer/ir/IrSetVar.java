package org.clafer.ir;

import org.clafer.common.Check;

/**
 *
 * @author jimmy
 */
public class IrSetVar extends IrAbstractSet implements IrVar {

    private final String name;

    protected IrSetVar(String name, IrDomain env, IrDomain ker, IrDomain card) {
        super(env, ker, card);
        this.name = Check.notNull(name);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public <A, B> B accept(IrSetExprVisitor<A, B> visitor, A a) {
        return visitor.visit(this, a);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        // TODO: should only print name
        return name + "{env=" + getEnv() + ", ker=" + getKer() + ", card=" + getCard() + "}";
    }
}
