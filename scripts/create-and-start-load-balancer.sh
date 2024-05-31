#!/bin/bash

source config.sh

# Step 1: Launch a plain Ubuntu VM instance.

source $DIR/launch-plain-ubuntu-vm.sh

# Step 2: Install load-balancer in the VM instance.

source $DIR/install-lbas-on-vm.sh

# Step 3: Test VM instance.

source $DIR/test-vm.sh 8080

# Done!
