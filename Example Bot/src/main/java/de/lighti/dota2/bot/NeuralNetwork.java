package de.lighti.dota2.bot;

import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Session.Runner;
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
	int numLayers = 5;
	int numNodes = 75;
	
	int numIterations = 0;
	
	float[] inputs;
	float[] outputs;
	
	float gamma = 0.99f;
	float epsilon = 0.2f;
	float learningRate = 0.001f;

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
		
		// Assign input and output weights
		Runner init = tfSession.runner()
				.addTarget("weights_in")
				.addTarget("random_weights_in")
				.addTarget("assign_weights_in")
				.addTarget("weights_out")
				.addTarget("random_weights_out")
				.addTarget("assign_weights_out");
		
		// and hidden layer weights
		for (int i=0; i < numLayers - 1; i++)
		{
			init.addTarget("weights_" + i)
				.addTarget("random_weights_" + i)
				.addTarget("assign_weights_" + i);
		}

		init.run();
		
		// print out initial weights
		List<Tensor> weights = tfSession.runner()
			.fetch("weights_in")
			.fetch("weights_0")
			.fetch("weights_out")
			.run();
		
		float[][] ins = new float[inputs.length][numNodes];
		weights.get(0).copyTo(ins);
		float[][] w0 = new float[numNodes][numNodes];
		weights.get(1).copyTo(w0);
		float[][] outs = new float[numNodes][outputs.length];
		weights.get(2).copyTo(outs);
		
		System.out.println("Input weights:\n" + Arrays.deepToString(ins));
		System.out.println("weights l0:\n" + Arrays.deepToString(w0));
		System.out.println("Output weights:\n" + Arrays.deepToString(outs));
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
	public void propagateReward(int[] action, float reward, float[] newInputs)
	{
		System.out.println("Propagating");
		numIterations++;
		/*
		if (numIterations % 1000 == 0)
		{
			epsilon -= 0.05f * (int) (numIterations / 1000);
			if (epsilon < 0.0f)
				epsilon = 0.0f;
			System.err.println("lowering epsilon");
		}
		*/
		float[] targetQ = getQ();
		float[] oldInputs = inputs;
		setInputs(newInputs);
		propagateReward(oldInputs, targetQ, action, reward);
	}
	
	// helper function for back-propagation
	private void propagateReward(float[] oldInputs, float[] targetQ, int[] action, float reward)
	{
		// and get the next q-value and action
		float[] newQ = getQ();
		
		int[] predictActions = Agent.instance.networkProcessor.runNumbers(newQ, true);
		
		for (int i=0; i < action.length; i++)
		{
			targetQ[action[i]] = targetQ[action[i]] + learningRate * (reward + gamma * newQ[predictActions[i]] - targetQ[action[i]]);
			// update new q-values
		}
		
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
				.fetch("result_weights_out")
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
