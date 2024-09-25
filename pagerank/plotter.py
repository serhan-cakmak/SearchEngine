import matplotlib.pyplot as plt
import numpy as np

a = []
with open("pagerank_best.txt") as f:
    for line in f.readlines():
        a.append(line.strip().split(" "))

b = [[] for i in range(4)]
n_vals = []
print(a)
for i in range(len(a)):
    n_vals.append(a[i][0])
    for j in range(1,5):

        b[j-1].append( float(a[i][j]))

b = np.array(b)
print(b)
labels = ["MC1", "MC2", "MC4", "MC5"]
print(n_vals)

for i in range(len(b)):
    plt.plot(n_vals, b[i], label=labels[i])

plt.legend()
plt.show()
