package au.com.rsutton.navigation.graphslam.v3;

import static org.junit.Assert.fail;

import org.junit.Test;

public class GraphSlamV3TestNewSolver
{

	@Test
	public void test()
	{
		GraphSlamV3<GraphSlamNodeLinear> slam = new GraphSlamV3<>(getCtor());

		slam.move("p1", 20, 1);

		slam.dump();

		GraphSlamNodeLinear node0 = slam.addNode("zero", 0, 1);

		slam.dump();

		GraphSlamNodeLinear node1 = slam.addNode("one", 10, 1);

		slam.dump();

		GraphSlamNodeLinear node2 = slam.addNode("two", 3, 1);

		slam.dump();

		slam.addConstraint(9, node1, 1);

		slam.addConstraint(4, node2, 1);

		slam.move("p2", 5, 1);

		slam.addConstraint(-1, node2, 1);

		slam.solve();

		slam.dump();

		fail("Not yet implemented");
	}

	private GraphSlamNodeConstructor<GraphSlamNodeLinear> getCtor()
	{
		return new GraphSlamNodeConstructor<GraphSlamNodeLinear>()
		{

			@Override
			public GraphSlamNodeLinear construct(String name, double initialPosition)
			{
				return new GraphSlamNodeLinear(name, initialPosition);
			}
		};
	}

}
