#!/bin/bash

source config.sh

# Degegister lambdas.

source $DIR/deregister-lambdas.sh

# Terminate all instances.
aws ec2 terminate-instances --instance-ids $(aws ec2 describe-instances --filters  "Name=instance-state-name,Values=pending,running,stopped,stopping" --query "Reservations[].Instances[].[InstanceId]" --output text | tr '\n' ' ')

# Deregister image
aws ec2 deregister-image --image-id $(cat image.id)
