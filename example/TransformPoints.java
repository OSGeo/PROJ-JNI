/*
 *    Java bindings to PROJ
 *
 *    This file is hereby placed into the Public Domain.
 *    This means anyone is free to do whatever they wish with this file.
 */
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.kortforsyningen.proj.Proj;


/**
 * An example of code transforming a few points from one
 * Coordinate Reference System (CRS) to another one.
 */
public class TransformPoints {
    /**
     * The factory to use for creating Coordinate Reference Systems (CRS) from EPSG codes.
     * For performance reasons, this instance should be fetched only at initialization time
     * and reused as long as necessary.
     *
     * Note: this interface will become {@code RegisterOperations} in a future version.
     */
    private final CRSAuthorityFactory factory;

    /**
     * The factory to use for inferring an operation between two Coordinate Reference Systems.
     * For performance reasons, this instance should be fetched only at initialization time
     * and reused as long as necessary.
     *
     * Note: this interface will become {@code RegisterOperations} in a future version.
     */
    private final CoordinateOperationFactory regops;

    /**
     * Creates a new instance which will use PROJ factories for Coordinate Reference Systems and operations.
     */
    private TransformPoints() {
        factory = Proj.getAuthorityFactory("EPSG");
        regops  = Proj.getOperationFactory(null);
    }

    /**
     * Projects the geographic coordinates of a few cities and prints the results.
     * The target Coordinate Reference System (CRS) is given in argument.
     *
     * @param  target              the EPSG code of target CRS. Example: "3395" for "WGS 84 / World Mercator".
     * @throws FactoryException    if an error occurred while creating a CRS or inferring the coordinate operation.
     * @throws TransformException  if an error occurred while applying the operation on coordinate values.
     */
    private void printCoordinates(String target) throws FactoryException, TransformException {
        CoordinateReferenceSystem sourceCRS = factory.createCoordinateReferenceSystem("4326");   // WGS 84
        CoordinateReferenceSystem targetCRS = factory.createCoordinateReferenceSystem(target);
        CoordinateOperation       operation = regops .createOperation(sourceCRS, targetCRS);
        double[] coordinates = {
            45.500,  -73.567,                    // Montreal
            49.250, -123.100,                    // Vancouver
            35.653,  139.839,                    // Tokyo
            48.865,    2.349                     // Paris
        };
        operation.getMathTransform().transform(
                coordinates, 0,                  // Source coordinates.
                coordinates, 0,                  // Target coordinates (in this case, overwrite sources).
                4);                              // Number of points to transform.

        System.out.printf("Montreal:  %11.1f %11.1f%n", coordinates[0], coordinates[1]);
        System.out.printf("Vancouver: %11.1f %11.1f%n", coordinates[2], coordinates[3]);
        System.out.printf("Tokyo:     %11.1f %11.1f%n", coordinates[4], coordinates[5]);
        System.out.printf("Paris:     %11.1f %11.1f%n", coordinates[6], coordinates[7]);
    }

    /**
     * Projects the geographic coordinates of a few cities and prints the results.
     * The target Coordinate Reference Systems (CRS) are given in argument.
     * More than once CRS may be specified.
     *
     * @param  args                the EPSG code of target CRS. Example: "3395" for "WGS 84 / World Mercator".
     * @throws FactoryException    if an error occurred while creating a CRS or inferring the coordinate operation.
     * @throws TransformException  if an error occurred while applying the operation on coordinate values.
     */
    public static void main(String[] args) throws FactoryException, TransformException {
        if (args.length == 0) {
            args = new String[] {"3395"};
            System.out.println("No EPSG code given. Default to 3395 (WGS 84 / World Mercator).");
        }
        TransformPoints transformer = new TransformPoints();
        for (String target : args) {
            transformer.printCoordinates(target);
            System.out.println();
        }
    }
}
