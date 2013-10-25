package org.clafer.compiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.clafer.collection.Either;
import org.clafer.common.Check;
import org.clafer.collection.Pair;
import org.clafer.instance.InstanceModel;
import solver.ResolutionPolicy;
import solver.Solver;
import solver.constraints.ICF;
import solver.exception.ContradictionException;
import solver.objective.IntObjectiveManager;
import solver.propagation.NoPropagationEngine;
import solver.propagation.hardcoded.SevenQueuesPropagatorEngine;
import solver.search.loop.monitors.IMonitorSolution;
import solver.search.solution.Solution;
import solver.variables.IntVar;
import solver.variables.SetVar;

/**
 *
 * @author jimmy
 */
public class ClaferOptimizer implements ClaferSearch<Pair<Integer, InstanceModel>> {

    public final Solver solver;
    private final ClaferSolutionMap solutionMap;
    private final Objective objective;
    private final Either<Integer, IntVar> score;
    private boolean first = true;
    private boolean more = true;
    private boolean second = true;
    private final Solution firstSolution = new Solution();

    ClaferOptimizer(Solver solver, ClaferSolutionMap solutionMap,
            Objective objective, Either<Integer, IntVar> score) {
        this.solver = Check.notNull(solver);
        this.solutionMap = Check.notNull(solutionMap);
        this.objective = Check.notNull(objective);
        this.score = Check.notNull(score);
    }

    public ClaferSolutionMap getSolutionMap() {
        return solutionMap;
    }

    public Objective getObjective() {
        return objective;
    }

    @Override
    public boolean find() {
        if (!more) {
            return false;
        }
        if (first) {
            more &= solveFirst();
            first = false;
            return more;
        }
        more &= solveNext();
        return more;
    }

    /*
     * Implementation of multiple optimal search based on discussion here:
     * https://github.com/chocoteam/choco3/issues/121.
     */
    private boolean solveFirst() {
        if (score.isLeft()) {
            return solver.findSolution();
        }
        IntVar scoreVar = score.getRight();
        solver.getSearchLoop().setObjectivemanager(new IntObjectiveManager(
                scoreVar,
                Objective.Minimize.equals(objective) ? ResolutionPolicy.MINIMIZE : ResolutionPolicy.MAXIMIZE,
                solver));
        solver.getSearchLoop().plugSearchMonitor(new IMonitorSolution() {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSolution() {
                if (first) {
                    firstSolution.record(solver);
                }
            }
        });
        if (solver.getEngine() == NoPropagationEngine.SINGLETON) {
            solver.set(new SevenQueuesPropagatorEngine(solver));
        }
        solver.getSearchLoop().getMeasures().setReadingTimeCount(System.nanoTime());
        solver.getSearchLoop().launch(false);
        if (!firstSolution.hasBeenFound()) {
            return false;
        }
        try {
            firstSolution.restore();
        } catch (ContradictionException e) {
            // Should never happen because the solution should not be contradictory.
            throw new IllegalStateException(e);
        }
        return true;
    }

    private boolean solveNext() {
        if (score.isLeft() || !second) {
            return solver.nextSolution();
        }
        second = false;
        IntVar scoreVar = score.getRight();
        int best = scoreVar.getValue();
        // TODO: forbid the current solution from happening again.                                                 
        solver.getEngine().flush();
        solver.getSearchLoop().reset();
        solver.post(ICF.arithm(scoreVar, "=", best));
        boolean next = solver.findSolution();
        return next && duplicateSolution() ? solver.nextSolution() : next;
    }

    private boolean duplicateSolution() {
        for (IntVar var : solutionMap.getIrSolution().getIntVars()) {
            if (var.getValue() != firstSolution.getIntVal(var)) {
                return false;
            }
        }
        for (SetVar var : solutionMap.getIrSolution().getSetVars()) {
            if (!Arrays.equals(var.getValue(), firstSolution.getSetVal(var))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Pair<Integer, InstanceModel> instance() {
        return new Pair<Integer, InstanceModel>(
                score.isLeft() ? score.getLeft() : score.getRight().getValue(),
                solutionMap.getInstance());
    }

    @Override
    public Pair<Integer, InstanceModel>[] allInstances() {
        List<Pair<Integer, InstanceModel>> instances = new ArrayList<Pair<Integer, InstanceModel>>();
        while (find()) {
            instances.add(instance());
        }
        @SuppressWarnings("unchecked")
        Pair<Integer, InstanceModel>[] pairs = new Pair[instances.size()];
        return instances.toArray(pairs);
    }

    @Override
    public Solver getInternalSolver() {
        return solver;
    }

    public static enum Objective {

        Maximize(ResolutionPolicy.MAXIMIZE),
        Minimize(ResolutionPolicy.MINIMIZE);
        private final ResolutionPolicy policy;

        Objective(ResolutionPolicy policy) {
            this.policy = policy;
        }

        ResolutionPolicy getPolicy() {
            return policy;
        }
    }
}
