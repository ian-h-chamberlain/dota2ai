package de.lighti.dota2.bot;

import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class NeuralNetwork {

	// Tensorflow representation
	Graph graph;
	Session tfSession;
	int numLayers = 1;
	int numNodes = 15;
	
	float[] inputs;
	float[] outputs;
	
	float gamma = 0.99f;
	float epsilon = 0.05f;

	public NeuralNetwork(int numInputs, int numOutputs){
		
		outputs = new float[numOutputs];
		inputs = new float[numInputs];
		
		graph = new Graph();
		
		String graphFile = "neuralNetwork.graph";

		try {
			// run our python graph generator first
			ProcessBuilder pb = new ProcessBuilder(
					"python", "src\\NeuralNetwork.py",
					"" + numLayers, "" + numNodes, "" + inputs.length, "" + outputs.length,
					graphFile);

			System.out.println("*** GRAPH GENERATOR RUNNING ***\n");
			Process p = pb.start();
			p.waitFor();

			// show output from error stream
			BufferedReader bf = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			String line;
			while ((line = bf.readLine()) != null)
			{
				System.out.println(line);
			}
			
			// and input stream
			bf = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((line = bf.readLine()) != null)
			{
				System.out.println(line);
			}

			System.out.println("\n*** GRAPH GENERATOR COMPLETE ***");

			// finally, read the results into our Tensorflow graph
			File inFile = new File(graphFile);
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
			.addTarget("weights0")
			.addTarget("weights1")
			.addTarget("randomUniform0")
			.addTarget("randomUniform1")
			.addTarget("assign0")
			.addTarget("assign1")
			.run();
	}

	public void setInputs(float[] input)  
	{
		inputs = input;
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
			
			// propagateReward(targetQ, act, r);
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
		System.out.println("Propagating " + action);
		float[] targetQ = getQ();
		float[] oldInputs = inputs;
		setInputs(newInputs);
		propagateReward(oldInputs, targetQ, action, reward);
	}
	
	// helper function for back-propagation
	private void propagateReward(float[] oldInputs, float[] targetQ, int action, float reward)
	{
		// and get the next q-value and action
		float[] newQ = getQ(); 
		
		float maxQ = newQ[0];
		for (int i=0; i < newQ.length; i++)
		{
			if (maxQ < newQ[i])
				maxQ = newQ[i];
		}
		
		// update new q-values
		targetQ[action] = reward + gamma * maxQ;
		System.out.println("updated targetq[action] to " + targetQ[action]);
		
		// now run the update model to back-propagate reward
		Tensor in = Tensor.create(new float[][]{oldInputs});
		
		List<Tensor> outs = tfSession.runner()
			.feed("inputs", in)
			.feed("nextQ", Tensor.create(targetQ))
			.addTarget("updateModel")
			.fetch("loss")
			.run();
		
		float loss = outs.get(0).floatValue();
		
		System.out.println("Loss: " + loss);
		
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
				.feed("inputs", in)
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
				.feed("inputs", in)
				.fetch("qOut")
				.run()
				.get(0)
				.copyTo(newQ);
		
		System.out.print("Q: ");
		for (int i=0; i<outputs.length; i++)
		{
			System.out.print(newQ[0][i] + ",");
		}
		System.out.println();
		
		return newQ[0];
	}
}
