/*
 *    Java bindings to PROJ
 *
 *    This file is hereby placed into the Public Domain.
 *    This means anyone is free to do whatever they wish with this file.
 */
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.metadata.quality.Result;
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
        describe(operation);
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
     * Prints a description of the given coordinate operation, in particular its domain of validity
     * and accuracy. It is user's responsibility to ensure that the coordinates to transform are in
     * the domain of validity.
     *
     * @param  operation  the coordinate operation to describe.
     */
    private static void describe(CoordinateOperation operation) {
        String name = operation.getName().getCode();
        System.out.printf("The coordinate operation is: %s%n", name);
        Extent extent = operation.getDomainOfValidity();
        if (extent != null) {
            /*
             * The extent may have horizontal, vertical and temporal components. In this example we take only
             * the horizontal components, and only the ones that are bounding boxes (other types are bounding
             * polygons and plain text descriptions). Those bounds are easy to use for checking coordinate
             * validity before transformation.
             */
            for (GeographicExtent ge : extent.getGeographicElements()) {
                if (ge instanceof GeographicBoundingBox) {
                    final GeographicBoundingBox bbox = (GeographicBoundingBox) ge;
                    System.out.printf("Its domain of validity is:%n"
                            + "    West bound longitude: %6.1f%n"
                            + "    East bound longitude: %6.1f%n"
                            + "    South bound latitude: %6.1f%n"
                            + "    North bound latitude: %6.1f%n",
                            bbox.getWestBoundLongitude(),
                            bbox.getEastBoundLongitude(),
                            bbox.getSouthBoundLatitude(),
                            bbox.getNorthBoundLatitude());
                }
            }
        }
        /*
         * The way to get accuracy is a bit unconvenient and depends on whether the accuracy is only a
         * description or is a quantitative measurement. The reason for this unconvenience is that the
         * quality API is designed for describing the quality of a wide range of phenomenons, not only
         * coordinate operations. Developers are encouraged to write their own convenience methods for
         * their needs.
         */
        for (PositionalAccuracy accuracy : operation.getCoordinateOperationAccuracy()) {
            for (Result result : accuracy.getResults()) {
                /*
                 * Result can be QuantitativeResult or ConformanceResult among others.
                 * For now we just print it.
                 */
                System.out.printf("Accuracy is: %s%n", result);
            }
        }
        System.out.println();
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
