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
	public ArrayList<Float> inputs;
	public NeuralNetwork(){
        // test tensorflow
        try (Graph g = new Graph()) 
        {
            final String value = "Hello from " + TensorFlow.version();
            graph = g;
            //Model            
            
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
                 Tensor output = s.runner().fetch("MyConst").run().get(0)) 
            {
            	
              System.out.println(new String(output.bytesValue(), "UTF-8"));
            }
            catch (UnsupportedEncodingException e) {
            	e.printStackTrace();
            	System.exit(1);
            }
		}
		
	}
	public void init(){
		
		
		
		
		
	}
}
