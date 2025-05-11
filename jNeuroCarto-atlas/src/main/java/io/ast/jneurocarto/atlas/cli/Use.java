package io.ast.jneurocarto.atlas.cli;

import java.io.IOException;
import java.util.Optional;

import io.ast.jneurocarto.atlas.BrainAtlas;
import io.ast.jneurocarto.atlas.Structure;
import picocli.CommandLine;

@CommandLine.Command(
  name = "use",
  usageHelpAutoWidth = true,
  description = "use atlas brain",
  subcommands = {
    Slice.class
  }
)
public class Use implements Runnable {

    @CommandLine.Option(names = {"-h", "-?", "--help"}, usageHelp = true)
    public boolean help;

    @CommandLine.Mixin
    public Main.ConfigOptions config;

    @CommandLine.Mixin
    public Main.UseAtlas useAtlas;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }

    private BrainAtlas getAtlas() throws IOException {
        var downloader = useAtlas.newDownloader(config);
        return useAtlas.download(downloader);
    }

    @CommandLine.Command(name = "meta", aliases = {"info"}, description = "print atlas meta information")
    public void printMetaInfo() throws IOException {
        var atlas = getAtlas();
        System.out.println(atlas.root());
        System.out.println(atlas.meta());
    }

    @CommandLine.Command(name = "files", description = "print related files")
    public void printFiles() throws IOException {
        var atlas = getAtlas();
        System.out.printf("%-16s : %s\n", "root", atlas.root());
        System.out.printf("%-16s : %s\n", "metadata", atlas.root().resolve(BrainAtlas.METADATA_FILENAME));
        System.out.printf("%-16s : %s\n", "structures", atlas.root().resolve(BrainAtlas.STRUCTURES_FILENAME));
        System.out.printf("%-16s : %s\n", "reference", atlas.root().resolve(BrainAtlas.REFERENCE_FILENAME));
        System.out.printf("%-16s : %s\n", "annotation", atlas.root().resolve(BrainAtlas.ANNOTATION_FILENAME));
        System.out.printf("%-16s : %s\n", "hemispheres", atlas.root().resolve(BrainAtlas.HEMISPHERES_FILENAME));
    }

    @CommandLine.Command(name = "structure", aliases = {"s"}, description = "print structure")
    public void searchStructure(
      @CommandLine.Option(names = "--id", description = "take ACRONYM as id") boolean asid,
      @CommandLine.Parameters(paramLabel = "ACRONYM", description = "structure acronym") String acronym
    ) throws IOException {
        var atlas = getAtlas();
        var structures = atlas.hierarchy();

        Optional<Structure> structure;
        if (asid) {
            structure = structures.get(Integer.parseInt(acronym));
        } else {
            structure = structures.get(acronym);
        }

        if (structure.isPresent()) {
            System.out.println(structure.get());
        } else {
            System.out.println("no such structure");
        }
    }

    @CommandLine.Command(name = "structure.parents", aliases = {"s.parents"}, description = "print structure parents")
    public void printStructureParents(
      @CommandLine.Option(names = "--id", description = "take ACRONYM as id") boolean asid,
      @CommandLine.Parameters(paramLabel = "ACRONYM", description = "structure acronym") String acronym
    ) throws IOException {
        var atlas = getAtlas();
        var structures = atlas.hierarchy();

        Optional<Structure> structure;
        if (asid) {
            structure = structures.get(Integer.parseInt(acronym));
        } else {
            structure = structures.get(acronym);
        }

        if (structure.isEmpty()) {
            System.out.println("no such structure");
        } else {
            structure.ifPresent(Use::printSimpleStructureLine);
            structures.parents(structure.get()).forEach(Use::printSimpleStructureLine);
        }
    }

    @CommandLine.Command(name = "structure.children", aliases = {"s.children"}, description = "print structure children")
    public void printStructureChildren(
      @CommandLine.Option(names = "--id", description = "take ACRONYM as id") boolean asid,
      @CommandLine.Option(names = "--all", description = "print all") boolean all,
      @CommandLine.Parameters(paramLabel = "ACRONYM", description = "structure acronym") String acronym
    ) throws IOException {
        var atlas = getAtlas();
        var structures = atlas.hierarchy();

        Optional<Structure> structure;
        if (asid) {
            structure = structures.get(Integer.parseInt(acronym));
        } else {
            structure = structures.get(acronym);
        }

        if (structure.isEmpty()) {
            System.out.println("no such structure");
        } else if (!all) {
            structures.children(structure.get()).forEach(Use::printSimpleStructureLine);
        } else {
            structure.ifPresent(st -> {
                System.out.println("* (" + st.id() + ") " + st.acronym() + " - " + st.name());
                int top = st.structurePath().length;
                structures.forAllChildren(st, s -> {
                    int level = s.structurePath().length;
                    System.out.println("  ".repeat(level - top) + "* (" + s.id() + ") " + s.acronym() + " - " + s.name());
                });
            });
        }
    }

    private static void printSimpleStructureLine(Structure s) {
        System.out.printf("%-8d : %-16s - %s\n", s.id(), s.acronym(), s.name());
    }

}
