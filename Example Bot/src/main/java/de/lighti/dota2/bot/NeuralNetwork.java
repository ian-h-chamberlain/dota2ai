package de.lighti.dota2.bot;

import org.tensorflow.Graph;
import org.tensorflow.Output;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlow;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class NeuralNetwork {
	public Graph graph;
	public float[] inputs = {0.0f, 1.0f, 2.0f};
	public void setInputs(float[] input)  
	{
		inputs = input;
		
	}
	public NeuralNetwork(){
        // test tensorflow

        try (Graph g = new Graph()) 
        {
            final String value = "Hello from " + TensorFlow.version();
            graph = g;

            //You can create a tensor using a standard float[] object.
            //Not sure if the best way to handle this is to create multiple tensors with one data each
            //or compress all the data into one tensor.
            try (Tensor in = Tensor.create(inputs))
            {
            	//This is the same as
            	Output out = constant(g, "MyFloat", inputs);
            	//This
            	/*g.opBuilder("Const", "MyFloat")
            	.setAttr("dtype", in.dataType())
            	.setAttr("value", in)
            	.build();*/
            	//System.out.println(in.bytesValue());
            }
            
            // Construct the computation graph with a single operation, a constant
            // named "MyConst" with a value "value".
            try (Tensor t = Tensor.create(value.getBytes("UTF-8")))
            {
            	
              // The Java API doesn't yet include convenience functions for adding operations.
               g.opBuilder("Const", "MyConst").setAttr("dtype", t.dataType()).setAttr("value", t).build();
            }
            catch (UnsupportedEncodingException e) 
            {
            	e.printStackTrace();
            	System.exit(1);
            }

            // Execute the "MyConst" operation in a Session.
            try (Session s = new Session(g);
                 Tensor output = s.runner().fetch("MyFloat").run().get(0);
        		 Tensor output1 = s.runner().fetch("MyConst").run().get(0);
            	)
            {
            	
            	float[] outData = new float[3];
            	System.out.println(output.toString());
            	System.out.println(output1.toString());
            	
            	//Data from tensor output must be copied into DATATYPE buffer.
            	//Here, it's float[]
            	output.copyTo(outData);
            	for (int i = 0; i < outData.length; i++)
            	{
            		System.out.println(outData[i]);
            	}

            }
           /* catch (UnsupportedEncodingException e) {
            	e.printStackTrace();
            	System.exit(1);
            }*/
		}
		
	}
	
	//Convenience function for creating operations.
    Output constant(Graph g, String name, Object value) {
        try (Tensor t = Tensor.create(value)) {
          return g.opBuilder("Const", name)
              .setAttr("dtype", t.dataType())
              .setAttr("value", t)
              .build()
              .output(0);
        }
      }

	public void init(){
		
		
		
		
		
	}
}
