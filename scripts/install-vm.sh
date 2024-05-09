#!/bin/bash

source config.sh

# Install Java, Maven, and other dependencies.
echo "Installing Java, Maven, and other dependencies..."
cmd="sudo apt update && sudo apt install openjdk-11-jdk make maven -y"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd
echo "Java, Maven, and other dependencies installed."

# Zip the project.
echo "Zipping the project..."
cd "$DIR"/../
zip -r $DIR/project.zip imageproc raytracer webserver Makefile pom.xml
cd $DIR
echo "Project zipped."

# Transfer the project to the server.
echo "Transferring the project to the server..."
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH $DIR/project.zip ec2-user@$(cat instance.dns):
echo "Project transferred."

# Unzip the project and prepare the environment on the server.
echo "Unzipping the project and preparing the environment on the server..."
cmd="unzip project.zip -d /home/ec2-user/; cd /home/ec2-user/; make build"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd 
echo "Project unzipped and environment prepared."

# Create a startup script.
echo "Creating a startup script..."
startup_script='#!/bin/bash\ncd /home/ec2-user/\nmake webserver'
echo -e $startup_script | ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) "cat > /home/ec2-user/start-webserver.sh; chmod +x /home/ec2-user/start-webserver.sh"
echo "Startup script created."

# Setup systemd service for the web server to start on instance launch.
echo "Setting up systemd service for the web server..."
systemd_service="[Unit]\nDescription=Java Web Server\n\n[Service]\nType=simple\nExecStart=/bin/bash /home/ec2-user/start-webserver.sh\n\n[Install]\nWantedBy=multi-user.target"
echo -e $systemd_service | ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) "sudo tee /etc/systemd/system/webserver.service > /dev/null"

# Enable and start the service.
cmd="sudo systemctl enable webserver.service; sudo systemctl start webserver.service"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd
echo "Systemd service for the web server enabled and started."