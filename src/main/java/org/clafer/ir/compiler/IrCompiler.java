package org.clafer.ir.compiler;

import java.util.ArrayList;
import java.util.Collections;
import org.clafer.ir.IrAllDifferent;
import org.clafer.ir.IrArrayToSet;
import org.clafer.ir.IrElement;
import org.clafer.ir.IrIfOnlyIf;
import org.clafer.ir.IrIfThenElse;
import org.clafer.ir.IrIntExpr;
import org.clafer.ir.IrWithin;
import org.clafer.ir.IrMember;
import org.clafer.ir.IrNotWithin;
import org.clafer.ir.IrNotImplies;
import org.clafer.ir.IrNotMember;
import org.clafer.ir.IrOr;
import org.clafer.ir.IrSelectN;
import org.clafer.ir.IrSetExpr;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.clafer.ir.IrNot;
import org.clafer.ir.IrSetTest;
import org.clafer.ir.IrSingleton;
import org.clafer.ir.IrSortStrings;
import org.clafer.ir.IrSetUnion;
import org.clafer.ir.IrAnd;
import org.clafer.common.Check;
import org.clafer.choco.constraint.Constraints;
import org.clafer.collection.Pair;
import org.clafer.collection.Triple;
import org.clafer.common.Util;
import org.clafer.ir.IrAdd;
import org.clafer.ir.IrBoolChannel;
import org.clafer.ir.IrBoolDomain;
import org.clafer.ir.IrIntChannel;
import org.clafer.ir.IrJoinRelation;
import org.clafer.ir.IrJoinFunction;
import org.clafer.ir.IrCard;
import solver.variables.SetVar;
import solver.constraints.Constraint;
import org.clafer.ir.IrBoolExpr;
import org.clafer.ir.IrBoolExprVisitor;
import org.clafer.ir.IrBoolVar;
import org.clafer.ir.IrDomain;
import org.clafer.ir.IrImplies;
import org.clafer.ir.IrCompare;
import org.clafer.ir.IrCount;
import org.clafer.ir.IrDiv;
import org.clafer.ir.IrExpr;
import org.clafer.ir.IrFilterString;
import org.clafer.ir.IrIntExprVisitor;
import org.clafer.ir.IrIntVar;
import org.clafer.ir.IrLone;
import org.clafer.ir.IrMask;
import org.clafer.ir.IrMinus;
import org.clafer.ir.IrModule;
import org.clafer.ir.IrMul;
import org.clafer.ir.IrOffset;
import org.clafer.ir.IrOne;
import org.clafer.ir.IrSetDifference;
import org.clafer.ir.IrSetExprVisitor;
import org.clafer.ir.IrSetIntersection;
import org.clafer.ir.IrSetSum;
import org.clafer.ir.IrSetTernary;
import org.clafer.ir.IrSetVar;
import org.clafer.ir.IrSortSets;
import org.clafer.ir.IrSortStringsChannel;
import org.clafer.ir.IrSubsetEq;
import org.clafer.ir.IrTernary;
import org.clafer.ir.IrUtil;
import org.clafer.ir.IrVar;
import org.clafer.ir.IrXor;
import org.clafer.ir.analysis.AnalysisUtil;
import org.clafer.ir.analysis.Canonicalizer;
import org.clafer.ir.analysis.Coalescer;
import org.clafer.ir.analysis.CommonSubexpression;
import org.clafer.ir.analysis.DuplicateConstraints;
import org.clafer.ir.analysis.Optimizer;
import solver.Solver;
import solver.constraints.ICF;
import solver.constraints.Operator;
import solver.constraints.set.SCF;
import solver.variables.BoolVar;
import solver.variables.IntVar;
import solver.variables.VF;

/**
 * Compile from IR to Choco.
 *
 * @author jimmy
 */
public class IrCompiler {

    private final Solver solver;
    private final boolean coalesceVariables;
    private int varNum = 0;

    private IrCompiler(Solver solver, boolean coalesceVariables) {
        this.solver = Check.notNull(solver);
        this.coalesceVariables = coalesceVariables;
    }

    public static IrSolutionMap compile(IrModule in, Solver out) {
        return compile(in, out, true);
    }

    public static IrSolutionMap compile(IrModule in, Solver out, boolean coalesceVariables) {
        IrCompiler compiler = new IrCompiler(out, coalesceVariables);
        return compiler.compile(in);
    }

    private IrSolutionMap compile(IrModule module) {
        IrModule optModule = Optimizer.optimize(Canonicalizer.canonical(module));

        List<IrBoolExpr> constraints = optModule.getConstraints();
        Map<IrIntVar, IrIntVar> coalescedIntVars = Collections.emptyMap();
        Map<IrSetVar, IrSetVar> coalescedSetVars = Collections.emptyMap();
        if (coalesceVariables) {
            Triple<Map<IrIntVar, IrIntVar>, Map<IrSetVar, IrSetVar>, IrModule> coalesceTriple = Coalescer.coalesce(optModule);
            coalescedIntVars = coalesceTriple.getFst();
            coalescedSetVars = coalesceTriple.getSnd();
            optModule = coalesceTriple.getThd();
            while (!coalesceTriple.getFst().isEmpty()
                    || !coalesceTriple.getSnd().isEmpty()) {
                coalesceTriple = Coalescer.coalesce(optModule);
                coalescedIntVars = compose(coalescedIntVars, coalesceTriple.getFst());
                coalescedSetVars = compose(coalescedSetVars, coalesceTriple.getSnd());
                optModule = coalesceTriple.getThd();
            }
            optModule = DuplicateConstraints.removeDuplicates(optModule);

            constraints = new ArrayList<IrBoolExpr>(optModule.getConstraints().size());
            for (IrBoolExpr constraint : optModule.getConstraints()) {
                Pair<IrIntExpr, IrSetVar> cardinality = AnalysisUtil.getAssignCardinality(constraint);
                if (cardinality != null && cardinality.getFst() instanceof IrIntVar) {
                    IrIntVar leftInt = (IrIntVar) cardinality.getFst();
                    SetVar rightSet = getSetVar(cardinality.getSnd());

                    CSet duplicate = cardIntVarMap.get(leftInt);
                    if (duplicate == null) {
                        cardIntVarMap.put(leftInt,
                                new CSet(rightSet,
                                IrUtil.intersection(leftInt.getDomain(), cardinality.getSnd().getCard())));
                    } else {
                        /*
                         * Case where
                         *     c = |A|
                         *     c = |B|
                         */
                        cardIntVarMap.put(leftInt, new CSet(rightSet, duplicate.getCard()));
                        post(SCF.cardinality(rightSet, duplicate.getCard()));
                    }
                } else {
                    constraints.add(constraint);
                }
            }
        }

        commonSubexpressions.addAll(CommonSubexpression.findCommonSubexpressions(optModule));

        for (IrBoolExpr constraint : constraints) {
            post(compileAsConstraint(constraint));
        }
        for (IrVar variable : optModule.getVariables()) {
            if (variable instanceof IrBoolVar) {
                compile((IrBoolVar) variable);
            } else if (variable instanceof IrIntVar) {
                compile((IrIntVar) variable);
            } else {
                compile((IrSetVar) variable);
            }
        }
        return new IrSolutionMap(
                coalescedIntVars, intVarMap,
                coalescedSetVars, setVarMap);
    }

    private static <T> Map<T, T> compose(Map<T, T> f1, Map<T, T> f2) {
        if (f2.isEmpty()) {
            return f1;
        }
        Map<T, T> composed = new HashMap<T, T>(f2);
        for (Entry<T, T> e : f1.entrySet()) {
            T key = e.getKey();
            T value = f2.get(e.getValue());
            if (value == null) {
                value = e.getValue();
            }
            composed.put(key, value);
        }
        return composed;
    }
    private final Map<SetVar, IntVar> cachedSetCardVars = new HashMap<SetVar, IntVar>();
    private final Map<IntVar, IntVar> cachedMinus = new HashMap<IntVar, IntVar>();
    private final Map<Pair<IntVar, Integer>, IntVar> cachedOffset = new HashMap<Pair<IntVar, Integer>, IntVar>();
    private final Set<IrExpr> commonSubexpressions = new HashSet<IrExpr>();
    private final Map<IrIntExpr, IntVar> cachedCommonIntSubexpressions = new HashMap<IrIntExpr, IntVar>();
    private final Map<IrSetExpr, CSet> cachedCommonSetSubexpressions = new HashMap<IrSetExpr, CSet>();

    private void post(Constraint constraint) {
        if (!solver.TRUE.equals(constraint)) {
            solver.post(constraint);
        }
    }

    private BoolVar boolVar(String name, IrBoolDomain domain) {
        switch (domain) {
            case TrueDomain:
                return VF.one(solver);
            case FalseDomain:
                return VF.zero(solver);
            default:
                return VF.bool(name, solver);
        }
    }

    private IntVar intVar(String name, IrDomain domain) {
        if (domain.size() == 1) {
            int constant = domain.getLowBound();
            switch (domain.getLowBound()) {
                case 0:
                    return VF.zero(solver);
                case 1:
                    return VF.one(solver);
                default:
                    return VF.fixed(constant, solver);
            }
        }
        if (domain.getLowBound() == 0 && domain.getHighBound() == 1) {
            return VF.bool(name, solver);
        }
        if (domain.isBounded()) {
            return VF.enumerated(name, domain.getLowBound(), domain.getHighBound(), solver);
        }
        return VF.enumerated(name, domain.getValues(), solver);
    }

    private SetVar setVar(String name, IrDomain env, IrDomain ker) {
        assert IrUtil.isSubsetOf(ker, env);
        if (env.size() == ker.size()) {
            int[] values = ker.getValues();
            return VF.set(name, values, values, solver);
        }
        return VF.set(name, env.getValues(), ker.getValues(), solver);
    }

    private CSet cset(String name, IrDomain env, IrDomain ker, IrDomain card) {
        SetVar set = setVar(name, env, ker);
        return new CSet(set, card);
    }

    private IntVar setCardVar(SetVar set, IrDomain card) {
        IntVar setCardVar = cachedSetCardVars.get(set);
        if (setCardVar == null) {
            setCardVar = intVar("|" + set.getName() + "|", card);
            if (!(set.instantiated() && card.size() == 1 && card.getLowBound() == set.getKernelSize())) {
                post(SCF.cardinality(set, setCardVar));
            }
            assert !cachedSetCardVars.containsKey(set);
            cachedSetCardVars.put(set, setCardVar);
        }
        return setCardVar;
    }

    private BoolVar numBoolVar(String name) {
        return boolVar(name + "#" + varNum++, IrBoolDomain.BoolDomain);
    }

    private IntVar numIntVar(String name, IrDomain domain) {
        return intVar(name + "#" + varNum++, domain);
    }

    private SetVar numSetVar(String name, IrDomain env, IrDomain ker) {
        return setVar(name + "#" + varNum++, env, ker);
    }

    private CSet numCset(String name, IrDomain env, IrDomain ker, IrDomain card) {
        return cset(name + "#" + varNum++, env, ker, card);
    }

    private BoolVar getBoolVar(IrBoolVar var) {
        BoolVar bool = (BoolVar) intVarMap.get(var);
        if (bool == null) {
            CSet set = cardIntVarMap.get(var);
            bool = set == null ? boolVar(var.getName(), var.getDomain()) : (BoolVar) set.getCard();
            intVarMap.put(var, bool);
        }
        return bool;
    }

    private IntVar getIntVar(IrIntVar var) {
        IntVar iint = intVarMap.get(var);
        if (iint == null) {
            CSet set = cardIntVarMap.get(var);
            iint = set == null ? intVar(var.getName(), var.getDomain()) : set.getCard();
            intVarMap.put(var, iint);
        }
        return iint;
    }
    private final Map<IrIntVar, IntVar> intVarMap = new HashMap<IrIntVar, IntVar>();
    private final Map<IrIntVar, CSet> cardIntVarMap = new HashMap<IrIntVar, CSet>();

    private SetVar getSetVar(IrSetVar var) {
        SetVar set = setVarMap.get(var);
        if (set == null) {
            set = setVar(var.getName(), var.getEnv(), var.getKer());
            setVarMap.put(var, set);
        }
        return set;
    }
    private final Map<IrSetVar, SetVar> setVarMap = new HashMap<IrSetVar, SetVar>();

    private BoolVar asBoolVar(Object obj) {
        if (obj instanceof Constraint) {
            return asBoolVar((Constraint) obj);
        }
        return (BoolVar) obj;
    }

    private BoolVar asBoolVar(Constraint op) {
        return op.reif();
    }

    private BoolVar compileAsBoolVar(IrBoolExpr expr) {
        return asBoolVar(expr.accept(boolExprCompiler, BoolVarNoReify));
    }

    private BoolVar[] compileAsBoolVars(IrBoolExpr[] exprs) {
        BoolVar[] vars = new BoolVar[exprs.length];
        for (int i = 0; i < vars.length; i++) {
            vars[i] = compileAsBoolVar(exprs[i]);
        }
        return vars;
    }

    private IntVar compileAsIntVar(IrBoolExpr expr) {
        return asBoolVar(expr.accept(boolExprCompiler, BoolVarNoReify));
    }

    private IntVar[] compileAsIntVars(IrBoolExpr[] exprs) {
        IntVar[] vars = new IntVar[exprs.length];
        for (int i = 0; i < vars.length; i++) {
            vars[i] = compileAsIntVar(exprs[i]);
        }
        return vars;
    }

    private Constraint asConstraint(Object obj) {
        if (obj instanceof BoolVar) {
            return asConstraint((BoolVar) obj);
        }
        return (Constraint) obj;
    }

    private Constraint asConstraint(BoolVar var) {
        return _arithm(var, "=", 1);
    }

    private Constraint compileAsConstraint(IrBoolExpr expr) {
        return asConstraint(expr.accept(boolExprCompiler, ConstraintNoReify));
    }

    private Constraint compileAsConstraint(IrBoolExpr expr, BoolVar reify) {
        BoolArg arg = new BoolArg(reify, Preference.Constraint);
        Constraint constraint = asConstraint(expr.accept(boolExprCompiler, arg));
        if (arg.hasReify()) {
            // The compliation failed to reify, explicitly reify now.
            return _arithm(arg.useReify(), "=", constraint.reif());
        }
        return constraint;
    }

    private Constraint compileAsConstraint(IrIntExpr expr, IntVar reify) {
        Object result = commonSubexpressions.contains(expr)
                ? compile(expr)
                : expr.accept(intExprCompiler, reify);
        if (result instanceof IntVar) {
            // The compliation failed to reify, explicitly reify now.
            return _arithm(reify, "=", (IntVar) result);
        }
        return (Constraint) result;
    }

    private Constraint compileAsConstraint(IrSetExpr expr, CSet reify) {
        Object result = commonSubexpressions.contains(expr)
                ? compile(expr)
                : expr.accept(setExprCompiler, reify);
        if (result instanceof CSet) {
            CSet set = (CSet) result;
            // The compliation failed to reify, explicitly reify now.
            return _equal(reify, set);
        }
        return (Constraint) result;
    }

    private Object compile(IrBoolExpr expr) {
        return expr.accept(boolExprCompiler, ConstraintNoReify);
    }

    private IntVar compile(IrIntExpr expr) {
        IntVar var = cachedCommonIntSubexpressions.get(expr);
        if (var == null) {
            var = (IntVar) expr.accept(intExprCompiler, null);
            if (commonSubexpressions.contains(expr)) {
                cachedCommonIntSubexpressions.put(expr, var);
            }
        }
        return var;
    }

    private IntVar[] compile(IrIntExpr[] exprs) {
        IntVar[] vars = new IntVar[exprs.length];
        for (int i = 0; i < vars.length; i++) {
            vars[i] = compile(exprs[i]);
        }
        return vars;
    }

    private CSet compile(IrSetExpr expr) {
        CSet set = cachedCommonSetSubexpressions.get(expr);
        if (set == null) {
            set = (CSet) expr.accept(setExprCompiler, null);
            if (commonSubexpressions.contains(expr)) {
                cachedCommonSetSubexpressions.put(expr, set);
            }
        }
        return set;
    }

    private CSet[] compile(IrSetExpr[] exprs) {
        CSet[] vars = new CSet[exprs.length];
        for (int i = 0; i < vars.length; i++) {
            vars[i] = compile(exprs[i]);
        }
        return vars;
    }
    private final IrBoolExprVisitor<BoolArg, Object> boolExprCompiler = new IrBoolExprVisitor<BoolArg, Object>() {
        @Override
        public Object visit(IrBoolVar ir, BoolArg a) {
            return getBoolVar(ir);
        }

        @Override
        public Object visit(IrNot ir, BoolArg a) {
            BoolVar var = compileAsBoolVar(ir.getExpr());
            if (a.hasReify()) {
                return _arithm(a.useReify(), "!=", var);
            }
            return Preference.Constraint.equals(a.getPreference())
                    ? _arithm(var, "=", 0)
                    : var.not();
        }

        @Override
        public Object visit(IrAnd ir, BoolArg a) {
            return _and(compileAsBoolVars(ir.getOperands()));
        }

        @Override
        public Object visit(IrLone ir, BoolArg a) {
            return _lone(compileAsBoolVars(ir.getOperands()));
        }

        @Override
        public Object visit(IrOne ir, BoolArg a) {
            return _one(compileAsBoolVars(ir.getOperands()));
        }

        @Override
        public Object visit(IrOr ir, BoolArg a) {
            return _or(compileAsBoolVars(ir.getOperands()));
        }

        @Override
        public Object visit(IrImplies ir, BoolArg a) {
            BoolVar antecedent = compileAsBoolVar(ir.getAntecedent());
            IntVar consequent = compileAsIntVar(ir.getConsequent());
            return _implies(antecedent, consequent);
        }

        @Override
        public Object visit(IrNotImplies ir, BoolArg a) {
            BoolVar $antecedent = compileAsBoolVar(ir.getAntecedent());
            IntVar $consequent = compileAsIntVar(ir.getConsequent());
            return _not_implies($antecedent, $consequent);
        }

        @Override
        public Object visit(IrIfThenElse ir, BoolArg a) {
            BoolVar antecedent = compileAsBoolVar(ir.getAntecedent());
            BoolVar consequent = compileAsBoolVar(ir.getConsequent());
            BoolVar alternative = compileAsBoolVar(ir.getAlternative());
            return _ifThenElse(antecedent, consequent, alternative);
        }

        @Override
        public Object visit(IrIfOnlyIf ir, BoolArg a) {
            if (ir.getLeft() instanceof IrCompare) {
                BoolVar right = compileAsBoolVar(ir.getRight());
                return compileAsConstraint(ir.getLeft(), right);
            }
            if (ir.getRight() instanceof IrCompare) {
                BoolVar left = compileAsBoolVar(ir.getLeft());
                return compileAsConstraint(ir.getRight(), left);
            }
            if (ir.getRight() instanceof IrBoolVar) {
                return compileAsConstraint(ir.getLeft(), compileAsIntVar(ir.getRight()));
            }
            return compileAsConstraint(ir.getRight(), compileAsIntVar(ir.getLeft()));
        }

        @Override
        public Object visit(IrXor ir, BoolArg a) {
            IntVar $left = compileAsIntVar(ir.getLeft());
            IntVar $right = compileAsIntVar(ir.getRight());
            return _arithm($left, "!=", $right);
        }

        @Override
        public Object visit(IrWithin ir, BoolArg a) {
            IntVar var = compile(ir.getValue());
            IrDomain range = ir.getRange();
            if (range.isBounded()) {
                return _within(var, range.getLowBound(), range.getHighBound());
            }
            return _within(var, range.getValues());
        }

        @Override
        public Object visit(IrNotWithin ir, BoolArg a) {
            IntVar var = compile(ir.getValue());
            IrDomain range = ir.getRange();
            if (range.isBounded()) {
                return _not_within(var, range.getLowBound(), range.getHighBound());
            }
            return _not_within(var, range.getValues());
        }

        @Override
        public Object visit(IrCompare ir, BoolArg a) {
            Object opt = compileCompareConstant(ir.getLeft(), ir.getOp(), ir.getRight(), a);
            if (opt == null) {
                opt = compileCompareConstant(ir.getRight(), ir.getOp().reverse(), ir.getLeft(), a);
            }
            if (opt != null) {
                return opt;
            }
            Triple<String, IrIntExpr, Integer> offset = getOffset(ir.getLeft());
            if (offset != null) {
                return _arithm(compile(ir.getRight()), offset.getFst(),
                        compile(offset.getSnd()), ir.getOp().getSyntax(), offset.getThd().intValue());
            }
            offset = getOffset(ir.getRight());
            if (offset != null) {
                return _arithm(compile(ir.getLeft()), offset.getFst(),
                        compile(offset.getSnd()), ir.getOp().getSyntax(), offset.getThd().intValue());
            }
            if (IrCompare.Op.Equal.equals(ir.getOp())) {
                if (ir.getRight() instanceof IrIntVar) {
                    return compileAsConstraint(ir.getLeft(), compile(ir.getRight()));
                }
                return compileAsConstraint(ir.getRight(), compile(ir.getLeft()));
            }
            return _arithm(compile(ir.getLeft()), ir.getOp().getSyntax(), compile(ir.getRight()));
        }

        /*
         * Optimize when one of the operands is a constant.
         */
        private Object compileCompareConstant(IrIntExpr left, IrCompare.Op op, IrIntExpr right, BoolArg a) {
            Integer constant = IrUtil.getConstant(right);
            if (constant != null) {
                boolean preferBoolVar = Preference.BoolVar.equals(a.getPreference());
                if (op.isEquality() && (a.hasReify() || preferBoolVar)) {
                    BoolVar reify = a.hasReify() ? a.useReify() : numBoolVar("ReifyEquality");
                    Constraint constraint =
                            IrCompare.Op.Equal.equals(op)
                            ? Constraints.reifyEqual(reify, compile(left), constant.intValue())
                            : Constraints.reifyNotEqual(reify, compile(left), constant.intValue());
                    if (preferBoolVar) {
                        post(constraint);
                        return reify;
                    }
                    return constraint;
                }
                if (IrCompare.Op.Equal.equals(op)) {
                    return compileAsConstraint(left, compile(right));
                }
                return _arithm(compile(left), op.getSyntax(), constant.intValue());
            }
            return null;
        }

        private Triple<String, IrIntExpr, Integer> getOffset(IrIntExpr expr) {
            if (expr instanceof IrAdd) {
                IrAdd add = (IrAdd) expr;
                IrIntExpr[] addends = add.getAddends();
                if (addends.length == 1) {
                    return new Triple<String, IrIntExpr, Integer>("-", addends[0], add.getOffset());
                }
            }
            return null;
        }

        @Override
        public Object visit(IrSetTest ir, BoolArg a) {
            switch (ir.getOp()) {
                case Equal:
                    if (ir.getRight() instanceof IrSetVar) {
                        return compileAsConstraint(ir.getLeft(), compile(ir.getRight()));
                    }
                    return compileAsConstraint(ir.getRight(), compile(ir.getLeft()));
                case NotEqual:
                    return _not_equal(compile(ir.getLeft()), compile(ir.getRight()));
                default:
                    throw new IllegalArgumentException("Unexpected operator.");
            }
        }

        @Override
        public Object visit(IrMember ir, BoolArg a) {
            return _member(compile(ir.getElement()), compile(ir.getSet()).getSet());
        }

        @Override
        public Object visit(IrNotMember ir, BoolArg a) {
            return _not_member(compile(ir.getElement()), compile(ir.getSet()).getSet());
        }

        @Override
        public Object visit(IrSubsetEq ir, BoolArg a) {
            return _subset_eq(compile(ir.getSubset()), compile(ir.getSuperset()));
        }

        @Override
        public Constraint visit(IrBoolChannel ir, BoolArg a) {
            BoolVar[] bools = compileAsBoolVars(ir.getBools());
            CSet set = compile(ir.getSet());
            return SCF.bool_channel(bools, set.getSet(), 0);
        }

        @Override
        public Constraint visit(IrIntChannel ir, BoolArg a) {
            IntVar[] ints = compile(ir.getInts());
            CSet[] sets = compile(ir.getSets());
            return Constraints.intChannel(mapSet(sets), ints);
        }

        @Override
        public Object visit(IrSortStrings ir, BoolArg a) {
            IntVar[][] strings = new IntVar[ir.getStrings().length][];
            for (int i = 0; i < strings.length; i++) {
                strings[i] = compile(ir.getStrings()[i]);
            }
            return ir.isStrict() ? _lex_chain_less(strings) : _lex_chain_less_eq(strings);
        }

        @Override
        public Object visit(IrSortSets ir, BoolArg a) {
            CSet[] sets = compile(ir.getSets());
            return Constraints.sortedSets(mapSet(sets), mapCard(sets));
        }

        @Override
        public Object visit(IrSortStringsChannel ir, BoolArg a) {
            IntVar[][] strings = new IntVar[ir.getStrings().length][];
            for (int i = 0; i < strings.length; i++) {
                strings[i] = compile(ir.getStrings()[i]);
            }
            return _lex_chain_channel(strings, compile(ir.getInts()));
        }

        @Override
        public Constraint visit(IrAllDifferent ir, BoolArg a) {
            IrIntExpr[] operands = ir.getOperands();

            IntVar[] $operands = new IntVar[operands.length];
            for (int i = 0; i < $operands.length; i++) {
                $operands[i] = compile(operands[i]);
            }
            return _all_different($operands);
        }

        @Override
        public Constraint visit(IrSelectN ir, BoolArg a) {
            IrBoolExpr[] bools = ir.getBools();
            IrIntExpr n = ir.getN();
            BoolVar[] $bools = new BoolVar[bools.length];
            for (int i = 0; i < $bools.length; i++) {
                $bools[i] = compileAsBoolVar(bools[i]);
            }
            IntVar $n = compile(n);
            return Constraints.selectN($bools, $n);
        }

        @Override
        public Object visit(IrFilterString ir, BoolArg a) {
            return _filter_string(compile(ir.getSet()).getSet(), ir.getOffset(), compile(ir.getString()), compile(ir.getResult()));
        }
    };
    private final IrIntExprVisitor<IntVar, Object> intExprCompiler = new IrIntExprVisitor<IntVar, Object>() {
        @Override
        public IntVar visit(IrIntVar ir, IntVar reify) {
            return getIntVar(ir);
        }

        @Override
        public IntVar visit(IrMinus ir, IntVar reify) {
            IntVar expr = compile(ir.getExpr());
            IntVar minus = cachedMinus.get(expr);
            if (minus == null) {
                minus = VF.minus(compile(ir.getExpr()));
                cachedMinus.put(expr, minus);
            }
            return minus;
        }

        @Override
        public Object visit(IrCard ir, IntVar reify) {
            CSet set = compile(ir.getSet());
            return set.getCard();
        }

        private Pair<Integer, IrIntExpr> getCoefficient(IrIntExpr e) {
            if (e instanceof IrMinus) {
                return new Pair<Integer, IrIntExpr>(-1, ((IrMinus) e).getExpr());
            } else if (e instanceof IrMul) {
                IrMul mul = (IrMul) e;
                Integer constant = IrUtil.getConstant(mul.getMultiplicand());
                if (constant != null) {
                    return new Pair<Integer, IrIntExpr>(constant, mul.getMultiplier());
                }
                constant = IrUtil.getConstant(mul.getMultiplier());
                if (constant != null) {
                    return new Pair<Integer, IrIntExpr>(constant, mul.getMultiplicand());
                }
            }
            return new Pair<Integer, IrIntExpr>(1, e);
        }

        private Pair<int[], IrIntExpr[]> getCoefficients(IrIntExpr[] es) {
            int[] coefficients = new int[es.length];
            IrIntExpr[] exprs = new IrIntExpr[es.length];
            for (int i = 0; i < es.length; i++) {
                Pair<Integer, IrIntExpr> coef = getCoefficient(es[i]);
                coefficients[i] = coef.getFst();
                exprs[i] = coef.getSnd();
            }
            return new Pair<int[], IrIntExpr[]>(coefficients, exprs);
        }

        @Override
        public Object visit(IrAdd ir, IntVar reify) {
            int offset = ir.getOffset();
            IrIntExpr[] addends = ir.getAddends();
            switch (addends.length) {
                case 0:
                    // This case should have already been optimized earlier.
                    return VF.fixed(offset, solver);
                case 1:
                    if (reify == null) {
                        return _offset(compile(addends[0]), offset);
                    }
                    return _arithm(reify, "-", compile(addends[0]), "=", offset);
                default:
                    Pair<int[], IrIntExpr[]> coeffs = getCoefficients(addends);
                    int[] coefficients = coeffs.getFst();
                    IntVar[] operands = compile(coeffs.getSnd());
                    if (reify == null) {
                        IntVar sum = numIntVar("Sum", IrUtil.offset(ir.getDomain(), -offset));
                        post(_scalar(sum, coefficients, operands));
                        return _offset(sum, offset);
                    }
                    if (offset != 0) {
                        coefficients = Util.cons(1, coefficients);
                        operands = Util.cons(VF.fixed(offset, solver), operands);
                    }
                    return _scalar(reify, coefficients, operands);
            }
        }

        @Override
        public Object visit(IrMul ir, IntVar reify) {
            IrIntExpr multiplicand = ir.getMultiplicand();
            IrIntExpr multiplier = ir.getMultiplier();
            Integer multiplicandConstant = IrUtil.getConstant(multiplicand);
            Integer multiplierConstant = IrUtil.getConstant(multiplier);
            if (multiplicandConstant != null) {
                switch (multiplicandConstant.intValue()) {
                    case 0:
                        return compileAsConstraint(multiplicand, reify);
                    case 1:
                        return compileAsConstraint(multiplier, reify);
                    default:
                        if (multiplicandConstant.intValue() >= -1) {
                            return VF.scale(compile(multiplier), multiplicandConstant.intValue());
                        }
                }
            }
            if (multiplierConstant != null) {
                switch (multiplierConstant.intValue()) {
                    case 0:
                        return compileAsConstraint(multiplier, reify);
                    case 1:
                        return compileAsConstraint(multiplicand, reify);
                    default:
                        if (multiplierConstant.intValue() >= -1) {
                            return VF.scale(compile(multiplicand), multiplierConstant.intValue());
                        }
                }
            }
            if (reify == null) {
                IntVar product = numIntVar("Mul", ir.getDomain());
                post(_times(compile(multiplicand), compile(multiplier), product));
                return product;
            }
            return _times(compile(multiplicand), compile(multiplier), reify);
        }

        @Override
        public Object visit(IrDiv ir, IntVar reify) {
            IrIntExpr dividend = ir.getDividend();
            IrIntExpr divisor = ir.getDivisor();
            if (reify == null) {
                IntVar quotient = numIntVar("Div", ir.getDomain());
                post(_div(compile(dividend), compile(divisor), quotient));
                return quotient;
            }
            return _div(compile(dividend), compile(divisor), reify);
        }

        @Override
        public Object visit(IrElement ir, IntVar reify) {
            if (reify == null) {
                IntVar element = numIntVar("Element", ir.getDomain());
                post(_element(compile(ir.getIndex()), compile(ir.getArray()), element));
                return element;
            }
            return _element(compile(ir.getIndex()), compile(ir.getArray()), reify);
        }

        @Override
        public Object visit(IrCount ir, IntVar reify) {
            IntVar[] array = compile(ir.getArray());
            if (reify == null) {
                IntVar count = numIntVar("Count", ir.getDomain());
                post(_count(ir.getValue(), array, count));
                return count;
            }
            return _count(ir.getValue(), array, reify);
        }

        @Override
        public Object visit(IrSetSum ir, IntVar reify) {
            CSet set = compile(ir.getSet());
            if (reify == null) {
                IntVar sum = numIntVar("SetSum", ir.getDomain());
                post(Constraints.setSum(set.getSet(), set.getCard(), sum));
                return sum;
            }
            return Constraints.setSum(set.getSet(), set.getCard(), reify);
        }

        @Override
        public Object visit(IrTernary ir, IntVar reify) {
            BoolVar antecedent = compileAsBoolVar(ir.getAntecedent());
            IntVar consequent = compile(ir.getConsequent());
            IntVar alternative = compile(ir.getAlternative());
            if (reify == null) {
                IntVar ternary = numIntVar("Ternary", ir.getDomain());
                post(_ifThenElse(antecedent,
                        _reify_equal(ternary, consequent),
                        _reify_equal(ternary, alternative)));
                return ternary;
            }
            return _ifThenElse(antecedent,
                    _reify_equal(reify, consequent),
                    _reify_equal(reify, alternative));
        }

        private Object compileBool(IrBoolExpr expr, IntVar a) {
            if (a instanceof BoolVar) {
                return compileAsConstraint(expr, (BoolVar) a);
            }
            IntVar var = compileAsIntVar(expr);
            return a == null ? var : _arithm(var, "=", a);
        }

        @Override
        public Object visit(IrBoolVar ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrNot ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrAnd ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrLone ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrOne ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrOr ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrImplies ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrNotImplies ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrIfThenElse ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrIfOnlyIf ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrXor ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrWithin ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrNotWithin ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrCompare ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrSetTest ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrMember ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrNotMember ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrSubsetEq ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrBoolChannel ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrIntChannel ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrSortStrings ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrSortSets ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrSortStringsChannel ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrAllDifferent ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrSelectN ir, IntVar a) {
            return compileBool(ir, a);
        }

        @Override
        public Object visit(IrFilterString ir, IntVar a) {
            return compileBool(ir, a);
        }
    };
    private final IrSetExprVisitor<CSet, Object> setExprCompiler = new IrSetExprVisitor<CSet, Object>() {
        @Override
        public Object visit(IrSetVar ir, CSet reify) {
            return new CSet(getSetVar(ir), ir.getCard());
        }

        @Override
        public Object visit(IrSingleton ir, CSet reify) {
            IntVar value = compile(ir.getValue());
            if (reify == null) {
                SetVar singleton = numSetVar("Singleton", ir.getEnv(), ir.getKer());
                post(Constraints.singleton(value, singleton));
                return new CSet(singleton, VF.one(solver));
            }
            return Constraints.singleton(value, reify.getSet(), reify.getCard());
        }

        @Override
        public Object visit(IrArrayToSet ir, CSet reify) {
            IntVar[] array = compile(ir.getArray());
            if (reify == null) {
                CSet set = numCset("ArrayToSet", ir.getEnv(), ir.getKer(), ir.getCard());
                post(Constraints.arrayToSet(array, set.getSet(), set.getCard(), ir.getGlobalCardinality()));
                return set;
            }
            return Constraints.arrayToSet(array, reify.getSet(), reify.getCard(), ir.getGlobalCardinality());
        }

        @Override
        public Object visit(IrJoinRelation ir, CSet reify) {
            CSet take = compile(ir.getTake());
            CSet[] children = compile(ir.getChildren());
            if (reify == null) {
                CSet joinRelation = numCset("JoinRelation", ir.getEnv(), ir.getKer(), ir.getCard());
                if (ir.isInjective()) {
                    post(Constraints.joinInjectiveRelation(take.getSet(), take.getCard(),
                            mapSet(children), mapCard(children), joinRelation.getSet(), joinRelation.getCard()));
                    return joinRelation;
                } else {
                    post(Constraints.joinRelation(take.getSet(), mapSet(children), joinRelation.getSet()));
                    return joinRelation;
                }
            }
            if (ir.isInjective()) {
                return Constraints.joinInjectiveRelation(take.getSet(), take.getCard(),
                        mapSet(children), mapCard(children), reify.getSet(), reify.getCard());
            }
            return Constraints.joinRelation(take.getSet(), mapSet(children), reify.getSet());
        }

        @Override
        public Object visit(IrJoinFunction ir, CSet reify) {
            CSet take = compile(ir.getTake());
            IntVar[] refs = compile(ir.getRefs());
            if (reify == null) {
                CSet joinFunction = numCset("JoinFunction", ir.getEnv(), ir.getKer(), ir.getCard());
                post(Constraints.joinFunction(take.getSet(), take.getCard(), refs, joinFunction.getSet(), joinFunction.getCard(), ir.getGlobalCardinality()));
                return joinFunction;
            }
            return Constraints.joinFunction(take.getSet(), take.getCard(), refs, reify.getSet(), reify.getCard(), ir.getGlobalCardinality());
        }

        @Override
        public Object visit(IrSetDifference ir, CSet reify) {
            CSet minuend = compile(ir.getMinuend());
            CSet subtrahend = compile(ir.getSubtrahend());
            if (reify == null) {
                CSet difference = numCset("Difference", ir.getEnv(), ir.getKer(), ir.getCard());
                post(_difference(minuend, subtrahend, difference));
                return difference;
            }
            return _difference(minuend, subtrahend, reify);
        }

        @Override
        public Object visit(IrSetIntersection ir, CSet reify) {
            CSet[] operands = compile(ir.getOperands());
            if (reify == null) {
                CSet intersection = numCset("Intersection", ir.getEnv(), ir.getKer(), ir.getCard());
                post(_intersection(operands, intersection));
                return intersection;
            }
            return _intersection(operands, reify);
        }

        @Override
        public Object visit(IrSetUnion ir, CSet reify) {
            CSet[] operands = compile(ir.getOperands());
            if (reify == null) {
                CSet union = numCset("Union", ir.getEnv(), ir.getKer(), ir.getCard());
                post(_union(operands, union));
                return union;
            }
            return _union(operands, reify);
        }

        @Override
        public Object visit(IrOffset ir, CSet reify) {
            CSet set = compile(ir.getSet());
            if (reify == null) {
                SetVar offset = numSetVar("Offset", ir.getEnv(), ir.getKer());
                post(_offset(set.getSet(), offset, ir.getOffset()));
                return new CSet(offset, set.getCard());
            }
            return _offset(set.getSet(), reify.getSet(), ir.getOffset());
        }

        @Override
        public Object visit(IrMask ir, CSet reify) {
            CSet set = compile(ir.getSet());
            if (reify == null) {
                CSet mask = numCset("Mask", ir.getEnv(), ir.getKer(), ir.getCard());
                post(_mask(set, mask, ir.getFrom(), ir.getTo()));
                return mask;
            }
            return _mask(set, reify, ir.getFrom(), ir.getTo());
        }

        @Override
        public Object visit(IrSetTernary ir, CSet reify) {
            BoolVar antecedent = compileAsBoolVar(ir.getAntecedent());
            CSet consequent = compile(ir.getConsequent());
            CSet alternative = compile(ir.getAlternative());
            if (reify == null) {
                CSet ternary = numCset("Ternary", ir.getEnv(), ir.getKer(), ir.getCard());
                post(_ifThenElse(antecedent,
                        _equal(ternary, consequent),
                        _equal(ternary, alternative)));
                return ternary;
            }
            return _ifThenElse(antecedent,
                    _equal(reify, consequent),
                    _equal(reify, alternative));
        }
    };

    private static Constraint _implies(BoolVar antecedent, Constraint consequent) {
        return _implies(antecedent, consequent.reif());
    }

    private static Constraint _ifThenElse(BoolVar antecedent, BoolVar consequent, BoolVar alternative) {
        return Constraints.ifThenElse(antecedent, consequent, alternative);
    }

    private static Constraint _ifThenElse(BoolVar antecedent, Constraint consequent, Constraint alternative) {
        Constraint thenClause = _implies(antecedent, consequent);
        Constraint elseClause = _implies(antecedent.not(), alternative);
        return _and(thenClause.reif(), elseClause.reif());
    }

    private static Constraint _sum(IntVar sum, IntVar... vars) {
        return ICF.sum(vars, sum);
    }

    private static Constraint _sum(IntVar sum, BoolVar... vars) {
        return ICF.sum(vars, sum);
    }

    private static Constraint _scalar(IntVar sum, int[] coefficients, IntVar[] vars) {
        boolean allOnes = true;
        for (int coefficient : coefficients) {
            if (coefficient != 1) {
                allOnes = false;
                break;
            }
        }
        if (allOnes) {
            return _sum(sum, vars);
        }
        return ICF.scalar(vars, coefficients, sum);
    }

    private static Constraint _times(IntVar multiplicand, IntVar multiplier, IntVar product) {
        return ICF.times(multiplicand, multiplier, product);
    }

    private static Constraint _div(IntVar dividend, IntVar divisor, IntVar quotient) {
        return ICF.eucl_div(dividend, divisor, quotient);
    }

    private static Constraint _arithm(IntVar var1, String op1, IntVar var2, String op2, int cste) {
        if (cste == 0) {
            switch (Operator.get(op2)) {
                case PL:
                    return ICF.arithm(var1, op1, var2);
                case MN:
                    return ICF.arithm(var1, op1, var2);
            }
        }
        return ICF.arithm(var1, op1, var2, op2, cste);
    }

    private static Constraint _and(BoolVar... vars) {
        switch (vars.length) {
            case 1:
                return _arithm(vars[0], "=", 1);
            case 2:
                return _arithm(vars[0], "+", vars[1], "=", 2);
            default:
                return Constraints.and(vars);
        }
    }

    private static Constraint _lone(BoolVar... vars) {
        switch (vars.length) {
            case 1:
                return vars[0].getSolver().TRUE;
            case 2:
                return _arithm(vars[0], "+", vars[1], "<=", 1);
            default:
                return Constraints.lone(vars);
        }
    }

    private static Constraint _one(BoolVar... vars) {
        switch (vars.length) {
            case 1:
                return _arithm(vars[0], "=", 1);
            case 2:
                return _arithm(vars[0], "+", vars[1], "=", 1);
            default:
                return Constraints.one(vars);
        }
    }

    private static Constraint _or(BoolVar... vars) {
        switch (vars.length) {
            case 1:
                return _arithm(vars[0], "=", 1);
            case 2:
                return _arithm(vars[0], "+", vars[1], ">=", 1);
            default:
                return Constraints.or(vars);
        }
    }

    private static Constraint _implies(BoolVar antecedent, IntVar consequent) {
        return _arithm(antecedent, "<=", consequent);
    }

    private static Constraint _not_implies(BoolVar antecedent, IntVar consequent) {
        return _arithm(antecedent, ">", consequent);
    }

    private static Constraint _arithm(IntVar var1, String op, IntVar var2) {
        if (var2.instantiated()) {
            return ICF.arithm(var1, op, var2.getValue());
        }
        return ICF.arithm(var1, op, var2);
    }

    private static Constraint _arithm(IntVar var1, String op, int c) {
        return ICF.arithm(var1, op, c);
    }

    private BoolVar _reify_equal(IntVar var1, IntVar var2) {
        if (var1.instantiated()) {
            BoolVar reify = numBoolVar("ReifyEqual");
            solver.post(Constraints.reifyEqual(reify, var2, var1.getValue()));
            return reify;
        }
        if (var2.instantiated()) {
            BoolVar reify = numBoolVar("ReifyEqual");
            solver.post(Constraints.reifyEqual(reify, var1, var2.getValue()));
            return reify;
        }
        return _arithm(var1, "=", var2).reif();
    }

    private static Constraint _element(IntVar index, IntVar[] array, IntVar value) {
        return ICF.element(value, array, index, 0);
    }

    private static Constraint _count(int value, IntVar[] array, IntVar count) {
        return ICF.count(value, array, count);
    }

    private static Constraint _equal(CSet var1, CSet var2) {
        return Constraints.equal(var1.getSet(), var1.getCard(), var2.getSet(), var2.getCard());
    }

    private static Constraint _not_equal(CSet var1, CSet var2) {
        if (var1.getSet().instantiated()) {
            return Constraints.notEqual(var2.getSet(), var1.getSet().getValue());
        }
        if (var2.getSet().instantiated()) {
            return Constraints.notEqual(var1.getSet(), var2.getSet().getValue());
        }
        return Constraints.notEqual(var1.getSet(), var2.getSet());
    }

    private static Constraint _all_different(IntVar... vars) {
        return ICF.alldifferent(vars, "AC");
    }

    private static Constraint _within(IntVar var, int low, int high) {
        return ICF.member(var, low, high);
    }

    private static Constraint _within(IntVar var, int[] values) {
        return ICF.member(var, values);
    }

    private static Constraint _not_within(IntVar var, int low, int high) {
        return ICF.not_member(var, low, high);
    }

    private static Constraint _not_within(IntVar var, int[] values) {
        return ICF.not_member(var, values);
    }

    private static Constraint _member(IntVar element, SetVar set) {
        return SCF.member(element, set);
    }

    private static Constraint _not_member(IntVar element, SetVar set) {
        return Constraints.notMember(element, set);
    }

    private static Constraint _lex_chain_less(IntVar[]... vars) {
        if (vars.length == 2) {
            return ICF.lex_less(vars[0], vars[1]);
        }
        return ICF.lex_chain_less(vars);
    }

    private static Constraint _lex_chain_less_eq(IntVar[]... vars) {
        if (vars.length == 2) {
            return ICF.lex_less_eq(vars[0], vars[1]);
        }
        return ICF.lex_chain_less_eq(vars);
    }

    private static Constraint _lex_chain_channel(IntVar[][] strings, IntVar[] ints) {
        return Constraints.lexChainChannel(strings, ints);
    }

    private static Constraint _filter_string(SetVar set, int offset, IntVar[] string, IntVar[] result) {
        return Constraints.filterString(set, offset, string, result);
    }

    private static Constraint _difference(CSet minuend, CSet subtrahend, CSet difference) {
        return Constraints.difference(
                minuend.getSet(), minuend.getCard(),
                subtrahend.getSet(), subtrahend.getCard(),
                difference.getSet(), difference.getCard());
    }

    private static Constraint _intersection(CSet[] operands, CSet intersection) {
        return Constraints.intersection(mapSet(operands), mapCard(operands), intersection.getSet(), intersection.getCard());
    }

    private static Constraint _union(CSet[] operands, CSet union) {
        return Constraints.union(mapSet(operands), mapCard(operands), union.getSet(), union.getCard());
    }

    private IntVar _offset(IntVar var, int offset) {
        Pair<IntVar, Integer> pair = new Pair<IntVar, Integer>(var, offset);
        IntVar cache = cachedOffset.get(pair);
        if (cache == null) {
            cache = VF.offset(var, offset);
            cachedOffset.put(pair, cache);
        }
        return cache;
    }

    private static Constraint _offset(SetVar set, SetVar offseted, int offset) {
        return SCF.offSet(set, offseted, offset);
    }

    private static Constraint _mask(CSet set, CSet masked, int from, int to) {
        return Constraints.mask(set.getSet(), set.getCard(), masked.getSet(), masked.getCard(), from, to);
    }

    private static Constraint _subset_eq(CSet sub, CSet sup) {
        return Constraints.subsetEq(sub.getSet(), sub.getCard(), sup.getSet(), sup.getCard());
    }
    private static final BoolArg ConstraintNoReify = new BoolArg(null, Preference.Constraint);
    private static final BoolArg BoolVarNoReify = new BoolArg(null, Preference.BoolVar);

    private static class BoolArg {

        // The solution needs to be reified in this variable.
        // Set to null if no reification needed.
        private BoolVar reify;
        // The prefered type of solution.
        private final Preference preference;

        BoolArg(BoolVar reify, Preference preference) {
            this.reify = reify;
            this.preference = preference;
        }

        boolean hasReify() {
            return reify != null;
        }

        BoolVar useReify() {
            BoolVar tmp = reify;
            reify = null;
            return tmp;
        }

        Preference getPreference() {
            return preference;
        }
    }

    private static enum Preference {

        Constraint,
        BoolVar;
    }

    private class CSet {

        private final SetVar set;
        private final IrDomain cardDomain;
        private IntVar card;

        CSet(SetVar set, IrDomain cardDomain) {
            this.set = Check.notNull(set);
            this.cardDomain = Check.notNull(cardDomain);
            this.card =
                    cardDomain.getLowBound() > set.getKernelSize() || cardDomain.getHighBound() < set.getEnvelopeSize()
                    ? setCardVar(set, cardDomain)
                    : null;
        }

        CSet(SetVar set, IntVar card) {
            this.set = Check.notNull(set);
            this.cardDomain = null;
            this.card = Check.notNull(card);
        }

        SetVar getSet() {
            return set;
        }

        boolean hasCardCached() {
            return card != null;
        }

        IntVar getCard() {
            if (card == null) {
                assert cardDomain != null;
                card = setCardVar(set, cardDomain);
            }
            return card;
        }
    }

    private static SetVar[] mapSet(CSet[] sets) {
        SetVar[] vars = new SetVar[sets.length];
        for (int i = 0; i < sets.length; i++) {
            vars[i] = sets[i].getSet();
        }
        return vars;
    }

    private static IntVar[] mapCard(CSet[] sets) {
        IntVar[] vars = new IntVar[sets.length];
        for (int i = 0; i < sets.length; i++) {
            vars[i] = sets[i].getCard();
        }
        return vars;
    }
}
