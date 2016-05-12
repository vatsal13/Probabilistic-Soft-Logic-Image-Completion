from os import listdir
from os.path import isfile, join
import re

mypath = "/home/vatsal/psl/proj1/psl-example/raw_data/olivetti/"

files = [f for f in listdir(mypath) if isfile(join(mypath, f))]
files.sort()
A = []
for i,j in enumerate(files[0:1]):
	with open(mypath+j) as fp:
		count = 0;
		for line in fp:
			count+=1
			a = re.split(" +", line.strip())
			A.append(a)

print "COUNT : ",count
print len(A)

f = open("/home/vatsal/psl/proj1/psl-example/raw_data/comp-del.txt", "a")

for img in range(41,42):
	f.write("//Image " + str(img+1) + "\n")
	for i in range(0,64):
		for j in range(0,32):
			pixel = "P_"+str(i)+"_"+str(j)
			value = A[i+(64*j)][img]
			#print i," ",j
			str_print = "P_"+str(i)+"_"+str(j) + "\t" + str(1) + "\t" + str(float(value)/255.0) + "\n"
			#f.write(str(float(value)) + ' ')
			f.write(str_print)

f.close()

# f = open("foo.txt", "r")
# line = f.readline()
# print line
# f.close()