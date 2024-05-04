from data_preparation import DataPreparation
from request_manager import RequestManager
from result_processor import ResultProcessor
from file_manager import FileManager
import os
import random
import uuid
import logging
import time


def setup_logger(session_path):
    """Setup and return a logger for the session."""
    logger = logging.getLogger('WebServerTester')
    logger.setLevel(logging.INFO)
    file_handler = logging.FileHandler(os.path.join(session_path,
                                                    'session.log'))
    formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
    file_handler.setFormatter(formatter)
    logger.addHandler(file_handler)
    return logger


def create_session_folder(base_path):
    """Create a new session folder identified by a UUID."""
    session_id = str(uuid.uuid4())
    session_path = os.path.join(base_path, session_id)
    os.makedirs(session_path, exist_ok=True)
    return session_path


def main(image_dir,
         scene_dir,
         base_output_path,
         image_urls,
         raytracer_url,
         count=None):
    session_output_path = create_session_folder(base_output_path)
    logger = setup_logger(session_output_path)

    image_files = FileManager.list_files(image_dir, count)
    scene_files = FileManager.list_files(scene_dir, count)
    logger.info(
        f"Processing {len(image_files)} images and {len(scene_files)} scenes.")

    image_tasks = [(DataPreparation.encode_image_to_base64(file), url, file)
                   for file in image_files
                   for url in image_urls]
    scene_tasks = [(DataPreparation.prepare_json_payload(file), raytracer_url,
                    file) for file in scene_files]

    all_tasks = image_tasks
    random.shuffle(all_tasks)

    logger.info("Starting concurrent requests...")
    mixed_results = RequestManager.concurrent_requests(all_tasks)
    logger.info("Requests completed.")

    for (result, duration, error), (_, url,
                                    file_path) in zip(mixed_results, all_tasks):
        if error is None:
            file_name = os.path.splitext(os.path.basename(file_path))[0]
            url_endpoint = url.split("/")[-1]
            output_filename = f"{file_name}_{url_endpoint}.jpg" if url in image_urls else f"{file_name}_{url_endpoint}.bmp"
            output_path = os.path.join(session_output_path, output_filename)
            ResultProcessor.decode_base64_to_image(result, output_path)
            logger.info(
                f"Processed and saved {output_filename} in {duration*1000:.2f} ms"
            )
        else:
            logger.error(f"Failed processing {file_path} due to {error}")


if __name__ == "__main__":
    image_dir = "resources/imageproc"
    scene_dir = "resources/raytracer"
    base_output_path = "output_sessions"
    image_urls = [
        "http://127.0.0.1:8000/blurimage", "http://127.0.0.1:8000/enhanceimage"
    ]
    raytracer_url = "http://127.0.0.1:8000/raytracer"
    main(image_dir,
         scene_dir,
         base_output_path,
         image_urls,
         raytracer_url,
         count=None)
