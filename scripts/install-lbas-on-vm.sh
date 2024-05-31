#!/bin/bash

source config.sh

sleep 2

source $DIR/install-project-on-vm.sh

# Create a startup script.
echo "Creating a startup script..."
startup_script='#!/bin/bash\ncd /home/ubuntu/\nmake loadbalancerautoscaler'
echo -e $startup_script | ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ubuntu@$(cat instance.dns) "cat > /home/ubuntu/start-lbas.sh; chmod +x /home/ubuntu/start-lbas.sh"
echo "Startup script created."

# Setup systemd service for the lbas to start on instance launch.
echo "Setting up systemd service for the lbas..."
systemd_service="[Unit]\nDescription=Java LoadBalancer and AS\n\n[Service]\nType=simple\nExecStart=/bin/bash /home/ubuntu/start-lbas.sh\n\n[Install]\nWantedBy=multi-user.target"
echo -e $systemd_service | ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ubuntu@$(cat instance.dns) "sudo tee /etc/systemd/system/lbas.service > /dev/null"

# Enable and start the service.
cmd="sudo systemctl enable lbas.service; sudo systemctl start lbas.service"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ubuntu@$(cat instance.dns) $cmd
echo "Systemd service for the lbas enabled and started."