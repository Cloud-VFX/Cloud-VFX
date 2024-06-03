#!/bin/bash

# CNV-23-24
# This script will issue in parallel one complex and one simple imageproc request.
# Modify it so it invokes your correct LB address and port in AWS, i.e., after http://
# If you need to change other request parameters to increase or decrease request complexity feel free to do so, provided they remain requests of different complexity.
# Added functionality to log request times.

function simple {
    local req_id=$1
    local start_time=$(date +%s%N) # Capture nanosecond timestamp
    echo "started imageproc simple ${req_id}"
    # Encode in Base64.
    base64 ./input/airplane.jpg | tr -d '\n' > temp_simple_imageproc_${req_id}.txt                                            

    # Append a formatting string.
    echo -e "data:image/jpg;base64,$(cat temp_simple_imageproc_${req_id}.txt)" > temp_simple_imageproc_${req_id}.txt               

    # Send the request.
    curl -s -X POST http://ec2-16-171-249-162.eu-north-1.compute.amazonaws.com:8080/blurimage --data @"./temp_simple_imageproc_${req_id}.txt" > result_simple_imageproc_${req_id}.txt   
    
    # Remove a formatting string (remove everything before the comma).
    sed -i 's/^[^,]*,//' result_simple_imageproc_${req_id}.txt                                          

    # Decode from Base64.
    base64 -d result_simple_imageproc_${req_id}.txt > ./output/result_simple_imageproc_${req_id}.jpg                                 
    
    # Remove temp files
    # rm result_simple_imageproc_${req_id}.txt temp_simple_imageproc_${req_id}.txt

    echo "finished imageproc simple ${req_id}"
    local end_time=$(date +%s%N) # Capture nanosecond timestamp
    # local duration=$(echo "scale=3; ($end_time - $start_time) / 1000000000" | bc) # Calculate duration in seconds
    echo "$req_id, blur_simple, $start_time, $end_time, $duration" >> log.txt # Log details
}

function complex {
    local req_id=$1
    local start_time=$(date +%s%N) # Capture nanosecond timestamp
    echo "started imageproc complex ${req_id}"
    # Encode in Base64.
    base64 ./input/horse.jpg > temp_complex_imageproc_${req_id}.txt                                            

    # Append a formatting string.
    echo -e "data:image/jpg;base64,$(cat temp_complex_imageproc_${req_id}.txt)" > temp_complex_imageproc_${req_id}.txt               

    # Send the request.
    curl -s -X POST http://ec2-16-171-249-162.eu-north-1.compute.amazonaws.com:8080/enhanceimage --data @"./temp_complex_imageproc_${req_id}.txt" > result_complex_imageproc_${req_id}.txt   
    
    # Remove a formatting string (remove everything before the comma).
    sed -i 's/^[^,]*,//' result_complex_imageproc_${req_id}.txt                                          

    # Decode from Base64.
    base64 -d result_complex_imageproc_${req_id}.txt > ./output/result_complex_imageproc_${req_id}.jpg

    # Remove temp files
    # rm temp_complex_imageproc_${req_id}.txt result_complex_imageproc_${req_id}.txt
    
    echo "finished imageproc complex ${req_id}"
    local end_time=$(date +%s%N) # Capture nanosecond timestamp
    local duration=$(echo "scale=3; ($end_time - $start_time) / 1000000000" | bc) # Calculate duration in seconds
    echo "$req_id, enhance_complex, $start_time, $end_time, $duration" >> log.txt # Log details
}

req_id=0

while [ $req_id -lt 100 ]; do
    ((req_id++))
    complex $req_id &
    ((req_id++))
    simple $req_id &
    ((req_id++))
    simple $req_id &
    ((req_id++))
    complex $req_id &
    ((req_id++))
    simple $req_id &
    sleep 1
done
