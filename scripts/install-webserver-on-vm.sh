#!/bin/bash

source config.sh

sleep 2

source $DIR/install-project-on-vm.sh

# Create a startup script.
echo "Creating a startup script..."
startup_script='#!/bin/bash\ncd /home/ubuntu/\nmake webserver-javassist JAVASSIST_TOOL=GenerateMetrics'
echo -e $startup_script | ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ubuntu@$(cat instance.dns) "cat > /home/ubuntu/start-webserver.sh; chmod +x /home/ubuntu/start-webserver.sh"
echo "Startup script created."

# Setup systemd service for the web server to start on instance launch.
echo "Setting up systemd service for the web server..."
systemd_service="[Unit]\nDescription=Java Web Server\n\n[Service]\nType=simple\nExecStart=/bin/bash /home/ubuntu/start-webserver.sh\n\n[Install]\nWantedBy=multi-user.target"
echo -e $systemd_service | ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ubuntu@$(cat instance.dns) "sudo tee /etc/systemd/system/webserver.service > /dev/null"

# Enable and start the service.
cmd="sudo systemctl enable webserver.service; sudo systemctl start webserver.service"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ubuntu@$(cat instance.dns) $cmd
echo "Systemd service for the web server enabled and started."