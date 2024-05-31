#!/bin/bash

# CNV-23-24
# This script will issue in parallel on complex and one simple raytracer request.
# Modify it so it invokes your correct LB address and port in AWS, i.e., after http://
# If you need to change other request parameters to increase or decrease request complexity feel free to do so, provided they remain requests of different complexity.

function simple {
    local run_id=$1
    echo "started raytracer simple ${run_id}"
    # Add scene.txt raw content to JSON.
    cat ./input/test01.txt | jq -sR '{scene: .}' > payload_simple_${run_id}.json                                                                          
    # Send the request.
    curl -s -X POST http://192.168.56.3:8080/raytracer?scols=400\&srows=300\&wcols=400\&wrows=300\&coff=0\&roff=0\&aa=false --data @"./payload_simple_${run_id}.json" > result_simple_raytracer_${run_id}.txt   
    # Remove a formatting string (remove everything before the comma).
    sed -i 's/^[^,]*,//' result_simple_raytracer_${run_id}.txt                                                                                             
    base64 -d result_simple_raytracer_${run_id}.txt > ./output/result_simple_raytracer_${run_id}.bmp
    # rm payload_simple_${run_id}.json result_simple_raytracer_${run_id}.txt 

    echo "finished raytracer simple ${run_id}"
}

function complex {
    local run_id=$1
    echo "started raytrace complex ${run_id}"
    # Add scene.txt raw content to JSON.
    cat ./input/test05.txt | jq -sR '{scene: .}' > payload_complex_${run_id}.json                                                                          
    # Add texmap.bmp binary to JSON (optional step, required only for some scenes).
    hexdump -ve '1/1 "%u\n"' ./input/calcada.jpeg | jq -s --argjson original "$(<payload_complex_${run_id}.json)" '$original * {texmap: .}' > payload_complex_${run_id}.json  
    # Send the request.
    curl -s -X POST http://192.168.56.3:8080/raytracer?scols=2400\&srows=1800\&wcols=400\&wrows=300\&coff=0\&roff=0\&aa=false --data @"./payload_complex_${run_id}.json" > result_complex_raytracer_${run_id}.txt   
    # Remove a formatting string (remove everything before the comma).
    sed -i 's/^[^,]*,//' result_complex_raytracer_${run_id}.txt                                                                                             
    base64 -d result_complex_raytracer_${run_id}.txt > ./output/result_complex_raytracer_${run_id}.bmp

    # rm payload_complex_${run_id}.json result_complex_raytracer_${run_id}.txt 
    echo "finished raytracer complex ${run_id}"
}

# simple $run_id &
# complex $run_id &
run_id=0
while [ $run_id -lt 200 ]; do
    ((run_id++))
    complex $run_id &
    simple $run_id &
    sleep 0.5
done