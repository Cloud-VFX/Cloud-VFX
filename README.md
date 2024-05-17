## SpecialVFX@Cloud - G02

This project contains five sub-projects:

1. `raytracer` - the Ray Tracing workload
2. `imageproc` - the BlurImage and EnhanceImage workloads
3. `webserver` - the web server exposing the functionality of the workloads
4. `JavassistWrapper` - the instrumentation class for getting execution metrics
5. `webserver_tester` - a python script to aid generate parallel load to the Load Balancer

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

### How to start a EC2 instance

- 1. Make sure you have the AWS CLI installed and configured
- 2. Enter the `scripts` directory
- 3. Run `./launch-vm.sh`
- 4. Run `./install-vm.sh`

Then, a EC2 instance will be started and the webserver will be available at
`http://<public_ip>:8080`. Where `<public_ip>` can be found on `scripts/instance.dns` file.

### How to create an image with autostart of the webserver
- 1. Run `./create-image.sh`
- 2. Run `./launch-deployment.sh`

Then, a Load Balancer (LB) and Auto Scaler group will be started and the webserver will be available at the public ip of the LB. 

To terminate the Auto Scaler and the Load Balancer:
- 3. Run `./terminate-deployment.sh`


### Future Metric Aggregation Logic:
#### Metric Aggregation

- To reduce the workload of the Load Balancer (LB) in estimating the complexity of incoming requests, a Lambda function will periodically aggregate recorded metrics. This function will run at intervals of period T (CRON job).
- The aggregation will create an easily queryable form for estimating request complexity, either as a function or an aggregated table.
- Note: For `BLUR` and `ENHANCE` request types, neither texture maps nor anti-aliasing are applicable.

#### Future Load Balancer logic

- When the webserver starts, start the load balancer service, connect to Metrics Storage System (MSS) and to CloudWatch. Initialize a variable to record the available instances, their current requests and the respective complexities.
- When a new request arrives, determine its type (BLUR, ENHANCE, RAYTRACE) and characteristics (# of pixels in image, if uses texture map, if uses anti-aliasing). Note that BLUR and ENHANCE never use neither texture map nor anti-aliasing.
- Given the request, query the MSS using request type and characteristics to get an estimated complexity value.
- Then, retrieve the list of currently available instances from the Auto Scaler. Note that the Auto Scaler will mask instances that are running but marked for termination. With the list, fetch usage for each instance (CPU and RAM) from CloudWatch.
- Between the instances, select the one with the lowest complexity score that does not exceed a 95% CPU usage threshold.
    - If no suitable instance is found, and the request is complex (RAYTRACE), trigger a Lambda function. This is to avoid bottlenecking the queue of simple requests.
    - If the request is less complex (BLUR or ENHANCE) and no suitable instance is found, add it to a queue.
    - After assigning the request, update an instance’s record with the new request and its complexity estimate.
- When a request completes, update the instance’s record to reflect the reduced load.
- In addition, to handle the queue, periodically check it for pending requests. Attempt to reassign queued requests when instances become available or when new instances are started by the auto-scaler.

#### Future Auto Scaler logic

- Start the auto-scaler service. Get an initial list of instances. Connect to CloudWatch to monitor instance usage.
- At every 5 seconds, retrieve the current usage for all instances from CloudWatch.
- If all instances have CPU usage bigger than 90%, launch a new instance. Add this instance to the list.
- If any instance has CPU usage lower than 30% for 2 minutes, mark the instance for termination. Periodically get from Load Balancer the number of requests running for this instance. When no request is running, terminate it.
