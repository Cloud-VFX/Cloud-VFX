#!/bin/bash

source config.sh

sleep 2

# Install Java, Maven, and other dependencies.
echo "Installing Java, Maven, and other dependencies..."
cmd="sudo apt update && sudo apt install openjdk-21-jdk make maven unzip -y"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ubuntu@$(cat instance.dns) $cmd
echo "Java, Maven, and other dependencies installed."

# Zip the project.
echo "Zipping the project..."
cd "$DIR"/../
zip -r $DIR/project.zip imageproc raytracer JavassistWrapper webserver metrics LoadBalancerAutoScaler Makefile pom.xml .env
cd $DIR
echo "Project zipped."

# Transfer the project to the server.
echo "Transferring the project to the server..."
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH $DIR/project.zip ubuntu@$(cat instance.dns):
echo "Project transferred."

# Unzip the project and prepare the environment on the server.
echo "Unzipping the project and preparing the environment on the server..."
cmd="unzip project.zip -d /home/ubuntu/; cd /home/ubuntu/; make build"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ubuntu@$(cat instance.dns) $cmd 
echo "Project unzipped and environment prepared."
