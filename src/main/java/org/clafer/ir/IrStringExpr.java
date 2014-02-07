package org.clafer.ir;


/**
 * An immutable expression that evaluates to a string.
 *
 * @author jimmy
 */
public interface IrStringExpr extends IrExpr {

    /**
     * The length of the string.
     *
     * @return the length of the string
     */
    public IrIntExpr getLength();

    /**
     * The characters of the string.
     * 
     * @return the characters of the string
     */
    public IrIntExpr[] getChars() ;

    /**
     * Dynamic dispatch on the visitor.
     *
     * @param <A> the parameter type
     * @param <B> the return type
     * @param visitor the visitor
     * @param a the parameter
     * @return the return value
     */
    public <A, B> B accept(IrStringExprVisitor<A, B> visitor, A a);
}
