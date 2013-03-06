package org.clafer.constraint;

import choco.Choco;
import choco.cp.model.CPModel;
import choco.cp.model.managers.MixedConstraintManager;
import choco.cp.solver.CPSolver;
import choco.cp.solver.search.integer.branching.AssignVar;
import choco.cp.solver.search.integer.valiterator.IncreasingDomain;
import choco.cp.solver.search.integer.varselector.MinDomain;
import choco.cp.solver.search.set.AssignSetVar;
import choco.cp.solver.search.set.MinDomSet;
import choco.cp.solver.search.set.MinEnv;
import choco.kernel.model.Model;
import choco.kernel.model.ModelException;
import choco.kernel.model.constraints.ComponentConstraint;
import choco.kernel.model.constraints.Constraint;
import choco.kernel.model.variables.Variable;
import choco.kernel.model.variables.integer.IntegerVariable;
import choco.kernel.model.variables.set.SetVariable;
import choco.kernel.solver.Solver;
import choco.kernel.solver.constraints.SConstraint;
import java.util.List;
import org.clafer.Util;

/**
 *
 * @author jimmy
 */
public class UpcastManager extends MixedConstraintManager {

    @Override
    public SConstraint makeConstraint(Solver solver, Variable[] vars, Object params, List<String> options) {
        if (solver instanceof CPSolver) {
            return new Upcast(
                    solver.getVar((SetVariable) vars[0]),
                    solver.getVar((IntegerVariable) vars[1]),
                    solver.getVar((SetVariable) vars[2]));
        }
        throw new ModelException("Could not found a constraint manager in " + this.getClass() + " !");
    }

    public static Constraint upcast(SetVariable from, IntegerVariable offset, SetVariable to) {
        return new ComponentConstraint(UpcastManager.class, null, new Variable[]{from, offset, to});
    }

    public static void main(String[] args) {
        Model m = new CPModel();

        SetVariable from = Choco.makeSetVar("from", 0, 10);
        IntegerVariable offset = Choco.makeIntVar("offset", 0, 10);
        SetVariable to = Choco.makeSetVar("to", 0, 10);

        m.addConstraint(upcast(from, offset, to));

        System.out.println(Util.allSolutions(m).getStatistics());

        Solver solver = Util.newSolver(m);
        solver.addGoal(new AssignVar(new MinDomain(solver), new IncreasingDomain()));
        solver.addGoal(new AssignSetVar(new MinDomSet(solver), new MinEnv()));
        System.out.println(Util.allSolutions(solver).getStatistics());
        
        // #4094 solutions 3004 Time (ms), 8178 Nodes, 8177 Backtracks, 0 Restarts - 
        // #4094 solutions 1008 Time (ms), 8123 Nodes, 8122 Backtracks, 0 Restarts - 
    }
}
