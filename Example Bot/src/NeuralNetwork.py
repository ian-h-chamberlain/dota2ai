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

if numLayers < 1:
    print("Cannot have less than 1 hidden layer!")
    sys.exit(1)

# helpers for the shape of layers
inputShape = [1, numInputs]
outputShape = [1, numOutputs]
intoHiddenShape = [numInputs, numNodes]
fullHiddenShape = [numNodes, numNodes]
outHiddenShape = [numNodes, numOutputs]

#These lines establish the feed-forward part of the network used to choose actions
inputs = tf.placeholder(shape=inputShape,dtype=tf.float32, name="inputs")

# initialize Variable objects for each layer's weights
hiddenLayers = []

hiddenLayers.append(tf.Variable(tf.zeros(intoHiddenShape), name="weights_in"))

for i in range(numLayers - 1):
    hiddenLayers.append(
        tf.Variable(tf.zeros(fullHiddenShape), name=("weights_" + str(i)))
    )

hiddenLayers.append(tf.Variable(tf.zeros(outHiddenShape), name = "weights_out"))

layersToHere = inputs

# need to explicitly make assign operations so we can initialize in Java
for w in hiddenLayers:
    nameSuffix = w.name.split(':')[0]
    tf.assign(w,
            tf.random_uniform(w.shape, -0.1, 0.1, name=("random_" + nameSuffix)),
            name=("assign_" + nameSuffix))
    
    # and keep multiplying layers together for the final qOut
    layersToHere = tf.matmul(layersToHere, w, name=("result_" + nameSuffix))

qOut = layersToHere
predict = tf.argmax(qOut,1, name="predict")

# build the training model using new q-values and loss function
nextQ = tf.placeholder(shape=outputShape,dtype=tf.float32, name="nextQ")
loss = tf.clip_by_value(
        tf.reduce_mean(tf.square(nextQ - qOut)),
        0, 1.0e5, name="loss")
trainer = tf.train.GradientDescentOptimizer(learning_rate=1.0e-8)
updateModel = trainer.minimize(loss, name="updateModel")

# save our graph to a file for use with Java
graphRep = tf.get_default_graph().as_graph_def()

f = open(graphFile, "wb")
f.write(graphRep.SerializeToString())

print("Finished building " + graphFile)

    