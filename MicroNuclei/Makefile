JFLAGS = -g
JC = javac
BUILD = ../build
SOURCEFILES = src/org/micromanager/micronuclei/MicroNucleiPlugin.java \
				 src/org/micromanager/micronuclei/MicroNucleiForm.java \
				 src/org/micromanager/micronuclei/Terms.java \
				 src/org/micromanager/micronuclei/analysis/EdgeDetectorSubModule.java \
				 src/org/micromanager/micronuclei/analysis/GreenCellsModule.java \
				 src/org/micromanager/micronuclei/analysis/JustNucleiModule.java \
				 src/org/micromanager/micronuclei/analysis/MicroNucleiAnalysisModule.java \
				 src/org/micromanager/micronuclei/analysis/NuclearSizeModule.java \
				 src/org/micromanager/micronuclei/analysisinterface/AnalysisException.java \
				 src/org/micromanager/micronuclei/analysisinterface/AnalysisModule.java \
				 src/org/micromanager/micronuclei/analysisinterface/AnalysisProperty.java \
				 src/org/micromanager/micronuclei/analysisinterface/PropertyException.java \
				 src/org/micromanager/micronuclei/analysisinterface/ResultRois.java \
				 src/org/micromanager/micronuclei/internal/data/ChannelInfo.java \
				 src/org/micromanager/micronuclei/internal/gui/ResultsListener.java \
				 src/org/micromanager/micronuclei/internal/gui/DragDropUtil.java 
MMPATH = ../../micromanager2Viewer3D
MMARTIFACTS = $(MMPATH)/dependencies/artifacts
IJJARPATH = $(MMARTIFACTS)/imagej/ij-1.51s.jar
MMJARPATH = $(MMPATH)/mmstudio/MMJ_.jar
SCIJAVAJARPATH = $(MMARTIFACTS)/compile/scijava-common-2.66.0.jar
MMCOREJARPATH = $(MMPATH)/MMCoreJ_wrap/MMCoreJ.jar
MIGJARPATH = $(MMARTIFACTS)/compile/miglayout-swing-4.2.jar
MIGCOREJARPATH = $(MMARTIFACTS)/compile/miglayout-core-4.2.jar
GUAVAJARPATH = $(MMARTIFACTS)/compile/guava-17.0.jar
PROJECTORJARPATH = $(MMPATH)/libraries/Projector.jar

.SUFFIXES: .java .class
../MicroNuclei.jar:  $(SOURCEFILES)
	test -d build || mkdir build
	rm -rf build/org
	$(JC) -source 1.8 -target 1.8 -sourcepath src -classpath $(IJJARPATH):$(MMJARPATH):$(MMCOREJARPATH):$(MIGCOREJARPATH):$(MIGJARPATH):$(GUAVAJARPATH):$(PROJECTORJARPATH):$(SCIJAVAJARPATH):. src/org/micromanager/micronuclei/MicroNucleiPlugin.java -d build
	## Work around bug in 'jar': jar cf MM_.jar -C bin/ bin/* does not work
	cd build && jar cf ../../MicroNuclei.jar * 

classes: $(CLASSES:.java)

default: ../MicroNuclei.jar

clean:
	$(RM) -r build/org
	$(RM) ../MicroNuclei.jar 
