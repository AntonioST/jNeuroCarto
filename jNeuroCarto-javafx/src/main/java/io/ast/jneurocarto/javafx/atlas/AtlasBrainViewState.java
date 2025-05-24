package io.ast.jneurocarto.javafx.atlas;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

import io.ast.jneurocarto.atlas.ImageSliceStack;

public class AtlasBrainViewState {
    @JsonAlias("atlas_brain")
    public String name;

    @JsonAlias("brain_slice")
    public ImageSliceStack.Projection projection = ImageSliceStack.Projection.coronal;

    @JsonAlias("slice_plane")
    public int plane;

    @JsonAlias("slice_rot_w")
    public int offsetWidth;

    @JsonAlias("slice_rot_h")
    public int offsetHeight;

    @JsonAlias("image_dx")
    public double imagePosX;

    @JsonAlias("image_dy")
    public double imagePosY;

    @JsonAlias("image_sx")
    public double imageScaleX;

    @JsonAlias("image_sy")
    public double imageScaleY;

    @JsonAlias("image_rt")
    public double imageRoration;

    public List<String> regions = new ArrayList<>();

    public List<CoordinateLabel> labels = new ArrayList<>();
}
