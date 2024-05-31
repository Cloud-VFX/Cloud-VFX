## SpecialVFX@Cloud - G02

This project contains five sub-projects:

1. `raytracer` - the Ray Tracing workload
2. `imageproc` - the BlurImage and EnhanceImage workloads
3. `webserver` - the web server exposing the functionality of the workloads
4. `JavassistWrapper` - the instrumentation class for getting execution metrics
5. `metrics` - the metrics storage system
6. `LoadBalancerAutoScale` - the Load Balancer and Auto Scaler system
7. `webserver_tester` - a python script to aid generate parallel load to the Load Balancer

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
- 4. The webserver will be available at `http://localhost:8000`
- 5. To run the `loadbalancer` and `autoscaler` run `make loadbalancerautoscaler`
- 6. The Load Balancer will be available at `http://localhost:8080`

### Launch deployment procedure

- 1. Make sure you have the AWS CLI installed and configured
- 2. Enter the `scripts` directory
- 3. Configure the .env file with the aws parameters
  - Leave the `IAM_ROLE_NAME` and `AWS_AMI_ID` as they are
- 4. Run `source launch-deployment.sh`

Then, a webserver vm will be launched and an image will be created from it. After that, the lambdas functions are going to be registered and finally the Load Balancer and Auto Scaler will be started.

### Terminate deployment procedure

To terminate the deployment, follow the steps below:

- 1. Make sure you have the AWS CLI installed and configured
- 2. Enter the `scripts` directory
- 3. Run `./terminate-deployment.sh`

### Details of implementation

If you want to know more about the implementation of the project, please refer to the `report` in the git repository,
where a detailed explanation of the project will be available.
