import tensorflow as tf
import numpy as np
import sys

tf.reset_default_graph()

if len(sys.argv) < 5:
    print("Invalid arguments to graph generator!")
    sys.exit(1)

# arguments:
# - number of layers for input
# - number of input nodes
# - number of output nodes
# - file to output to
numLayers, numInputs, numOutputs, graphFile = sys.argv[1:]

numLayers = int(numLayers)
numInputs = int(numInputs)
numOutputs = int(numOutputs)

if numLayers != 1:
    print("No support for " + numLayers + " layers yet!")
    sys.exit(1)

# helpers for the shape of layers
inputShape = [1, numInputs]
outputShape = [1, numOutputs]
middleShape = [numInputs, numOutputs]

#These lines establish the feed-forward part of the network used to choose actions
inputs = tf.placeholder(shape=inputShape,dtype=tf.float32, name="inputs")

W = tf.Variable(tf.zeros(middleShape), name="weights")

# need to explicitly assign so we can initialize in Java
tf.assign(W, tf.random_uniform(
        middleShape, 0, 0.01, name="randomUniform"
        ), name="assign")

Qout = tf.matmul(inputs,W, name="qOut")
predict = tf.argmax(Qout,1, name="predict")

# build the training model using new q-values and loss function
nextQ = tf.placeholder(shape=outputShape,dtype=tf.float32, name="nextQ")
loss = tf.reduce_sum(tf.square(nextQ - Qout), name="loss")
trainer = tf.train.GradientDescentOptimizer(learning_rate=0.1)
updateModel = trainer.minimize(loss, name="updateModel")

# save our graph to a file for use with Java
graphRep = tf.get_default_graph().as_graph_def()

f = open(graphFile, "wb")
f.write(graphRep.SerializeToString())

print("Finished building " + graphFile)