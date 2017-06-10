package au.com.rsutton.hazelcast;

import java.util.LinkedList;
import java.util.List;

import au.com.rsutton.entryPoint.units.Distance;
import au.com.rsutton.mapping.particleFilter.ParticleFilterObservationSet;
import au.com.rsutton.mapping.particleFilter.ScanObservation;
import au.com.rsutton.robot.rover.Angle;
import au.com.rsutton.robot.rover.LidarObservation;

public class RobotLocation extends MessageBase<RobotLocation> implements ParticleFilterObservationSet
{

	private static final long serialVersionUID = 938950572423708619L;
	private Angle deadReaconingHeading;

	private Distance distanceTravelled;

	private Distance clearSpaceAhead;
	private long time = System.currentTimeMillis();
	private List<LidarObservation> observations;

	public RobotLocation()
	{
		super(HcTopic.LOCATION);

	}

	public void setTopic()
	{

		this.topicInstance = HazelCastInstance.getInstance().getTopic(HcTopic.LOCATION.toString());
	}

	public void setDeadReaconingHeading(Angle angle)
	{
		this.deadReaconingHeading = angle;

	}

	@Override
	public Angle getDeadReaconingHeading()
	{
		return deadReaconingHeading;
	}

	public void setClearSpaceAhead(Distance clearSpaceAhead)
	{
		this.clearSpaceAhead = clearSpaceAhead;

	}

	public Distance getClearSpaceAhead()
	{
		return clearSpaceAhead;
	}

	public long getTime()
	{
		return time;
	}

	public void addObservations(List<LidarObservation> observations)
	{
		this.observations = observations;

	}

	@Override
	public List<ScanObservation> getObservations()
	{
		List<ScanObservation> obs = new LinkedList<>();
		obs.addAll(observations);

		return obs;
	}

	public void setObservations(List<LidarObservation> observations)
	{
		this.observations = observations;
	}

	public Distance getDistanceTravelled()
	{
		return distanceTravelled;
	}

	public void setDistanceTravelled(Distance distanceTravelled)
	{
		this.distanceTravelled = distanceTravelled;
	}

	@Override
	public String toString()
	{
		return "RobotLocation [deadReaconingHeading=" + deadReaconingHeading + ", distanceTravelled="
				+ distanceTravelled + "]";
	}

}
