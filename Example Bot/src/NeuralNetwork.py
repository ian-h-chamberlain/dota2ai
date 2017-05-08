import tensorflow as tf
import numpy as np
import sys

tf.reset_default_graph()

if len(sys.argv) != 6:
    print("Invalid arguments to graph generator!")
    sys.exit(1)

# arguments:
# - number of hidden layers
# - number of hidden layer nodes
# - number of input nodes
# - number of output nodes
# - file to output to
numLayers, numNodes, numInputs, numOutputs, graphFile  = sys.argv[1:]

numLayers = int(numLayers)
numNodes = int(numNodes)
numInputs = int(numInputs)
numOutputs = int(numOutputs)

if numLayers != 1:
    print("No support for " + numLayers + " layers yet!")
    sys.exit(1)

# helpers for the shape of layers
inputShape = [1, numInputs]
outputShape = [1, numOutputs]
intoHiddenShape = [numInputs, numNodes]
outHiddenShape = [numNodes, numOutputs]

#These lines establish the feed-forward part of the network used to choose actions
inputs = tf.placeholder(shape=inputShape,dtype=tf.float32, name="inputs")

w1 = tf.Variable(tf.zeros(intoHiddenShape), name="weights0")
w2 = tf.Variable(tf.zeros(outHiddenShape), name = "weights1")

# need to explicitly assign so we can initialize in Java
tf.assign(w1, tf.random_uniform(
        intoHiddenShape, 0, 0.01, name="randomUniform0"
        ), name="assign0")
        
tf.assign(w2, tf.random_uniform(
        outHiddenShape, 0, 0.01, name="randomUniform1"
        ), name="assign1")
    
qOut = tf.matmul(tf.matmul(inputs, w1), w2, name="qOut")
predict = tf.argmax(qOut,1, name="predict")

# build the training model using new q-values and loss function
nextQ = tf.placeholder(shape=outputShape,dtype=tf.float32, name="nextQ")
loss = tf.clip_by_value(
        tf.reduce_mean(tf.square(nextQ - qOut)),
        -1.0e3, 1.0e3, name="loss")
trainer = tf.train.GradientDescentOptimizer(learning_rate=1.0e-8)
updateModel = trainer.minimize(loss, name="updateModel")

tf.add_check_numerics_ops()

# save our graph to a file for use with Java
graphRep = tf.get_default_graph().as_graph_def()

f = open(graphFile, "wb")
f.write(graphRep.SerializeToString())

print("Finished building " + graphFile)

    