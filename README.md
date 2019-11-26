# PROJ-JNI
Java Native Interface for [PROJ](https://proj.org/) C/C++ library.
PROJ is a generic coordinate transformation software that transforms
geospatial coordinates from one coordinate reference system (CRS) to another.
This includes cartographic projections as well as geodetic transformations.
This package exposes PROJ services as implementations of [GeoAPI](http://www.geoapi.org/) interfaces.
Both PROJ and GeoAPI are modeled according the ISO 19111 international standard.

## Developer documentation:

  * [PROJ binding Javadoc](https://kortforsyningen.github.io/PROJ-JNI/index.html)
  * [GeoAPI 3.0.1 Javadoc](http://www.geoapi.org/3.0/javadoc/index.html)

**WARNING:** the `kortforsyningen` namespace in package name, module name, Maven artifact, _etc._
is temporary. The final name will be selected later, tentatively in December 2019.

## Prerequites:

  * For building:
    - Java Developer Kit (JDK) version 11 or later.
    - PROJ 6 with its header files.
    - **g++** compiler and **make** building tools.
    - Apache Maven.
  * For running:
    - Java Runtime Environment (JRE) version 11 or later.
    - PROJ 6.


## Build instruction

Build instructions are given in a
[separated page](https://kortforsyningen.github.io/PROJ-JNI/install.html)


## Example

An example is given in the [example directory](./example).
