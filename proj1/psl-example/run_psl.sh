mvn -e clean compile;
mvn dependency:build-classpath -Dmdep.outputFile=classpath.out;
java -cp ./target/classes:`cat classpath.out` $1; 								# eg. class path($1) = edu.umd.cs.example.BasicExample