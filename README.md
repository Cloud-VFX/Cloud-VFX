## SpecialVFX@Cloud

This project contains three sub-projects:

1. `raytracer` - the Ray Tracing workload
2. `imageproc` - the BlurImage and EnhanceImage workloads
3. `webserver` - the web server exposing the functionality of the workloads

Refer to the `README.md` files of the sub-projects to get more details about each specific sub-project.

### Dependencies

This is a list of dependencies required to build and run the project:

- Java 11+ is required to run the project.
- Maven is required to build the project.
- Make is required to use the provided Makefile.

### How to build everything

- 1. Make sure your `JAVA_HOME` environment variable is set to Java 11+ distribution
- 2. Run `make build` or `mvn clean package`
- 3. Start the webserver by running `make webserver`
  - 3.1 If you want to run the webserver and instrument it with javassist use:
    `make webserver-javaassist JAVASSIST_TOOL=(name_of_the_tool)`
- 4. The webserver will be available at `http://localhost:8080`

### How to start the EC2 instances

- 1. Make sure you have the AWS CLI installed and configured
- 2. Enter the `scripts` directory
- 3. Run `./launch-vm.sh`
- 4. Run `./install-vm.sh`
- 5. Run `./test-vm.sh`

Then, a EC2 instance will be started and the webserver will be available at
`http://<public_ip>:8080`. Where `<public_ip>` can be found on `scripts/instance.dns` file.

### Setup LB and AS

...
