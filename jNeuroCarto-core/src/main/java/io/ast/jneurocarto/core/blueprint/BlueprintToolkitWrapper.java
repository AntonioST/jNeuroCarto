package io.ast.jneurocarto.core.blueprint;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.core.ElectrodeDescription;

@NullMarked
public class BlueprintToolkitWrapper<T> extends BlueprintToolkit<T> {

    protected final BlueprintToolkit<T> toolkit;

    public BlueprintToolkitWrapper(BlueprintToolkit<T> toolkit) {
        super(toolkit.blueprint);
        this.toolkit = toolkit;
    }

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public BlueprintToolkitWrapper<T> clone() {
        return new BlueprintToolkitWrapper<>(toolkit.clone());
    }

    @Override
    public void printBlueprint(int[] blueprint, Appendable out) throws IOException {
        toolkit.printBlueprint(blueprint, out);
    }

    @Override
    public int index(int s, int x, int y) {
        return toolkit.index(s, x, y);
    }

    /*@Override
    public int[] index(T chmap) {
        return toolkit.index(chmap);
    }*/

    @Override
    public int[] index(List<ElectrodeDescription> e) {
        return toolkit.index(e);
    }

    @Override
    public int[] index(Predicate<ElectrodeDescription> picker) {
        return toolkit.index(picker);
    }

    /*@Override
    public boolean[] mask(T chmap) {
        return toolkit.mask(chmap);
    }*/

    @Override
    public boolean[] mask(List<ElectrodeDescription> e) {
        return toolkit.mask(e);
    }

    @Override
    public boolean[] mask(Predicate<ElectrodeDescription> e) {
        return toolkit.mask(e);
    }

    /*@Override
    public boolean[] mask(int[] index) {
        return toolkit.mask(index);
    }*/

    @Override
    public int[] set(int[] a, int[] i, int v) {
        return toolkit.set(a, i, v);
    }

    @Override
    public int[] set(int[] a, int[] i, int[] v) {
        return toolkit.set(a, i, v);
    }

    @Override
    public int[] set(int[] a, int[] i, int offset, int length, int v) {
        return toolkit.set(a, i, offset, length, v);
    }

    @Override
    public int[] setIfUnset(int[] a, int[] i, int offset, int length, int v) {
        return toolkit.setIfUnset(a, i, offset, length, v);
    }

    @Override
    public int[] move(int[] output, int[] blueprint, int step, IntBinaryOperator tester) {
        return toolkit.move(output, blueprint, step, tester);
    }

    @Override
    public int[] moveIndex(int[] index, int step) {
        return toolkit.moveIndex(index, step);
    }

    @Override
    public int moveIndex(int[] output, int[] index, int step) {
        return toolkit.moveIndex(output, index, step);
    }

    @Override
    public int[] surrounding(int electrode, boolean diagonal) {
        return toolkit.surrounding(electrode, diagonal);
    }

    @Override
    public Clustering findClustering(int[] blueprint, boolean diagonal) {
        return toolkit.findClustering(blueprint, diagonal);
    }

    @Override
    public Clustering findClustering(int[] blueprint, int category, boolean diagonal) {
        return toolkit.findClustering(blueprint, category, diagonal);
    }

    @Override
    public Clustering findClustering(int[] blueprint, boolean diagonal, IntBinaryOperator tester) {
        return toolkit.findClustering(blueprint, diagonal, tester);
    }

    @Override
    public List<ClusteringEdges> getClusteringEdges(Clustering clustering) {
        return toolkit.getClusteringEdges(clustering);
    }

    @Override
    public int[] fillClusteringEdges(int[] blueprint, ClusteringEdges edge) {
        return toolkit.fillClusteringEdges(blueprint, edge);
    }

    @Override
    public int[] fill(int[] blueprint, Clustering clustering, AreaThreshold threshold) {
        return toolkit.fill(blueprint, clustering, threshold);
    }

    @Override
    protected int[] extend(int[] output, int[] blueprint, int category, int step, int value, AreaThreshold threshold) {
        return toolkit.extend(output, blueprint, category, step, value, threshold);
    }

    @Override
    protected int[] reduce(int[] output, int[] blueprint, int category, int step, int value, AreaThreshold threshold) {
        return toolkit.reduce(output, blueprint, category, step, value, threshold);
    }

    @Override
    public int[] loadBlueprint(Path file) throws IOException {
        return toolkit.loadBlueprint(file);
    }

    @Override
    public void saveNumpyBlueprint(Path file, int[] blueprint) throws IOException {
        toolkit.saveNumpyBlueprint(file, blueprint);
    }

    @Override
    public int[] loadNumpyBlueprint(Path file) throws IOException {
        return toolkit.loadNumpyBlueprint(file);
    }

    @Override
    public int[] loadCsvBlueprint(Path file) throws IOException {
        return toolkit.loadCsvBlueprint(file);
    }

    @Override
    public void saveCsvBlueprint(Path file, int[] blueprint) throws IOException {
        toolkit.saveCsvBlueprint(file, blueprint);
    }

    @Override
    public double[] get(double[] data, int[] index) {
        return toolkit.get(data, index);
    }

    /*@Override
    public double[] get(double[] data, T chmap) {
        return toolkit.get(data, chmap);
    }*/

    @Override
    public double[] get(double[] data, List<ElectrodeDescription> e) {
        return toolkit.get(data, e);
    }

    @Override
    public double[] set(double[] a, int[] i, double v) {
        return toolkit.set(a, i, v);
    }

    /*@Override
    public double[] set(double[] a, T chmap, double v) {
        return toolkit.set(a, chmap, v);
    }*/

    @Override
    public double[] set(double[] a, List<ElectrodeDescription> e, double v) {
        return toolkit.set(a, e, v);
    }

    @Override
    public double[] set(double[] a, int[] i, double[] v) {
        return toolkit.set(a, i, v);
    }

    @Override
    public double[] set(double[] a, int[] i, int offset, int length, double v) {
        return toolkit.set(a, i, offset, length, v);
    }

    @Override
    public double[] set(double[] a, int[] i, int offset, int length, DoubleUnaryOperator oper) {
        return toolkit.set(a, i, offset, length, oper);
    }

    /*@Override
    public double[] interpolateNaN(double[] a, int k, InterpolateNaNBuiltinMethod f) {
        return toolkit.interpolateNaN(a, k, f);
    }*/

    @Override
    public double[] interpolateNaN(double[] o, double[] a, double[] k, ToDoubleFunction<double[]> f) {
        return toolkit.interpolateNaN(o, a, k, f);
    }

    /*@Override
    public double[] interpolateNaN(double[] a, int kx, int ky, InterpolateNaNBuiltinMethod f) {
        return toolkit.interpolateNaN(a, kx, ky, f);
    }*/

    @Override
    public double[] interpolateNaN(double[] o, double[] a, double[][] k, ToDoubleFunction<double[][]> f) {
        return toolkit.interpolateNaN(o, a, k, f);
    }
}
