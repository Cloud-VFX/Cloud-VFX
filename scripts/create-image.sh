#!/bin/bash

source config.sh

# Step 1: launch a vm instance.
$DIR/launch-plain-ubuntu-vm.sh

# Step 2: install software in the VM instance.
$DIR/install-webserver-on-vm.sh

# Step 3: test VM instance.
$DIR/test-vm.sh 8000

# Step 4: create VM image (AIM).
aws ec2 create-image --instance-id $(cat instance.id) --name CNV-Image-Javassist2 | jq -r .ImageId > image.id
echo "New VM image with id $(cat image.id)."

# Step 5: Wait for image to become available.
echo "Waiting for image to be ready... (this can take a couple of minutes)"
aws ec2 wait image-available --filters Name=name,Values=CNV-Image-Javassist2
echo "Waiting for image to be ready... done! \o/"

# Step 6: terminate the vm instance.
aws ec2 terminate-instances --instance-ids $(cat instance.id) > /dev/null

# Step 7: Add image.id to the .env file.
# Check with sed if the flag AWS_AMI_ID is already set.
if grep -q "AWS_AMI_ID" $DIR/../.env; then
    # If it is, replace the value using a temporary file.
    sed "s/AWS_AMI_ID=.*/AWS_AMI_ID=$(cat image.id)/" $DIR/../.env > $DIR/../.env.tmp && mv $DIR/../.env.tmp $DIR/../.env
else
    # If it isn't, add the line.
    echo "AWS_AMI_ID=$(cat image.id)" >> $DIR/../.env
fi

