import requests
from concurrent.futures import ThreadPoolExecutor
import time
import logging


class RequestManager:

    @staticmethod
    def send_request(data, url, file_path, query_params=None):
        """Send a request to the specified URL and measure the time taken with high precision, with error handling."""
        start = time.perf_counter()
        headers = {'Content-Type': 'application/json'}
        try:
            response = requests.post(url,
                                     data=data,
                                     headers=headers,
                                     params=query_params)
            duration = time.perf_counter() - start
            return response.content, duration, None
        except Exception as e:
            duration = time.perf_counter() - start
            logger = logging.getLogger('WebServerTester')
            logger.error(f"Request failed for {file_path} to {url}: {str(e)}")
            return None, duration, e

    @staticmethod
    def concurrent_requests(tasks):
        """Handle concurrent requests to multiple endpoints, returning results, timings, and errors."""
        with ThreadPoolExecutor(max_workers=len(tasks)) as executor:
            results = executor.map(
                lambda task: RequestManager.send_request(*task), tasks)
        return list(results)
