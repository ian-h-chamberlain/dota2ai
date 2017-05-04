import tensorflow as tf
import numpy as np

tf.reset_default_graph()

#These lines establish the feed-forward part of the network used to choose actions
inputs1 = tf.placeholder(shape=[1,4],dtype=tf.float32, name="inputs1")

W = tf.Variable(tf.zeros([4, 3]), name="weights")

tf.assign(W, tf.random_uniform(
        [4, 3], 0, 0.01, name="randomUniform"
        ), name="assign")

Qout = tf.matmul(inputs1,W, name="qOut")
predict = tf.argmax(Qout,1, name="predict")

#Below we obtain the loss by taking the sum of squares difference between the target and prediction Q values.
nextQ = tf.placeholder(shape=[1,3],dtype=tf.float32, name="nextQ")
loss = tf.reduce_sum(tf.square(nextQ - Qout), name="loss")
trainer = tf.train.GradientDescentOptimizer(learning_rate=0.1)
updateModel = trainer.minimize(loss, name="updateModel")

graphRep = tf.get_default_graph().as_graph_def()

f = open('neuralNetwork.graph', 'wb')

f.write(graphRep.SerializeToString())

print("Finished building neuralNetwork.graph")