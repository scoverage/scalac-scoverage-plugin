mvn clean install

# SCALA_HOME is typically /usr/share/java on ubuntu with apt-get
#cp target/scales.jar $SCALA_HOME/misc/scala-devel/plugins

#/home/sam/development/scala-2.10.2/bin/scalac -classpath "$SCALA_HOME/misc/scala-devel/plugins/scales.jar" Test.scala

#javap -c Test.class

cp target/scales.jar /home/sam/development/workspace/scales-test/lib/
