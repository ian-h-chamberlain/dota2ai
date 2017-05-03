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
	Session tfSession;
	
	float[] inputs = {
			0.0f, 1.0f, 2.0f
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
		
		tfSession = new Session(graph);
		
		// multiply the matrix by the input weights
		Output qOut = binaryOp("qOut", "MatMul", inputTensor, weightMatrix);

		// find max q-value and return index
		binaryOp("predict", "ArgMax", qOut, constant("1", 1));

	}
	
	// Execute the the Q-learning algorithm in a series of iterations
	public void runOnce()
	{
		
		Tensor in = Tensor.create(new float[][]{inputs});

		// assign the variable to matrix
		tfSession.runner().addTarget("weights")
				.addTarget("RandomUniform")
				.addTarget("assign")
				.run();

		// run network forward and get a prediction
		List<Tensor> output = tfSession.runner()
				.feed(inputTensor, in)
				.fetch("qOut")		// get q-values
				.fetch("predict")	// and index of highest
				.run();
		
		// print out data
		float[][] outData = new float[1][outputs.length];
		output.get(0).copyTo(outData);
		
		System.out.print("Q:values: ");
		for (int i = 0; i < outData[0].length; i++)
		{
			System.out.print(outData[0][i] + ", ");
		}
		
		long[] predictData = new long[1];
		output.get(1).copyTo(predictData);
		
		System.out.println("\nPicked: " + predictData[0]);
	}
	
	//Convenience function for creating operations.
    Output constant(String name, Object value) {
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
}
