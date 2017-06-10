package au.com.rsutton.navigation.graphslam;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import au.com.rsutton.navigation.graphslam.DimensionXYTheta.ComponentAngle;

public class DimensionXYThetaTest
{

	@Test
	public void checkThetaIsCalculatedCorrectly()
	{
		for (int i = 0; i < 360; i++)
		{
			DimensionXYTheta dimension = new DimensionXYTheta(0, 0, ComponentAngle.createComponentAngle(i));

			double degrees = dimension.getThetaDegrees().getAngle();
			if (degrees < 0)
			{
				degrees += 360;
			}
			System.out.println(i + " = " + degrees);
			assertTrue(Math.abs(degrees - i) < 2);
		}

	}

}
