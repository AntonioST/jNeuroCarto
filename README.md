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

- TODO

The following features are supported by python packages, but implemented in Java:

- read/write [numpy](https://github.com/numpy/numpy) (`.npy`) file.
- manage [brainglobe](https://github.com/brainglobe/brainglobe-atlasapi) files.
- [bokeh](https://github.com/bokeh/bokeh) interactive chart.

Relevant Papers
---------------

Su, TS., Kloosterman, F. NeuroCarto: A Toolkit for Building Custom Read-out Channel Maps for
High Electrode-count Neural Probes. *Neuroinform* **23**, 1–16 (2025).
https://doi.org/10.1007/s12021-024-09705-2

Documents
---------

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

TODO


