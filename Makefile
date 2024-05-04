# Java runtime
JAVA = java
MVN = mvn

# Default make target
.DEFAULT_GOAL := help

# Build and run each project
raytracer:
	@echo "Building and running Ray Tracer..."
	$(MAKE) run PROJECT=raytracer CLASS=pt.ulisboa.tecnico.cnv.raytracer.Main INPUT_FILE=$(INPUT_FILE) OUTPUT_FILE=$(OUTPUT_FILE) ARGS="400 300 400 300 0 0"

imageproc-blur:
	@echo "Building and running Image Processing (Blur)..."
	$(MAKE) run PROJECT=imageproc CLASS=pt.ulisboa.tecnico.cnv.imageproc.BlurImageHandler INPUT_FILE=$(INPUT_FILE) OUTPUT_FILE=$(OUTPUT_FILE) ARGS=""

imageproc-enhance:
	@echo "Building and running Image Processing (Enhance)..."
	$(MAKE) run PROJECT=imageproc CLASS=pt.ulisboa.tecnico.cnv.imageproc.EnhanceImageHandler INPUT_FILE=$(INPUT_FILE) OUTPUT_FILE=$(OUTPUT_FILE) ARGS=""

webserver:
	@echo "Building and running Web Server..."
	$(MAKE) run PROJECT=webserver CLASS=pt.ulisboa.tecnico.cnv.webserver.WebServer ARGS=""

# Build target
build:
	@echo "Building ..."
	$(MVN) clean package

# Run target
run:
	@echo "Running $(PROJECT)..."
	$(JAVA) -cp $(PROJECT)/target/$(PROJECT)-1.0.0-SNAPSHOT-jar-with-dependencies.jar $(CLASS) $(INPUT_FILE) $(OUTPUT_FILE) $(ARGS)

# Help target
help:
	@echo "Usage: make [target]"
	@echo ""
	@echo "Available targets:"
	@echo "  raytracer         : Build and run the Ray Tracer"
	@echo "  imageproc-blur    : Build and run Image Processing (Blur)"
	@echo "  imageproc-enhance : Build and run Image Processing (Enhance)"
	@echo "  webserver         : Build and run Web Server"
	@echo "  help              : Show this help message"

.PHONY: raytracer imageproc-blur imageproc-enhance webserver build run help
