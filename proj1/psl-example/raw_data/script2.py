f = open("/home/vatsal/psl/proj1/psl-example/raw_data/IsPixel.txt", "a")

for i in range(0,64):
		for j in range(32,64):
			pixel = "P_"+str(i)+"_"+str(j)+"\n"
			f.write(pixel)

f.close()