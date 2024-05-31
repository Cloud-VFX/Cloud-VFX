#!/bin/bash

source config.sh

cd ..

make build

cd scripts

aws iam create-role \
	--role-name lambda-basic-permissions \
	--assume-role-policy-document '{"Version": "2012-10-17","Statement": [{ "Effect": "Allow", "Principal": {"Service": "lambda.amazonaws.com"}, "Action": "sts:AssumeRole"}]}' > /dev/null

aws iam attach-role-policy \
	--role-name lambda-basic-permissions \
	--policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole > /dev/null

sleep 5 # wait for role to be created

echo "Creating EnhanceImageHandler lambda function..."

aws lambda create-function \
    --function-name EnhanceImageHandler \
    --zip-file fileb://../imageproc/target/imageproc-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
    --handler pt.ulisboa.tecnico.cnv.imageproc.EnhanceImageHandler::handleRequest \
    --runtime java11 \
    --timeout 15 \
	--memory-size 256 \
    --role arn:aws:iam::${AWS_ACCOUNT_ID}:role/lambda-basic-permissions > /dev/null

echo "EnhanceImageHandler lambda function created."

echo "Creating BlurImageHandler lambda function..."

aws lambda create-function \
    --function-name BlurImageHandler \
    --zip-file fileb://../imageproc/target/imageproc-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
    --handler pt.ulisboa.tecnico.cnv.imageproc.BlurImageHandler::handleRequest \
    --runtime java11 \
    --timeout 15 \
    --memory-size 256 \
    --role arn:aws:iam::${AWS_ACCOUNT_ID}:role/lambda-basic-permissions > /dev/null

echo "BlurImageHandler lambda function created."

echo "Creating RaytracerHandler lambda function..."

aws lambda create-function \
    --function-name RaytracerHandler \
    --zip-file fileb://../raytracer/target/raytracer-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
    --handler pt.ulisboa.tecnico.cnv.raytracer.RaytracerHandler::handleRequest \
    --runtime java11 \
    --timeout 15 \
    --memory-size 256 \
    --role arn:aws:iam::${AWS_ACCOUNT_ID}:role/lambda-basic-permissions > /dev/null

echo "RaytracerHandler lambda function created."

echo "All lambda functions created."