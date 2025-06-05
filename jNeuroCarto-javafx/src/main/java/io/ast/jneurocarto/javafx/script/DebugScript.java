package io.ast.jneurocarto.javafx.script;

import module io.ast.jneurocarto.atlas;
import module io.ast.jneurocarto.core;
import module io.ast.jneurocarto.javafx;
import module java.base;
import module javafx.graphics;
import module org.jspecify;
import java.nio.file.Path;


@NullMarked
@BlueprintScript("debug")
public class DebugScript {

    @BlueprintScript(value = "echo", description = "echo arguments in log message area.")
    public void echo(BlueprintAppToolkit<Object> toolkit,
                     @ScriptParameter("args") PyValue... args) {
        toolkit.printLogMessage("");
        toolkit.printLogMessage(Arrays.stream(args).map(Objects::toString).toList());
    }

    /**
     * Test async function with sleep. Make sure it won't affect the UI thread.
     */
//    @BlueprintScript(value = "count", description = "count until", async = true)
    public void count(BlueprintAppToolkit<Object> toolkit,
                      @ScriptParameter("n") int n,
                      @ScriptParameter(value = "e", defaultValue = "0",
                          description = "throw error on end") boolean e) throws InterruptedException {
        var i = 0;
        while (i < n) {
            toolkit.printLogMessage("" + i);
            Thread.sleep(1000);
            i++;
        }

        if (e) {
            throw new RuntimeException("" + n);
        }

        toolkit.printLogMessage("" + n);
    }

    /**
     * Test async call UI function via {@link ScriptThread#awaitFxApplicationThread(Runnable)}.
     */
    //    @BlueprintScript(value = "count-echo", description = "count and echo", async = true)
    public void countEcho(BlueprintAppToolkit<Object> toolkit,
                          ScriptPlugin plugin,
                          @ScriptParameter("n") int n) throws InterruptedException {
        var success = ScriptThread.awaitFxApplicationThread(() -> {
            if (plugin.selectScript("echo")) {
                plugin.setScriptInputLine("");
                return true;
            } else {
                return false;
            }
        }).getOrThrowRuntimeException();
        if (!success) return;

        var i = 0;
        while (i < n) {
            var j = i;
            ScriptThread.awaitFxApplicationThread(() -> {
                plugin.appendScriptInputValueText("" + j);
            });
            Thread.sleep(1000);
            i++;
        }

        ScriptThread.awaitFxApplicationThread((Runnable) plugin::runScript);

        ScriptThread.awaitFxApplicationThread(() -> {
            toolkit.printLogMessage("count");
            plugin.showAndRunScript("count", "" + n);
        });
        var error = plugin.waitScriptFinished("count");
        toolkit.printLogMessage(List.of("error?", Objects.toString(error)));
    }

    /**
     * Test {@link io.ast.jneurocarto.javafx.atlas.AtlasPlugin#anchorImageTo(ImageSliceStack.Projection, Coordinate, Point2D)}
     */
//    @BlueprintScript("anchor")
    public void atlasAnchorImageTo(
        BlueprintAppToolkit<Object> toolkit,
        AtlasPlugin atlas,
        @ScriptParameter(value = "coor", label = "(AP,DV,ML)", defaultValue = "(0,0,0)",
            description = "referenced anatomical coordinate (mm)") double[] coordinate,
        @ScriptParameter(value = "onto", label = "(X,Y)", defaultValue = "None",
            description = "move coor onto point (mm)") double @Nullable [] point,
        @ScriptParameter(value = "proj", defaultValue = "None",
            description = "change projection") ImageSliceStack.@Nullable Projection projection,
        @ScriptParameter(value = "global", defaultValue = "False",
            description = "change coor to global anatomical coordinate") boolean global) {
        if (coordinate.length != 3) throw new RuntimeException("not (AP,DV,ML), but " + Arrays.toString(coordinate));
        if (point != null && point.length != 2) throw new RuntimeException("not (XY), but " + Arrays.toString(point));

        var c = new Coordinate(coordinate[0] * 1000, coordinate[1] * 1000, coordinate[2] * 1000);
        if (global) {
            c = atlas.project(c);
        }

        if (point == null) {
            if (projection == null) {
                atlas.anchorImageTo(c);
            } else {
                atlas.anchorImageTo(projection, c);
            }
        } else {
            var p = new Point2D(point[0] * 1000, point[1] * 1000);
            if (projection == null) {
                atlas.anchorImageTo(c, p);
            } else {
                atlas.anchorImageTo(projection, c, p);
            }
        }
    }

    /**
     * test {@link AtlasPlugin#updateMaskedRegion()}
     */
    @BlueprintScript("mask")
    public void atlasMaskRegion(
        BlueprintAppToolkit<Object> toolkit,
        ScriptPlugin script,
        AtlasPlugin atlas,
        @ScriptParameter(value = "name", label = "NAME|clear",
            description = "structure name") String name
    ) throws PluginNotLoadException {
        if ("clear".equals(name)) {
            atlas.clearMaskedRegions();
            script.setScriptInputLine("");
        } else {
            var n = toolkit.atlasGetRegion(name);
            if (n != null) {
                toolkit.printLogMessage(n);
                atlas.addMaskedRegion(n);
            }
        }
    }

    /**
     * test temporary add event handle on canvas
     */
    @BlueprintScript(value = "snapshot", async = true)
    public void snapshot(
        BlueprintAppToolkit<Object> toolkit,
        AtlasPlugin atlas,
        @ScriptParameter(value = "filename", label = "FILE.npy", defaultValue = "snapshot.npy",
            description = "output filename") Path file
    ) throws InterruptedException {
        var selection = ScriptThread.listenOnDataSelectEvent(toolkit);
        var bounds = atlas.painter().getImageTransform().transform(selection.bounds);

        var image = atlas.getImageSlice();
        if (image == null) {
            toolkit.printLogMessage("nothing can be select");
            return;
        }

        var res = image.resolution();
        int x0 = (int) (bounds.getMinX() / res[1]);
        int y0 = (int) (bounds.getMinY() / res[2]);
        int x1 = (int) (bounds.getMaxX() / res[1]);
        int y1 = (int) (bounds.getMaxY() / res[2]);
        int w = x1 - x0;
        int h = y1 - y0;

        // FIXME why left-right-flipped
        var arr = image.image(ImageSlice.INT_IMAGE, x0, y0, w, h, null);

        try {
            Numpy.write(file, arr);
            toolkit.printLogMessage("save to " + file);
        } catch (IOException e) {
            toolkit.printLogMessage("fail save to " + file);
        }
    }
}
