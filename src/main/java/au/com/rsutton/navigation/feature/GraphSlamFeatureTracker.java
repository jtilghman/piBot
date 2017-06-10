package au.com.rsutton.navigation.feature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import au.com.rsutton.entryPoint.controllers.HeadingHelper;
import au.com.rsutton.navigation.graphslam.DimensionCertainty;
import au.com.rsutton.navigation.graphslam.v3.DimensionWrapperXYTheta;
import au.com.rsutton.navigation.graphslam.v3.GraphSlamV3XYTheta;

public class GraphSlamFeatureTracker
{

	GraphSlamV3XYTheta slam;
	private DimensionWrapperXYTheta currentLocation;

	Map<Feature, DimensionWrapperXYTheta> featureMap = new HashMap<>();

	double stablizedX = 0;
	double stablizedY = 0;

	public GraphSlamFeatureTracker()
	{
		DimensionCertainty certainty = new DimensionCertainty(new double[] {
				1, 1, 1, 1 });

		slam = new GraphSlamV3XYTheta(0, 0, 0, certainty);
	}

	public void setNewLocation(double dx, double dy, double dTheta, double certain)
	{
		DimensionCertainty certainty = new DimensionCertainty(new double[] {
				certain, certain, certain, certain });

		currentLocation = slam.setNewLocation(dx, dy, dTheta, certainty);

		stablizedX = (stablizedX * 0.9) + (currentLocation.getX() * 0.1);
		stablizedY = (stablizedY * 0.9) + (currentLocation.getY() * 0.1);

		slam.solve();

		System.out.println(
				"DUmping		 -------------------------------------------------------------------------------------------");
		// slam.dumpPositionsY();
		refreshFeatureMapFromSlam();

		System.out.println("Current location " + currentLocation);

	}

	public void addObservations(List<Feature> spikes, double heading)
	{
		double matches = 1;
		for (Feature spike : spikes)
		{
			Feature offsetSpike = new Feature(spike.x, spike.y, spike.angle, spike.getAngleAwayFromWall(),
					spike.getFeatureType());
			if (checkForMatch(offsetSpike, heading, 40))
			{
				matches++;
			}
		}

		for (Feature spike : spikes)
		{
			double dx = stablizedX - spike.x;
			double dy = stablizedY - spike.y;
			double distance = Math.sqrt((dx * dx) + (dy * dy));
			if (distance > 75)
			{
				Feature offsetSpike = new Feature(spike.x, spike.y, spike.angle, spike.getAngleAwayFromWall(),
						spike.getFeatureType());
				addObservation(offsetSpike, heading, matches / 5.0);
			}
		}

	}

	boolean checkForMatch(Feature newObservation, double pfAngle, double maxMatchDistance)
	{

		Vector3D spd = new Vector3D(newObservation.x, newObservation.y, 0);

		double thetaDegrees = pfAngle;

		Rotation rotation = new Rotation(RotationOrder.XYZ, 0, 0, Math.toRadians(thetaDegrees));

		Vector3D relativeXY = rotation.applyTo(spd);
		Vector3D absoluteXY = relativeXY.add(new Vector3D(stablizedX, stablizedY, 0));

		double absoluteTheta = HeadingHelper.normalizeHeading(newObservation.angle + thetaDegrees);

		// create absolute Spike for feature matching
		Feature absoluteSpike = new Feature(absoluteXY.getX(), absoluteXY.getY(), absoluteTheta,
				newObservation.angleAwayFromWall, newObservation.getFeatureType());

		// create adjusted Spike to compensate for the relative heading

		return !findFeaturesNear(absoluteSpike, maxMatchDistance, 40).isEmpty();

	}

	public void addObservation(Feature newObservation, double pfAngle, double certaintyModifier)
	{

		Vector3D spd = new Vector3D(newObservation.x, newObservation.y, 0);

		double thetaDegrees = pfAngle;

		Rotation rotation = new Rotation(RotationOrder.XYZ, 0, 0, Math.toRadians(thetaDegrees));

		Vector3D relativeXY = rotation.applyTo(spd);
		Vector3D absoluteXY = relativeXY.add(new Vector3D(stablizedX, stablizedY, 0));

		double absoluteTheta = HeadingHelper.normalizeHeading(newObservation.angle + thetaDegrees);

		// create absolute Spike for feature matching
		Feature absoluteFeature = new Feature(absoluteXY.getX(), absoluteXY.getY(), absoluteTheta,
				newObservation.angleAwayFromWall, newObservation.getFeatureType());

		System.out.println("Raw      " + newObservation);
		System.out.println("Absolute " + absoluteFeature);

		// create adjusted Spike to compensate for the relative heading

		double dx = absoluteFeature.x - stablizedX;
		double dy = absoluteFeature.y - stablizedY;
		double dTheta = absoluteFeature.angle - currentLocation.getTheta();

		double x = absoluteFeature.x;
		double y = absoluteFeature.y;

		double maxMatchDistance = 50;
		Map<Feature, DimensionWrapperXYTheta> nearFeatures = findFeaturesNear(absoluteFeature, maxMatchDistance, 40);
		System.out
				.println("-------------------------------------------------------------------------------------------");
		if (!nearFeatures.isEmpty())
		{
			double matchedFeatures = nearFeatures.size();
			for (Entry<Feature, DimensionWrapperXYTheta> matched : nearFeatures.entrySet())
			{

				double distance = Vector3D.distance(new Vector3D(x, y, 0),
						new Vector3D(matched.getValue().getX(), matched.getValue().getY(), 0));

				double certaintiy = Math.max(1.0 - ((distance) / maxMatchDistance), .01);

				// reduce the certainty by the number of features matched.
				certaintiy = certaintiy / (matchedFeatures * 2.0);

				System.out.println("Updating feature " + matched.getValue());
				// TODO: apportion certainty to quality of match
				update(matched.getValue(), dx, dy, dTheta, certaintiy * certaintyModifier);

			}
		} else
		{
			// dont allow adding distant features

			Map<Feature, DimensionWrapperXYTheta> furtherFeatures = findFeaturesNear(absoluteFeature, maxMatchDistance,
					100);
			if (furtherFeatures.isEmpty())
			{
				// only add a new feature if there are no features within a
				// meter

				double distance = Math.sqrt(Math.pow(newObservation.x, 2) + Math.pow(newObservation.y, 2));

				// disallow adding distant features
				if (distance < 200)
				{

					double certaintiy = Math.max(1.0 - ((distance) / maxMatchDistance), .01);
					System.out.println("Adding new Feature");
					featureMap.put(absoluteFeature, addNewFeature(dx, dy, dTheta, certaintiy * certaintyModifier));
				}
			}
		}
	}

	private void refreshFeatureMapFromSlam()
	{
		for (Entry<Feature, DimensionWrapperXYTheta> entry : featureMap.entrySet())
		{
			Feature spike = entry.getKey();
			DimensionWrapperXYTheta dimesion = entry.getValue();
			spike.x = dimesion.getX();
			spike.y = dimesion.getY();
			spike.angle = dimesion.getTheta();
		}
	}

	private DimensionWrapperXYTheta addNewFeature(double dx, double dy, double dTheta, double certainty2)
	{
		DimensionCertainty certainty = new DimensionCertainty(new double[] {
				certainty2, certainty2, certainty2, certainty2 });
		DimensionWrapperXYTheta position = slam.add("feature", dx, dy, dTheta, certainty);
		return position;
	}

	private void update(DimensionWrapperXYTheta position, double dx, double dy, double dTheta, double matchCertainty)
	{

		double maxDistance = 500;
		double distance = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
		double c = matchCertainty * ((maxDistance - distance) / maxDistance);

		DimensionCertainty certainty = new DimensionCertainty(new double[] {
				c, c, c, c });
		slam.update(position, dx, dy, dTheta, certainty);

	}

	private Map<Feature, DimensionWrapperXYTheta> findFeaturesNear(Feature feature, double range, double angleRange)
	{
		Map<Feature, DimensionWrapperXYTheta> result = new HashMap<>();

		for (Entry<Feature, DimensionWrapperXYTheta> entry : featureMap.entrySet())
		{
			Feature existingFeature = entry.getKey();
			if (feature.getDistance(existingFeature) < range)
			{
				// within distance
				// if (Math.abs(HeadingHelper.getChangeInHeading(feature.angle,
				// spike.angle)) < angleRange)

				if (feature.getFeatureType() == existingFeature.getFeatureType())
				{
					// correct angle
					if (feature.angleAwayFromWall == existingFeature.angleAwayFromWall)
					{
						// correct orientation
						result.put(entry.getKey(), entry.getValue());

					}
				}

			}
		}

		return result;
	}

	public Set<Feature> getFeatures()
	{
		return featureMap.keySet();
	}

	public DimensionWrapperXYTheta getCurrentLocation()
	{
		return currentLocation;
	}

}
