#!/bin/bash

source config.sh

echo "Deleting lambda functions..."
echo "Deleting EnhanceImageHandler lambda function..."
aws lambda delete-function --function-name EnhanceImageHandler

echo "Deleting BlurImageHandler lambda function..."
aws lambda delete-function --function-name BlurImageHandler

echo "Deleting RaytracerHandler lambda function..."
aws lambda delete-function --function-name RaytracerHandler

aws iam detach-role-policy --role-name lambda-basic-permissions --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

echo "Deleting lambda role..."
aws iam delete-role --role-name lambda-basic-permissions