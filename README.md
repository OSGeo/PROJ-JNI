<img src="PROJ-JNI.min.svg" alt="PROJ-JNI" width="30%">

# PROJ-JNI
Java Native Interface for [PROJ](https://proj.org/) C/C++ library.
PROJ is a generic coordinate transformation software that transforms
geospatial coordinates from one coordinate reference system (CRS) to another.
This includes cartographic projections as well as geodetic transformations.
This package exposes PROJ services as implementations of [GeoAPI](https://www.geoapi.org/) interfaces.
Both PROJ and GeoAPI are modeled according the ISO 19111 international standard.
Latest release is [1.0](https://github.com/OSGeo/PROJ-JNI/releases/1.0).

## Developer documentation:

  * [Installation guide](https://github.com/OSGeo/PROJ-JNI/wiki/Install)
  * [PROJ binding Javadoc](https://osgeo.github.io/PROJ-JNI/index.html)
  * [GeoAPI 3.0.1 Javadoc](https://www.geoapi.org/3.0/javadoc/index.html)
  * [Example](./example)

## Prerequisites:

  * For building:
    - Java Developer Kit (JDK) version 11 or later.
    - PROJ 6 with its header files.
    - **g++** compiler and **make** building tools.
    - Apache Maven.
  * For running:
    - Java Runtime Environment (JRE) version 11 or later.
    - PROJ 6.

## Contributing

See
[CONTRIBUTING.md](https://github.com/OSGeo/PROJ-JNI/blob/master/CONTRIBUTING.md).

## Code of conduct

Please note that this project is released with a [Contributor Code of
Conduct](https://github.com/OSGeo/PROJ-JNI/blob/master/CODE_OF_CONDUCT.md).
By participating in this project you agree to abide by its terms.
