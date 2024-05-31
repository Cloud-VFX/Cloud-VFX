#!/bin/bash

source config.sh

# Receive a port number as an argument.
port=$1

# Requesting an instance reboot.
aws ec2 reboot-instances --instance-ids $(cat instance.id)
echo "Rebooting instance to test autostart."

# Letting the instance shutdown.
sleep 1

# Wait for port $port to become available.
while ! nc -z $(cat instance.dns) $port; do
	echo "Waiting for $(cat instance.dns):$port..."
	sleep 0.5
done

echo "Waiting... for the service to fully start."
sleep 10

# Sending a query!
echo "Sending a query!"
curl $(cat instance.dns):$port/
