package de.lighti.dota2.bot;

import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;

public class NeuralNetwork {

	// Tensorflow representation
	Graph graph;
	Session tfSession;
	
	float[] inputs = {
			0.0f, 1.0f, 2.0f, 0.5f,
	};
	float[] outputs;
	
	float gamma = 0.99f;
	float epsilon = 0.1f;

	public void setInputs(float[] input)  
	{
		inputs = input;
	}
	
	public NeuralNetwork(int numOutputs){
		
		outputs = new float[numOutputs];
		
		graph = new Graph();

		try {
			// run our python graph generator first
			ProcessBuilder pb = new ProcessBuilder("python", "src\\NeuralNetwork.py");
			Process p = pb.start();
			p.waitFor();
			byte[] out = new byte[p.getInputStream().available()];
			p.getInputStream().read(out);
			System.out.print(new String(out));

			// and read the results into our Tensorflow graph
			File inFile = new File("neuralNetwork.graph");
			byte[] results = Files.readAllBytes(inFile.toPath());

			graph.importGraphDef(results);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		// initialize session
		tfSession = new Session(graph);
		
		// Assign all variables in the graph
		tfSession.runner()
			.addTarget("weights")
			.addTarget("randomUniform")
			.addTarget("assign")
			.run();
	}
	
	// Run a series of fake iterations to test Q-learning
	public void testQ(int iters)
	{
		// not actually using real input yet
		setInputs(new float[]{
			1.0f, 1.5f, -0.5f, 1.0f,
		});
		Random rn = new Random();
		
		for (int i=0; i<iters; i++)
		{
			int act = getAction();
			
			if (rn.nextFloat() < epsilon)
			{
				act = rn.nextInt(outputs.length);
			}
			
			float[] targetQ = getQ();
			
			// set new state based on action
			if (act == 0)
				inputs[0] += 0.01f;
			else if (act == 1)
				inputs[0] -= 0.01f;

			float r = fakeReward(act);
			
			propagateReward(targetQ, act, r);
		}
		
		System.out.print("[");
		for (int i=0; i<inputs.length; i++)
		{
			System.out.print(inputs[i] + ",");
		}
		System.out.println("]");
	}
	
	private float fakeReward(int action)
	{
		if (action == 0)
		{
			if (inputs[0] <= 1.0f)
				return 10;
			else
				return -10;
		}
		else if (action == 1)
		{
			if (inputs[0] >= 1.0f)
				return 10;
			else
				return -10;
		}
		else
			return 0;
	}
	
	// After taking an action, back propagate the reward for that action based on a new state
	public void propagateReward(int action, float reward, float[] newInputs)
	{
		float[] targetQ = getQ();
		setInputs(newInputs);
		propagateReward(targetQ, action, reward);
	}
	
	// helper function for back-propagation
	private void propagateReward(float[] targetQ, int action, float reward)
	{
		// and get the next q-value and action
		float[] newQ = getQ(); 
		int maxQIndex = getAction();
		
		// find maxQ of new state
		float maxQ = newQ[maxQIndex];
		
		// update new q-values
		targetQ[action] = reward + gamma * maxQ;
		
		// now run the update model to back-propagate reward
		Tensor in = Tensor.create(new float[][]{inputs});

		tfSession.runner()
			.feed("inputs1", in)
			.feed("nextQ", Tensor.create(targetQ))
			.addTarget("updateModel")
			.addTarget("weights")
			.run();
		
		// TODO reduce epsilon over iterations
	}
	
	// Helper to pass inputs directly to the run method
	public int getAction(float[] ins)
	{
		setInputs(ins);
		return getAction();
	}
	
	// Execute one iteration of the algorithm
	private int getAction()
	{
		Tensor in = Tensor.create(new float[][]{inputs});

		// run network forward and get a prediction
		List<Tensor> output = tfSession.runner()
				.feed("inputs1", in)
				.fetch("predict")	// and index of highest
				.run();
		
		long[] predictData = new long[1];
		output.get(0).copyTo(predictData);
		
		// return the index of the action we took
		return (int) predictData[0];
	}
	
	public float[] getQ(float[] ins)
	{
		setInputs(ins);
		return getQ();
	}
	
	private float[] getQ()
	{
		Tensor in = Tensor.create(new float[][]{inputs});

		float[][] newQ = new float[1][outputs.length];

		tfSession.runner()
				.feed("inputs1", in)
				.fetch("qOut")
				.run()
				.get(0)
				.copyTo(newQ);
		
		for (int i=0; i<outputs.length; i++)
		{
			System.out.print(newQ[0][i] + ",");
		}
		System.out.println();
		
		return newQ[0];
	}
}
