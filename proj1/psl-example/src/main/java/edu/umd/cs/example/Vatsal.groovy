/*
 * This file is part of the PSL software.
 * Copyright 2011-2015 University of Maryland
 * Copyright 2013-2015 The Regents of the University of California
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.umd.cs.example;

import java.text.DecimalFormat;

import edu.umd.cs.psl.application.inference.MPEInference;
import edu.umd.cs.psl.application.learning.weight.maxlikelihood.MaxLikelihoodMPE;
import edu.umd.cs.psl.config.*
import edu.umd.cs.psl.database.DataStore
import edu.umd.cs.psl.database.Database;
import edu.umd.cs.psl.database.DatabasePopulator;
import edu.umd.cs.psl.database.Partition;
import edu.umd.cs.psl.database.ReadOnlyDatabase;
import edu.umd.cs.psl.database.rdbms.RDBMSDataStore
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver.Type
import edu.umd.cs.psl.groovy.PSLModel;
import edu.umd.cs.psl.groovy.PredicateConstraint;
import edu.umd.cs.psl.groovy.SetComparison;
import edu.umd.cs.psl.model.argument.ArgumentType;
import edu.umd.cs.psl.model.argument.GroundTerm;
import edu.umd.cs.psl.model.argument.UniqueID;
import edu.umd.cs.psl.model.argument.Variable;
import edu.umd.cs.psl.model.atom.GroundAtom;
import edu.umd.cs.psl.model.function.ExternalFunction;
import edu.umd.cs.psl.ui.functions.textsimilarity.*
import edu.umd.cs.psl.ui.loading.InserterUtils;
import edu.umd.cs.psl.util.database.Queries;

/* 
 * The first thing we need to do is initialize a ConfigBundle and a DataStore
 */

/*
 * A ConfigBundle is a set of key-value pairs containing configuration options.
 * One place these can be defined is in psl-example/src/main/resources/psl.properties
 */
ConfigManager cm = ConfigManager.getManager()
ConfigBundle config = cm.getBundle("img-complete")

/* Uses H2 as a DataStore and stores it in a temp. directory by default */
def defaultPath = System.getProperty("java.io.tmpdir")
String dbpath = config.getString("dbpath", defaultPath + File.separator + "img-complete")
DataStore data = new RDBMSDataStore(new H2DatabaseDriver(Type.Disk, dbpath, true), config)

/*
 * Now we can initialize a PSLModel, which is the core component of PSL.
 * The first constructor argument is the context in which the PSLModel is defined.
 * The second argument is the DataStore we will be using.
 */
PSLModel m = new PSLModel(this, data)

/*
 * In this example program, the task is to align two social networks, by
 * identifying which pairs of users are the same across networks.
 */

/* 
 * We create four predicates in the model, giving their names and list of argument types
 */
m.add predicate: "ObsBright",     types: [ArgumentType.UniqueID, ArgumentType.UniqueID]
m.add predicate: "InfBright",     types: [ArgumentType.UniqueID, ArgumentType.UniqueID]
m.add predicate: "IsPixel",	   types: [ArgumentType.UniqueID] 
m.add predicate: "Image",	   types: [ArgumentType.UniqueID] 
m.add function: "North", 	   implementation : new North()
m.add function: "South", 	   implementation : new South()
m.add function: "West", 	   implementation : new West()
m.add function: "East", 	   implementation : new East()
m.add function: "MirrorVertical", 	   implementation : new MirrorVertical()
m.add function: "Equals", 	   implementation : new Equals()
// JaiMataDi

def pixel = '';


for (i in (Globals.PIXEL_MIN..Globals.PIXEL_MAX)) {
	for (j in (Globals.PIXEL_MIN..(Globals.PIXEL_MAX/2))) {

		pixel = getPixelId(i,j)
		GroundTerm p = data.getUniqueID(pixel);
		
		m.add rule : ( ObsBright(p,I) & MirrorVertical(p,A) & IsPixel(A) & Image(I) ) >> InfBright(A,I), weight:3
		m.add rule : ( ObsBright(p,I) & MirrorVertical(p,A) & IsPixel(A) & Image(I) ) >> ~InfBright(A,I), weight:2
		m.add rule : ( ~ObsBright(p,I) & MirrorVertical(p,A) & IsPixel(A) & Image(I) ) >> InfBright(A,I), weight:2
		m.add rule : ( ~ObsBright(p,I) & MirrorVertical(p,A) & IsPixel(A) & Image(I) ) >> ~InfBright(A,I), weight:3

	}
}


for (i in (Globals.PIXEL_MIN..Globals.PIXEL_MAX)) {
	for (j in ( (Globals.PIXEL_MAX+1)/2..Globals.PIXEL_MAX) ) {

		pixel = getPixelId(i,j)
		GroundTerm p = data.getUniqueID(pixel);
		
		m.add rule : ( InfBright(p,I) & North(p,A) & IsPixel(A) & Image(I) ) >> InfBright(A,I), weight:2
		m.add rule : ( InfBright(p,I) & North(p,A) & IsPixel(A) & Image(I) ) >> ~InfBright(A,I), weight:2
		m.add rule : ( ~InfBright(p,I) & North(p,A) & IsPixel(A) & Image(I) ) >> InfBright(A,I), weight:2
		m.add rule : ( ~InfBright(p,I) & North(p,A) & IsPixel(A) & Image(I) ) >> ~InfBright(A,I), weight:2

		m.add rule : ( InfBright(p,I) & East(p,A) & IsPixel(A) & Image(I) ) >> InfBright(A,I), weight:2
		m.add rule : ( InfBright(p,I) & East(p,A) & IsPixel(A) & Image(I) ) >> ~InfBright(A,I), weight:2
		m.add rule : ( ~InfBright(p,I) & East(p,A) & IsPixel(A) & Image(I) ) >> InfBright(A,I), weight:2
		m.add rule : ( ~InfBright(p,I) & East(p,A) & IsPixel(A) & Image(I) ) >> ~InfBright(A,I), weight:2

		m.add rule : ( InfBright(p,I) & South(p,A) & IsPixel(A) & Image(I) ) >> InfBright(A,I), weight:2
		m.add rule : ( InfBright(p,I) & South(p,A) & IsPixel(A) & Image(I) ) >> ~InfBright(A,I), weight:2
		m.add rule : ( ~InfBright(p,I) & South(p,A) & IsPixel(A) & Image(I) ) >> InfBright(A,I), weight:2
		m.add rule : ( ~InfBright(p,I) & South(p,A) & IsPixel(A) & Image(I) ) >> ~InfBright(A,I), weight:2

		m.add rule : ( InfBright(p,I) & West(p,A) & IsPixel(A) & Image(I) ) >> InfBright(A,I), weight:2
		m.add rule : ( InfBright(p,I) & West(p,A) & IsPixel(A) & Image(I) ) >> ~InfBright(A,I), weight:2
		m.add rule : ( ~InfBright(p,I) & West(p,A) & IsPixel(A) & Image(I) ) >> InfBright(A,I), weight:2
		m.add rule : ( ~InfBright(p,I) & West(p,A) & IsPixel(A) & Image(I) ) >> ~InfBright(A,I), weight:2

	}
}

// print model
println m;

def evidencePartition = new Partition(0);
def dir = 'data'+java.io.File.separator+'sn'+java.io.File.separator;

def insert = data.getInserter(ObsBright, evidencePartition)
//InserterUtils.loadDelimitedDataTruth(insert, dir+"vatsal_ObsBright.txt");
InserterUtils.loadDelimitedDataTruth(insert, dir+"train-input-olivetti.txt");

insert = data.getInserter(IsPixel, evidencePartition)
//InserterUtils.loadDelimitedData(insert, dir+"vatsal_IsPixel.txt");
InserterUtils.loadDelimitedData(insert, dir+"IsPixel.txt");

insert = data.getInserter(Image, evidencePartition)
//InserterUtils.loadDelimitedData(insert, dir+"vatsal_Image.txt");
InserterUtils.loadDelimitedData(insert, dir+"Image.txt");

def targetPartition = new Partition(1);
Database db = data.getDatabase(targetPartition, [ObsBright, IsPixel, Image] as Set, evidencePartition);


/*
 * Before running inference, we have to add the target atoms to the database.
 * If inference (or learning) attempts to access an atom that is not in the database,
 * it will throw an exception.
 * 
 * The below code builds a set of all users, then uses a utility class
 * (DatabasePopulator) to create all possible SamePerson atoms between users of
 * each network.
 */

Set<GroundTerm> pixelsA = new HashSet<GroundTerm>();
Set<GroundTerm> imagesB = new HashSet<GroundTerm>();


for (i in (Globals.PIXEL_MIN..Globals.PIXEL_MAX)) {
	for (j in ( (Globals.PIXEL_MAX+1)/2..Globals.PIXEL_MAX) ) {
			pixel = getPixelId(i,j)
			GroundTerm p = data.getUniqueID(pixel);
			pixelsA.add(p);
		}
	}

for (i in (1..Globals.NO_IMAGES)){
	imagesB.add(data.getUniqueID(i));
}

Map<Variable, Set<GroundTerm>> popMap = new HashMap<Variable, Set<GroundTerm>>();
popMap.put(new Variable("PixelA"), pixelsA)
popMap.put(new Variable("ImageB"), imagesB)


DatabasePopulator dbPop = new DatabasePopulator(db);
dbPop.populate((InfBright(PixelA, ImageB)).getFormula(), popMap);

/*
 * Now we can run inference
 */

MPEInference inferenceApp = new MPEInference(m, db, config);
inferenceApp.mpeInference();
inferenceApp.close();

println "Inference results with hand-defined weights:"
DecimalFormat formatter = new DecimalFormat("#.##");
for (GroundAtom atom : Queries.getAllAtoms(db, InfBright))
	println atom.toString() + "\t" + formatter.format(atom.getValue());


println 'Main'
////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////

Partition trueDataPartition = new Partition(2);
insert = data.getInserter(InfBright, trueDataPartition)
//InserterUtils.loadDelimitedDataTruth(insert, dir + "vatsal_InfBright.txt");
InserterUtils.loadDelimitedDataTruth(insert, dir + "train-output-olivetti.txt");

Database trueDataDB = data.getDatabase(trueDataPartition, [InfBright] as Set);
MaxLikelihoodMPE weightLearning = new MaxLikelihoodMPE(m, db, trueDataDB, config);
weightLearning.learn();
weightLearning.close();

/*
 * Let's have a look at the newly learned weights.
 */
println ""
println "Learned model:"
println m

/*
 * Now, we apply the learned model to a different social network alignment data set.
 * We load the data set as before (into new partitions) and run inference.
 * Finally, we print the results.
 */

/*
 * Loads evidence
 */
Partition evidencePartition2 = new Partition(3);

insert = data.getInserter(ObsBright, evidencePartition2)
//InserterUtils.loadDelimitedDataTruth(insert, dir+"vatsal_ObsBright.txt");
InserterUtils.loadDelimitedDataTruth(insert, dir+"Test_input.txt");

insert = data.getInserter(IsPixel, evidencePartition2)
//InserterUtils.loadDelimitedData(insert, dir+"vatsal_IsPixel.txt");
InserterUtils.loadDelimitedData(insert, dir+"Test_pixel.txt");

insert = data.getInserter(Image, evidencePartition2)
//InserterUtils.loadDelimitedData(insert, dir+"vatsal_Image.txt");
InserterUtils.loadDelimitedData(insert, dir+"Test_image.txt");

/*
 * Populates targets
 */


def targetPartition2 = new Partition(4);
Database db2 = data.getDatabase(targetPartition2, [ObsBright, IsPixel, Image] as Set, evidencePartition2);

pixelsA.clear();
for (i in (Globals.PIXEL_MIN..Globals.PIXEL_MAX)) {
	for (j in ( (Globals.PIXEL_MAX+1)/2..Globals.PIXEL_MAX) ) {
			pixel = getPixelId(i,j)
			GroundTerm p = data.getUniqueID(pixel);
			pixelsA.add(p);
		}
	}

imagesB.clear();

for (i in (1..Globals.NO_IMAGES_TEST)){
	imagesB.add(data.getUniqueID(i));
}

dbPop = new DatabasePopulator(db2);
dbPop.populate((InfBright(PixelA, ImageB)).getFormula(), popMap);

/*
 * Performs inference
 */
inferenceApp = new MPEInference(m, db2, config);
result = inferenceApp.mpeInference();
inferenceApp.close();

println "Inference results on second social network with learned weights:"
formatter = new DecimalFormat("#.##");
for (GroundAtom atom : Queries.getAllAtoms(db2, InfBright))
	println atom.toString() + "\t" + formatter.format(atom.getValue());
	
/*
 * We close the Databases to flush writes
 */
db.close();
trueDataDB.close();
db2.close();

///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////

def getPixelId(i,j){
	def pixel = 'P_' + i.toString() + '_' + j.toString()
	return pixel
}

def getPixelCord(pixel){
	pixel = pixel.split('_')
	def i = pixel[1].toInteger()
	def j = pixel[2].toInteger()
	return [i,j]
}

def getPixelMirrorCord(pixel){
	pixel = pixel.split('_')
	def i = pixel[1].toInteger()
	i = Globals.PIXEL_MAX - i
	def j = pixel[2].toInteger()
	return [i,j]
}

public class Globals {
	
	public static final int PIXEL_MAX = 63
	public static final int PIXEL_MIN = 0
	public static final int DEBUG = 1
	public static final int NO_IMAGES = 20
	public static final int NO_IMAGES_TEST = 1

	public static debugOut(String s){
		if(DEBUG){println s}
	}

}

class North implements ExternalFunction {
	@Override
	public int getArity() {
		return 2;
	}
	@Override
	public ArgumentType[] getArgumentTypes() {
		return [ArgumentType.UniqueID, ArgumentType.UniqueID].toArray();
	}
	@Override
	public double getValue(ReadOnlyDatabase db, GroundTerm... args) {
		
		def pixel  = args[0].toString().split("_")
		def p_i = pixel[1].toInteger()
		def p_j = pixel[2].toInteger()
		
		def neighbor = args[1].toString().split("_")
		def n_i = neighbor[1].toInteger()
		def n_j = neighbor[2].toInteger()
		if ( p_j==n_j && p_i==(n_i+1) ) {
			return 1.0
		}
		else {
			return 0.0
		}
	}
}

class East implements ExternalFunction {
	@Override
	public int getArity() {
		return 2;
	}
	@Override
	public ArgumentType[] getArgumentTypes() {
		return [ArgumentType.UniqueID, ArgumentType.UniqueID].toArray();
	}
	@Override
	public double getValue(ReadOnlyDatabase db, GroundTerm... args) {
		
		def pixel  = args[0].toString().split("_")
		def p_i = pixel[1].toInteger()
		def p_j = pixel[2].toInteger()

		def neighbor = args[1].toString().split("_")
		def n_i = neighbor[1].toInteger()
		def n_j = neighbor[2].toInteger()

		if ( p_i==n_i && p_j==(n_j-1) ) {
			return 1.0
		}
		else {
			return 0.0
		}
	}
	
}

class South implements ExternalFunction {
	@Override
	public int getArity() {
		return 2;
	}
	@Override
	public ArgumentType[] getArgumentTypes() {
		return [ArgumentType.UniqueID, ArgumentType.UniqueID].toArray();
	}
	@Override
	public double getValue(ReadOnlyDatabase db, GroundTerm... args) {
		
		def pixel  = args[0].toString().split("_")
		def p_i = pixel[1].toInteger()
		def p_j = pixel[2].toInteger()

		def neighbor = args[1].toString().split("_")
		def n_i = neighbor[1].toInteger()
		def n_j = neighbor[2].toInteger()

		if ( p_j==n_j && p_i==(n_j-1) ) {
			return 1.0
		}
		else {
			return 0.0
		}
	}
	
}

class West implements ExternalFunction {
	@Override
	public int getArity() {
		return 2;
	}
	@Override
	public ArgumentType[] getArgumentTypes() {
		return [ArgumentType.UniqueID, ArgumentType.UniqueID].toArray();
	}
	@Override
	public double getValue(ReadOnlyDatabase db, GroundTerm... args) {
		def pixel  = args[0].toString().split("_")
		def p_i = pixel[1].toInteger()
		def p_j = pixel[2].toInteger()

		def neighbor = args[1].toString().split("_")
		def n_i = neighbor[1].toInteger()
		def n_j = neighbor[2].toInteger()

		if ( p_i==n_i && p_j==(n_j+1) ) {
			return 1.0
		}
		else {
			return 0.0
		}
	}
	
}

class MirrorVertical implements ExternalFunction {
	@Override
	public int getArity() {
		return 2;
	}
	@Override
	public ArgumentType[] getArgumentTypes() {
		return [ArgumentType.UniqueID, ArgumentType.UniqueID].toArray();
	}
	@Override
	public double getValue(ReadOnlyDatabase db, GroundTerm... args) {
		def pixel  = args[0].toString().split("_")
		def p_i = pixel[1].toInteger()
		def p_j = pixel[2].toInteger()

		def neighbor = args[1].toString().split("_")
		def n_i = neighbor[1].toInteger()
		def n_j = neighbor[2].toInteger()
		'''
		println "----------------"
		print p_i
		println p_j
		print n_i
		println n_j
		print "----------------"
		'''
		n_j = Globals.PIXEL_MAX - n_j		
		if ( p_j==n_j && p_i==n_i ) {
			return 1.0
		}
		else {
			return 0.0
		}
	}
	
}

class Equals implements ExternalFunction {
	@Override
	public int getArity() {
		return 2;
	}
	@Override
	public ArgumentType[] getArgumentTypes() {
		return [ArgumentType.String, ArgumentType.String].toArray();
	}
	@Override
	public double getValue(ReadOnlyDatabase db, GroundTerm... args) {
		if ( args[0].toString()==args[0].toString() ) {
			return 1.0
		}
		else {
			return 0.0
		}
	}
	
}
