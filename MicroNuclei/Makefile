JFLAGS = -g
JC = javac
BUILD = ../build
SOURCEFILES = src/org/micromanager/micronuclei/MicroNucleiPlugin.java \
				 src/org/micromanager/micronuclei/MicroNucleiPlugin.java
IJJARPATH = /Applications/Micro-Manager1.4/ij.jar
MMJARPATH = /Applications/Micro-Manager1.4/plugins/Micro-Manager/MMJ_.jar
MMCOREJARPATH = /Applications/Micro-Manager1.4/plugins/Micro-Manager/MMCoreJ.jar
MIGJARPATH = /Applications/Micro-Manager1.4/plugins/Micro-Manager/miglayout-4.0-swing.jar
PROJECTORJARPATH = /Applications/Micro-Manager1.4/mmplugins/Device_Control/Projector.jar

.SUFFIXES: .java .class
../MicroNuclei.jar:  \
	$(SOURCEFILES)
	test -d build || mkdir build
	rm -rf build/*
	$(JC) -source 1.6 -target 1.6 -sourcepath src -classpath $(IJJARPATH):$(MMJARPATH):$(MMCOREJARPATH):$(MIGJARPATH):$(PROJECTORJARPATH):. src/org/micromanager/micronuclei/*.java -d build
	## Work around bug in 'jar': jar cf MM_.jar -C bin/ bin/* does not work
	cd build && jar cf ../../MicroNuclei.jar * 

classes: $(CLASSES:.java)

default: ../MicroNuclei.jar

clean:
	$(RM) -r build/*
	$(RM) ../MicroNuclei.jar 
