JFLAGS = -g
JC = javac
BUILD = ../build
SOURCEFILES = src/org/micromanager/micronuclei/MicroNucleiPlugin.java \
				 src/org/micromanager/micronuclei/MicroNucleiForm.java \
				 src/org/micromanager/micronuclei/Terms.java \
				 src/org/micromanager/micronuclei/analysis/GreenCellsModule.java \
				 src/org/micromanager/micronuclei/analysis/MicroNucleiAnalysisModule.java \
				 src/org/micromanager/micronuclei/analysis/JustNucleiModule.java \
				 src/org/micromanager/micronuclei/analysisinterface/AnalysisException.java \
				 src/org/micromanager/micronuclei/analysisinterface/AnalysisModule.java \
				 src/org/micromanager/micronuclei/analysisinterface/AnalysisProperty.java \
				 src/org/micromanager/micronuclei/analysisinterface/PropertyException.java \
				 src/org/micromanager/micronuclei/analysisinterface/ResultRois.java \
				 src/org/micromanager/micronuclei/internal/gui/ResultsListener.java \
				 src/org/micromanager/micronuclei/internal/gui/DragDropUtil.java 
IJJARPATH = /Applications/Micro-Manager2.0/ij.jar
MMJARPATH = /Applications/Micro-Manager2.0/plugins/Micro-Manager/MMJ_.jar
SCIJAVAJARPATH = /Applications/Micro-Manager2.0/plugins/Micro-Manager/scijava-common-2.34.0.jar
MMCOREJARPATH = /Applications/Micro-Manager2.0/plugins/Micro-Manager/MMCoreJ.jar
MIGJARPATH = /Applications/Micro-Manager2.0/plugins/Micro-Manager/miglayout-swing-4.2.jar
MIGCOREJARPATH = /Applications/Micro-Manager2.0/plugins/Micro-Manager/miglayout-core-4.2.jar
PROJECTORJARPATH = /Applications/Micro-Manager2.0/mmplugins/Projector.jar

.SUFFIXES: .java .class
../MicroNuclei.jar:  $(SOURCEFILES)
	test -d build || mkdir build
	rm -rf build/*
	$(JC) -source 1.6 -target 1.6 -sourcepath src -classpath $(IJJARPATH):$(MMJARPATH):$(SCIJAVAJARPATH):$(MMCOREJARPATH):$(MIGCOREJARPATH):$(MIGJARPATH):$(PROJECTORJARPATH):. src/org/micromanager/micronuclei/*.java -d build
	## Work around bug in 'jar': jar cf MM_.jar -C bin/ bin/* does not work
	cd build && jar cf ../../MicroNuclei.jar * 

classes: $(CLASSES:.java)

default: ../MicroNuclei.jar

clean:
	$(RM) -r build/*
	$(RM) ../MicroNuclei.jar 
