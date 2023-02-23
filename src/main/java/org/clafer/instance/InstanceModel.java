package org.clafer.instance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.clafer.ast.AstClafer;
import org.clafer.ast.AstConcreteClafer;
import org.clafer.common.Check;
import org.sysml.*;
import org.sysml.pprinter.SysmlPrinter;

/**
 *
 * @author jimmy
 */
public class InstanceModel {

    private final InstanceClafer[] topClafers;

    public InstanceModel(InstanceClafer... topClafers) {
        this.topClafers = Check.noNulls(topClafers);
    }

    public InstanceClafer[] getTopClafers() {
        return topClafers;
    }

    public InstanceClafer[] getTopClafers(AstConcreteClafer type) {
        List<InstanceClafer> typedTopClafers = new ArrayList<>();
        for (InstanceClafer topClafer : topClafers) {
            if (type.equals(topClafer.getType())) {
                typedTopClafers.add(topClafer);
            }
        }
        return typedTopClafers.toArray(new InstanceClafer[typedTopClafers.size()]);
    }

    public InstanceClafer getTopClafer(AstConcreteClafer type) {
        InstanceClafer typedTopClafer = null;
        for (InstanceClafer topClafer : topClafers) {
            if (type.equals(topClafer.getType())) {
                if (typedTopClafer != null) {
                    throw new IllegalArgumentException("More than one top Clafer with type " + type);
                }
                typedTopClafer = topClafer;
            }
        }
        if (typedTopClafer == null) {
            throw new IllegalArgumentException("No top Clafer with type " + type);
        }
        return typedTopClafer;
    }

    private SysmlProperty compileInstance(InstanceClafer model) {
        String propertyName = model.getType().getName();
        ArrayList<SysmlBlockDefElement> children = new ArrayList<SysmlBlockDefElement>();
        for (InstanceClafer child : model.getChildren()) {
            children.add(compileInstance(child));
        }
        return new SysmlProperty(new SysmlBlockVisibility(SysmlVisibilityOption.PLUS), new SysmlPropertyType("part"), propertyName, children.toArray(new SysmlBlockDefElement[children.size()]));
    }

    /**
     * Print solution as SysMLv2
     *
     * This is the start of exploring an automated product derivation tool
     * and the requirements around inferring a system model from clafer are
     * still tbd
     * @param out
     * @throws IOException
     */
    public void printSysml(Appendable out) throws IOException {
        for (InstanceClafer top : topClafers) {
            SysmlPrinter pprinter = new SysmlPrinter(out);
            SysmlProperty model = compileInstance(top);
            pprinter.visit(model, "");
        }
    }

    /**
     * Print solution to stdout.
     *
     * @throws IOException an IO error occurred
     */
    public void print() throws IOException {
        print(System.out);
    }

    /**
     * Print solution.
     *
     * @param out the stream to print to
     * @throws IOException an IO error occurred
     */
    public void print(Appendable out) throws IOException {
        for (InstanceClafer top : topClafers) {
            top.print(out);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof InstanceModel) {
            InstanceModel other = (InstanceModel) obj;
            return Arrays.equals(topClafers, other.topClafers);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(topClafers);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        try {
            print(result);
        } catch (IOException e) {
            // StringBuilder should not throw an IOException.
            throw new Error(e);
        }
        return result.toString();
    }
}
