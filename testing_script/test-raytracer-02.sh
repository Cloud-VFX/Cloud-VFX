#!/bin/bash

# CNV-23-24
# This script will issue in parallel one complex and one simple raytracer request.
# Modify it so it invokes your correct LB address and port in AWS, i.e., after http://
# If you need to change other request parameters to increase or decrease request complexity feel free to do so, provided they remain requests of different complexity.
# Added functionality to log request times.

function simple {
    local req_id=$1
    local start_time=$(date +%s%N) # Capture nanosecond timestamp
    echo "started raytracer simple ${req_id}"
    # Add scene.txt raw content to JSON.
    cat ./input/test01.txt | jq -sR '{scene: .}' > payload_simple_${req_id}.json                                                                          
    # Send the request.
    curl -s -X POST http://ec2-13-60-85-58.eu-north-1.compute.amazonaws.com:8080/raytracer?scols=800\&srows=600\&wcols=800\&wrows=600\&coff=0\&roff=0\&aa=false --data @"./payload_simple_${req_id}.json" > result_simple_raytracer_${req_id}.txt   
    # Remove a formatting string (remove everything before the comma).
    sed -i 's/^[^,]*,//' result_simple_raytracer_${req_id}.txt                                                                                             
    base64 -d result_simple_raytracer_${req_id}.txt > ./output/result_simple_raytracer_${req_id}.bmp
    # rm payload_simple_${req_id}.json result_simple_raytracer_${req_id}.txt 

    echo "finished raytracer simple ${req_id}"
    local end_time=$(date +%s%N) # Capture nanosecond timestamp
    local duration=$(echo "scale=3; ($end_time - $start_time) / 1000000000" | bc) # Calculate duration in seconds
    echo "$req_id, raytracer_simple, $start_time, $end_time, $duration" >> log.txt # Log details
}

function complex {
    local req_id=$1
    local start_time=$(date +%s%N) # Capture nanosecond timestamp
    echo "started raytracer complex ${req_id}"
    # Add scene.txt raw content to JSON.
    cat ./input/wood.txt | jq -sR '{scene: .}' > payload_complex_${req_id}.json                                                                          
    # Add texmap.bmp binary to JSON (optional step, required only for some scenes).
    hexdump -ve '1/1 "%u\n"' ./input/calcada.jpeg | jq -s --argjson original "$(<payload_complex_${req_id}.json)" '$original * {texmap: .}' > payload_complex_${req_id}.json  
    # Send the request.
    curl -s -X POST http://ec2-13-60-85-58.eu-north-1.compute.amazonaws.com:8080/raytracer?scols=800\&srows=600\&wcols=800\&wrows=600\&coff=0\&roff=0\&aa=false --data @"./payload_complex_${req_id}.json" > result_complex_raytracer_${req_id}.txt   
    # Remove a formatting string (remove everything before the comma).
    sed -i 's/^[^,]*,//' result_complex_raytracer_${req_id}.txt                                                                                             
    base64 -d result_complex_raytracer_${req_id}.txt > ./output/result_complex_raytracer_${req_id}.bmp

    # rm payload_complex_${req_id}.json result_complex_raytracer_${req_id}.txt 
    echo "finished raytracer complex ${req_id}"
    local end_time=$(date +%s%N) # Capture nanosecond timestamp
    local duration=$(echo "scale=3; ($end_time - $start_time) / 1000000000" | bc) # Calculate duration in seconds
    echo "$req_id, raytracer_complex, $start_time, $end_time, $duration" >> log.txt # Log details
}

req_id=0
while [ $req_id -lt 100 ]; do
    ((req_id++))
    complex $req_id &
    ((req_id++))
    simple $req_id &
    ((req_id++))
    complex $req_id &
    sleep 0.5
done
