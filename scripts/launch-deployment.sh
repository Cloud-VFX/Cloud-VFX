#!/bin/bash

source config.sh

# Step 1: create VM image (AIM).

source $DIR/create-image.sh

# Step 2: create lambda function.

source $DIR/register-lambdas.sh

# Step 3: Create and start load balancer.

source $DIR/create-and-start-load-balancer.sh

# End of script.