import json
import requests
import base64
import threading
from time import sleep, time
from datetime import datetime

def request_function(mode, task, run_id, results):
    start_time = time()
    
    # Configuration for each mode and task
    config = {
        'imageproc': {
            'simple': {'file': 'airplane.jpg', 'url': 'http://ec2-13-53-200-114.eu-north-1.compute.amazonaws.com:8080/blurimage'},
            'complex': {'file': 'horse.jpg', 'url': 'http://ec2-13-53-200-114.eu-north-1.compute.amazonaws.com:8080/enhanceimage'}
        },
        'raytracer': {
            'simple': {'file': 'test01.txt', 'url': 'http://ec2-13-53-200-114.eu-north-1.compute.amazonaws.com:8080/raytracer', 'params': {'scols': 400, 'srows': 300, 'wcols': 400, 'wrows': 300, 'coff': 0, 'roff': 0, 'aa': 'false'}},
            'complex': {'file': 'test01.txt', 'url': 'http://ec2-13-53-200-114.eu-north-1.compute.amazonaws.com:8080/raytracer', 'params': {'scols': 400, 'srows': 300, 'wcols': 400, 'wrows': 300, 'coff': 0, 'roff': 0, 'aa': 'false'}}
        }
    }

    cfg = config[task][mode]
    result_file_name = f"result_{task}_{mode}_{run_id}.{'bmp' if task == 'raytracer' else 'jpg'}"

    if task == 'raytracer':
        with open(cfg['file'], "r") as file:
            data = {"scene": file.read()}
        if mode == "complex":
            with open("calcada.jpeg", "rb") as image_file:
                data["texmap"] = image_file.read().hex()
        payload = json.dumps(data)
    else:
        with open(cfg['file'], "rb") as image_file:
            payload = f"data:image/jpg;base64,{base64.b64encode(image_file.read()).decode()}"

    response = requests.post(cfg['url'], params=cfg.get('params', {}), data=payload)
    end_time = time()
    
    # Store result data
    request_data = {
        'request_id': run_id,
        'request_type': f"{task}_{mode}",
        'timestamp_start': datetime.fromtimestamp(start_time).isoformat(),
        'timestamp_end': datetime.fromtimestamp(end_time).isoformat(),
        'length_request': end_time - start_time
    }
    results.append(request_data)

    response_data = response.text.split(',', 1)[-1]
    with open(result_file_name, "wb") as result_file:
        result_file.write(base64.b64decode(response_data))

def run_cycle(run_id, results):
    threads = []
    tasks = [('simple', 'imageproc', 4), ('complex', 'imageproc', 3),
             ('simple', 'raytracer', 2), ('complex', 'raytracer', 1)]
    
    for mode, task, count in tasks:
        for i in range(count):
            thread = threading.Thread(target=request_function, args=(mode, task, f"{run_id}_{i}", results))
            threads.append(thread)
            thread.start()

    for thread in threads:
        thread.join()

    sleep(0.5)

def main():
    results = []
    for i in range(1):
        run_cycle(i, results)
    return results

if __name__ == "__main__":
    execution_results = main()
    # Example to see the results
    print(json.dumps(execution_results, indent=4))
