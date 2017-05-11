package de.lighti.dota2.bot;

import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Session.Runner;
import org.tensorflow.Tensor;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class NeuralNetwork {

	// Tensorflow representation
	Graph graph;
	Session tfSession;
	int numLayers = 5;
	int numNodes = 75;
	
	int numIterations = 0;
	
	float loss = 0;
	
	float[] inputs;
	float[] outputs;
	
	float gamma = 0.5f;
	float epsilon = 0.4f;
	float learningRate = 0.01f;

	public NeuralNetwork(int numInputs, int numOutputs){
		
		initGraph(numInputs, numOutputs);
		
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
	}

	// load a neural network from a file
	public NeuralNetwork(String filename, int numInputs, int numOutputs)
	{
		initGraph(numInputs, numOutputs);
		
		List<Tensor> weights = new ArrayList<Tensor>();
		
		ObjectInputStream ois = null;
		FileInputStream fis = null;
		float[][] blah = null;
		try {
			fis = new FileInputStream(filename);
			ois = new ObjectInputStream(fis);
			do {
				blah = (float[][]) ois.readObject();
				
				if (blah != null) {
					weights.add(Tensor.create(blah));
				}
			} while (blah != null);
		}
		catch (EOFException e) { } // Once we reach EOF we're done
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (ois != null) {
				try {
					ois.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		System.out.println(weights.size());

		// pass in our Tensor values to the graph
		Runner r = tfSession.runner();
		r.feed("phold_weights_in", weights.get(0))
			.addTarget("assign_phold_weights_in");
		r.feed("phold_weights_out", weights.get(1))
			.addTarget("assign_phold_weights_out");
		
		for (int i=2; i<weights.size(); i++)
		{
			r.feed("phold_weights_" + (i - 2), weights.get(i))
				.addTarget("assign_phold_weights_" + (i - 2));
		}
		
		r.run();
	}
	
	private void initGraph(int numInputs, int numOutputs)
	{
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
	}
	
	// store the network weights as a file
	public void saveWeights(String filename)
	{
		Runner r = tfSession.runner();
		
		r.fetch("weights_in")
			.fetch("weights_out");
		
		for (int i=0; i<numLayers - 1; i++)
		{
			r.fetch("weights_" + i);
		}
		
		List<Tensor> weights = r.run();
		
		ObjectOutputStream out = null;
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(filename);
			out = new ObjectOutputStream(fos);
			for (int i=0; i<weights.size(); i++) {
				long[] s = weights.get(i).shape();
				float[][] t = new float[(int) s[0]][(int) s[1]];
				weights.get(i).copyTo(t);
				out.writeObject(t);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (out != null) {
				try {
					out.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void setInputs(float[] input)  
	{
		inputs = input;
	}
	
	// After taking an action, back propagate the reward for that action based on a new state
	public void propagateReward(int[] action, float reward, float[] newInputs)
	{
		System.out.println("Propagating");
		numIterations++;

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
		
		loss = outs.get(0).floatValue();
		
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
