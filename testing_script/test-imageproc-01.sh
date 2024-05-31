#!/bin/bash

# CNV-23-24
# This script will issue in parallel on complex and one simple imageproc request.
# Modify it so it invokes your correct LB address and port in AWS, i.e., after http://
# If you need to change other request parameters to increase or decrease request complexity feel free to do so, provided they remain requests of different complexity.

function simple {
    local run_id=$1
    echo "started imageproc simple ${run_id}"
    # Encode in Base64.
    base64 ./input/airplane.jpg | tr -d '\n' > temp_simple_imageproc_${run_id}.txt                                            

    # Append a formatting string.
    echo -e "data:image/jpg;base64,$(cat temp_simple_imageproc_${run_id}.txt)" > temp_simple_imageproc_${run_id}.txt               

    # Send the request.
    curl -s -X POST http://192.168.56.3:8080/blurimage --data @"./temp_simple_imageproc_${run_id}.txt" > result_simple_imageproc_${run_id}.txt   
    
    # Remove a formatting string (remove everything before the comma).
    sed -i 's/^[^,]*,//' result_simple_imageproc_${run_id}.txt                                          

    # Decode from Base64.
    base64 -d result_simple_imageproc_${run_id}.txt > ./output/result_simple_imageproc_${run_id}.jpg                                 
    
    # Remove temp files
    # rm result_simple_imageproc_${run_id}.txt temp_simple_imageproc_${run_id}.txt

    echo "finished imageproc simple ${run_id}"
}

function complex {
    local run_id=$1
    echo "started imageproc complex ${run_id}"
    # Encode in Base64.
    base64 ./input/horse.jpg > temp_complex_imageproc_${run_id}.txt                                            

    # Append a formatting string.
    echo -e "data:image/jpg;base64,$(cat temp_complex_imageproc_${run_id}.txt)" > temp_complex_imageproc_${run_id}.txt               

    # Send the request.
    curl -s -X POST http://192.168.56.3:8080/enhanceimage --data @"./temp_complex_imageproc_${run_id}.txt" > result_complex_imageproc_${run_id}.txt   
    
    # Remove a formatting string (remove everything before the comma).
    sed -i 's/^[^,]*,//' result_complex_imageproc_${run_id}.txt                                          

    # Decode from Base64.
    base64 -d result_complex_imageproc_${run_id}.txt > ./output/result_complex_imageproc_${run_id}.jpg

    # Remove temp files
    # rm temp_complex_imageproc_${run_id}.txt result_complex_imageproc_${run_id}.txt
    
    echo "finished imageproc complex ${run_id}"
}

run_id=0

while [ $run_id -lt 2 ]; do
    ((run_id++))
    complex $run_id &
    simple $run_id &
    sleep 0.5
done