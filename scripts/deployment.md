## Deployment flow

- Create the webserver image

  - launch one vm
  - install the webserver configurations into it
  - reboot and check if the webserver is running
  - create an image from this vm
  - terminate the vm

- Launch lambdas

  - create a lambda function for each of the services

- Launch the vm for Load Balancer
  - launch one vm
  - install the load balancer configurations into it
  - reboot and check if the load balancer is running
  - create an image from this vm
  - terminate the vm
