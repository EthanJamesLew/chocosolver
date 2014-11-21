package org.clafer.choco.constraint;

import static org.clafer.choco.constraint.ConstraintQuickTest.$;
import org.clafer.choco.constraint.ConstraintQuickTest.Check;
import org.clafer.choco.constraint.ConstraintQuickTest.Input;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import solver.Solver;
import solver.constraints.Constraint;
import solver.variables.CSetVar;
import solver.variables.IntVar;
import static solver.variables.Var.card;
import static solver.variables.Var.cset;
import static solver.variables.Var.env;
import static solver.variables.Var.ker;
import static solver.variables.VariableFactory.enumerated;

/**
 *
 * @author jimmy
 */
@RunWith(ConstraintQuickTest.class)
public class SetMinTest {

    @Input(solutions = 45)
    public Object testSumSet(Solver solver) {
        /*
         * import Control.Monad
         *
         * powerset = filterM (const [True, False])
         *
         * solutions = do
         *     set <- powerset [-4..3]
         *     guard $ length set <= 2
         *     min <- [-4..4]
         *     guard $ null set || min == head set
         *     return (set, min)
         */
        return $(cset("set", env(-4, -3, -2, -1, 0, 1, 2, 3), ker(), card(0, 1, 2), solver),
                enumerated("min", -4, 4, solver));
    }

    @Check
    public void check(int[] set, int min) {
        if (set.length > 0) {
            assertEquals(set[0], min);
        }
    }

    @Test(timeout = 60000)
    public Constraint setup(CSetVar set, IntVar max) {
        return Constraints.min(set.getSet(), set.getCard(), max);
    }
}
