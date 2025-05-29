jNeuroCarto: A Neuropixels Channelmap Editor for Java
=====================================================

It is a java-forked from original repository [NeuroCarto](https://github.com/AntonioST/NeuroCarto).
It is a side project for trying and testing new features in Java and some frameworks.

NeuroCarto is a neural probe channel map editor for the Neuropixels probe family.
It allows user to create a blueprint for arranging electrodes in a desired density
and generate a custom channel map.

Features
--------

- Read/Visualize/Modify/Write Neuropixels channelmap files (`*.imro`).
- Read SpikeGLX meta file (`*.meta`).
- Read/Visualize/Modify/Write Blueprint (a blueprint for generating a channelmap by a programming way).
- Show Atlas mouse brain as a background image.
- Customize electrode selection and probe kind.
- Show channel efficiency and electrode density.

### Difference from NeuroCarto

- support more configuration.
- better image transformation (translation and rotation).
- use Java standard tool (`ServiceLoader`) to load external components.

#### Missed features but planned in future

- same ability of blueprint functions.
- probe implant coordinate transformation.

#### Highlighted

The following features are supported by python packages, but implemented in Java:

- read/write [numpy](https://github.com/numpy/numpy) (`.npy`) file (only int and float 1~3-dimension array).
- manage/download [brainglobe](https://github.com/brainglobe/brainglobe-atlasapi) files.
- interactive chart for supporting [bokeh](https://github.com/bokeh/bokeh)-like features.
- chart graphics for supporting [matplotlib](https://github.com/matplotlib/matplotlib)-like features

Relevant Papers
---------------

Su, TS., Kloosterman, F. NeuroCarto: A Toolkit for Building Custom Read-out Channel Maps for
High Electrode-count Neural Probes. *Neuroinform* **23**, 1–16 (2025).
https://doi.org/10.1007/s12021-024-09705-2

Documents
---------

### General conception

Please check original [Documentation](https://neurocarto.readthedocs.io/en/latest/) for more details.

### Programming side

TODO

Modules
-------

* `jNeuroCarto-core` core module
* `jNeuroCarto-probe-npx` Neuropixels extension
* `jNeuroCarto-javafx` standard alone application, use JavaFX.
* `jNeuroCarto-probe-npx-javafx` Neuropixels plugins for application
* `jNeuroCarto-atlas` Atlas brain supporting.
* `jNeuroCarto-kotlin` kotlin support and demonstration of blueprint script

### Working in progress modules

* `jNeuroCarto-web` web application, use Vaadin.
* `jNeuroCarto-web-chart` web component.

### Other modules

* `jNeuroCarto-probe-npx-jmh` [JMH](https://github.com/openjdk/jmh) benchmark testing.

Install and Run
---------------

### Prepare environment.

Require `Java 24`.

### Install

```shell
mvn install
```

### Run

TODO

Build from source
-----------------

### Dependency overview

Core dependencies

- `org.jspecify` null annotations
- `org.slf4j` logging framework
- `info.picocli` command line interface
- `com.fasterxml.jackson.core` json read/write
- `org.junit.jupiter` testing framework
- `org.openjfx` GUI framework
- `io.github.classgraph` classpath/module scanner

Other dependencies

- `com.twelvemonkeys.imageio:imageio-tiff` tiff file
- `org.apache.commons:commons-csv` csv file
- `org.jetbrains.kotlin:kotlin-stdlib` kotlin support

Web related

- `org.springframework.boot`
- `com.vaadin`
- (npm) `plotly.js`

### Build

```
mvn package
```


