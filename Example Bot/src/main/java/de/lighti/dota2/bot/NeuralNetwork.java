package de.lighti.dota2.bot;

import org.tensorflow.DataType;
import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.Output;
import org.tensorflow.Session;
import org.tensorflow.Shape;
import org.tensorflow.Tensor;

import java.util.List;

public class NeuralNetwork {

	// Tensorflow representation
	Graph graph;
	Output inputTensor;
	Output optimizer;
	Session tfSession;
	
	float[] inputs = {
			0.0f, 1.0f, 2.0f, 0.5f,
	};
	float[] outputs;

	public void setInputs(float[] input)  
	{
		inputs = input;
	}
	
	public NeuralNetwork(int numOutputs){
		
		outputs = new float[numOutputs];

		graph = new Graph();
		
		// set up the initial inputs to the neural network
		inputTensor = outputFromShape(
				"inputTensor", "Placeholder",
				Shape.make(1,  inputs.length),
				DataType.FLOAT);
		
		// and create a variable to hold the weights
		Output weightMatrix = outputFromShape(
				"weights", "Variable",
				Shape.make(inputs.length, outputs.length), // TODO: make a deep network
				DataType.FLOAT);

		Output wShape = constant("wShape", new int[]{inputs.length, outputs.length});
		
		// initialize with random values
		Output rValues = unaryOp("RandomUniform", wShape, DataType.FLOAT);
		assignVar(weightMatrix, rValues);
		
		// initialize session
		tfSession = new Session(graph);

		// assign the variable to matrix
		tfSession.runner().addTarget("weights")
				.addTarget("RandomUniform")
				.addTarget("assign")
				.run();
		
		// multiply the matrix by the input weights
		Output qOut = binaryOp("qOut", "MatMul", inputTensor, weightMatrix);

		// find max q-value and return index
		binaryOp("predict", "ArgMax", qOut, constant("1", 1));

		
		// TRAINING COMPONENTS
		Output nextQ = outputFromShape(
				"nextQ", "Placeholder",
				Shape.make(1, outputs.length),
				DataType.FLOAT);
		
		// calculate the squared difference between nextQ and qOut
		Output difference = binaryOp(
				"sub", "Sub",
				nextQ, qOut);
		
		Output squared = unaryOp("Square", difference);
		
		Output numDims = constant("dims", squared.shape().numDimensions());
		Output lossFunction = binaryOp(
				"sum", "Sum", squared, numDims);
		
		// TODO figure out how to get a gradient descent optimizer
	}
	
	// Run a series of fake iterations to test Q-learning
	public void testQ(int iters)
	{
		// not actually using real input yet
		setInputs(new float[]{
			0.5f, 1.5f, -0.5f, 1.0f,
		});
		
		float gamma = 0.99f;
		
		for (int i=0; i<iters; i++)
		{
			int act = getAction();
			float[] targetQ = getQ();
			float r = fakeReward(act);
			
			// set new state based on action
			if (act == 1)
				inputs[0] += 0.1f;
			else if (act == 2)
				inputs[0] -= 0.1f;
			
			// and get the next q-value and action
			float[] newQ = getQ(); 
			int maxQIndex = getAction();
			
			// find maxQ of new state
			float maxQ = newQ[maxQIndex];
			
			// update new q-values
			targetQ[act] = r + gamma * maxQ;
			
			// TODO propagate the new q-values back through the network
		}
	}
	
	private float fakeReward(int action)
	{
		if (action == 1)
		{
			if (inputs[0] < 4.0f)
				return 100;
			else
				return -100;
		}
		else if (action == 2)
		{
			if (inputs[0] > 4.0f)
				return 100;
			else
				return -100;
		}
		else
			return 0;
	}
	
	// Helper to pass inputs directly to the run method
	public int getAction(float[] ins)
	{
		setInputs(ins);
		return getAction();
	}
	
	// Execute one iteration of the algorithm
	public int getAction()
	{
		Tensor in = Tensor.create(new float[][]{inputs});

		// run network forward and get a prediction
		List<Tensor> output = tfSession.runner()
				.feed(inputTensor, in)
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
	
	public float[] getQ()
	{
		Tensor in = Tensor.create(new float[][]{inputs});

		float[][] newQ = new float[1][outputs.length];

		tfSession.runner()
				.feed(inputTensor, in)
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
	
	//Convenience function for creating operations.
    private Output constant(String name, Object value) {
        try (Tensor t = Tensor.create(value)) {
          return graph.opBuilder("Const", name)
              .setAttr("dtype", t.dataType())
              .setAttr("value", t)
              .build()
              .output(0);
        }
      }

    // Helper function to get an Output from two inputs
    private Output binaryOp(String name, String type, Output in1, Output in2) 
    {
          return graph.opBuilder(type, name)
        		  .addInput(in1)
        		  .addInput(in2)
        		  .build()
        		  .output(0);
    }
    
    // Helper function to assign Variable Outputs
    private Operation assignVar(Output ref, Output value)
    {
    	return graph.opBuilder("Assign", "assign")
    			.addInput(ref)
    			.addInput(value)
    			.setAttr("validate_shape", true)
    			.build();
    }
    
    // Helper function for general shape-based Outputs
    private Output outputFromShape(String name, String type, Shape shape, DataType dtype)
    {
    	return graph.opBuilder(type, name)
    			.setAttr("shape", shape)
    			.setAttr("dtype", dtype)
    			.build()
    			.output(0);
    }
    
    // Helper function for unary Outputs
    private Output unaryOp(String type, Output in, DataType dtype)
    {
    	return graph.opBuilder(type, type)
    			.addInput(in)
    			.setAttr("dtype", dtype)
    			.build()
    			.output(0);
    }

    private Output unaryOp(String type, Output in)
    {
    	return graph.opBuilder(type, type)
    			.addInput(in)
    			.build()
    			.output(0);
    }
}
