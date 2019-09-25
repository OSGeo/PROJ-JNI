# PROJ-JNI
Java Native Interface for [PROJ](https://proj.org/) C/C++ library.
PROJ is a generic coordinate transformation software that transforms
geospatial coordinates from one coordinate reference system (CRS) to another.
This includes cartographic projections as well as geodetic transformations.
This package exposes PROJ services as implementations of [GeoAPI](http://www.geoapi.org/) interfaces.
Both PROJ and GeoAPI are modeled according the ISO 19111 international standard.

## Prerequites:

  * For building:
    - Java Developer Kit (JDK) version 11 or later.
    - PROJ 6. On Linux platforms, it is provided by the `proj` package.
    - PROJ 6 header files. On Linux platforms, it is provided by the `proj-devel` package.
    - `g++` compiler and `make` building tools.
    - Apache Maven.
  * For running:
    - Java Runtime Environment (JRE) version 11 or later.
    - PROJ 6. On Linux platforms, it is provided by the `proj` package.


## Build instruction
If PROJ 6 is installed in the standard directories, the following should work:

```
cd src/main/cpp
make
cd -
mvn install
```

If PROJ 6 is not available in the standard directories but instead has been downloaded
and built in another directory, then the PROJ 6 location needs to be specified first.
On Linux platform:

```
export PROJ_DIR=/path/to/PROJ/directory
export PROJ_LIB=$PROJ_DIR/data
```

Then, follow the same instructions than the ones for standard directories.
