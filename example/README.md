For running this demo, enter the following command from the project root
directory. Note that there is no need to run `javac`; this will be done
on-the-fly by the `java` command.

``` sh
mvn install
java --class-path target/proj-2.0-SNAPSHOT.jar example/TransformPoints.java
```

The expected output for the example is

``` txt
No EPSG code given. Default to 3395 (WGS 84 / World Mercator).
The coordinate operation is: World Mercator
Its domain of validity is:
    West bound longitude: -180.0
    East bound longitude:  180.0
    South bound latitude:  -80.0
    North bound latitude:   84.0

Montreal:   -8189441.0   5670094.0
Vancouver: -13703429.3   6285000.3
Tokyo:      15566806.3   4228072.9
Paris:        261489.5   6219786.6
```
