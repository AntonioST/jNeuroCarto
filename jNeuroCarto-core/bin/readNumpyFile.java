import io.ast.jneurocarto.core.numpy.Numpy;

void main(String[] args) throws IOException {
    var array = Numpy.read(Path.of(args[0]), Numpy.ofString());
    IO.println(Arrays.toString(array));
    if (args.length > 1) {
        Numpy.write(Path.of(args[1]), array);
    }
}