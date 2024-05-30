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

webserver-javassist:
	@echo "Building and running Web Server with Javassist..."
	$(MAKE) run-javassist PROJECT=webserver CLASS=pt.ulisboa.tecnico.cnv.webserver.WebServer JAVASSIST_TOOL=$(JAVASSIST_TOOL) ARGS=""

# Build target
build:
	@echo "Building ..."
	$(MVN) clean package

# Run target
run:
	@echo "Running $(PROJECT)..."
	$(JAVA) -cp $(PROJECT)/target/$(PROJECT)-1.0.0-SNAPSHOT-jar-with-dependencies.jar $(CLASS) $(INPUT_FILE) $(OUTPUT_FILE) $(ARGS)

run-javassist:
	@echo "Running $(PROJECT)... with Javassist"
	$(JAVA) -cp $(PROJECT)/target/$(PROJECT)-1.0.0-SNAPSHOT-jar-with-dependencies.jar -javaagent:JavassistWrapper/target/JavassistWrapper-1.0-jar-with-dependencies.jar=$(JAVASSIST_TOOL):pt.ulisboa.tecnico.cnv.webserver,pt.ulisboa.tecnico.cnv.imageproc,pt.ulisboa.tecnico.cnv.raytracer:output $(CLASS) $(INPUT_FILE) $(OUTPUT_FILE) $(ARGS)

input_file_with_extension := $(INPUT_FILE)
input_file_without_extension := $(basename $(input_file_with_extension))

raytracer-input:
	@echo "Generating Ray Tracer input..."
	cat $(INPUT_FILE) | jq -sR '{"scene": .}' > $(input_file_without_extension).json
	# Check if $(textmap) is not empty
	if [ -n "$(textmap)" ]; then \
		hexdump -ve '1/1 "%u\n"' $(textmap) | jq -s --argjson original "$$(<$(input_file_without_extension).json)" '$$original + {texmap: .}' > $(input_file_without_extension)_temp.json; \
		mv $(input_file_without_extension)_temp.json $(input_file_without_extension).json; \
	fi
	curl -X POST http://127.0.0.1:8000/raytracer?scols=400\&srows=300\&wcols=400\&wrows=300\&coff=0\&roff=0\&aa=false --data @"./$(input_file_without_extension).json" > $(input_file_without_extension)_out.txt
	sed -i '' 's/^[^,]*,//'  $(input_file_without_extension)_out.txt
	base64 -D -i $(input_file_without_extension)_out.txt > $(input_file_without_extension)_out.bmp
	rm $(input_file_without_extension).json
	rm $(input_file_without_extension)_out.txt
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
