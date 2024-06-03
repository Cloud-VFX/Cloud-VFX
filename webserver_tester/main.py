from data_preparation import DataPreparation
from request_manager import RequestManager
from result_processor import ResultProcessor
from file_manager import FileManager
from image_comparator import compare_images_in_directory
from colorama import Fore, init
import os
import random
import uuid
import logging
import Image

# Initialize Colorama
init(autoreset=True)

RAYTRACER_QUERY_PARAMS = {
    'scols': 400,
    'srows': 300,
    'wcols': 400,
    'wrows': 300,
    'coff': 0,
    'roff': 0,
    'aa': 'false'
}


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


def apply_random_multiplier_on_raytracer_params():
    """Apply a random multiplier on the raytracer parameters."""
    random_multiplier = random.uniform(0.5, 2.0)
    params_copy = RAYTRACER_QUERY_PARAMS.copy()
    params_copy['scols'] = int(params_copy['scols'] * random_multiplier)
    params_copy['srows'] = int(params_copy['srows'] * random_multiplier)
    params_copy['wcols'] = int(params_copy['wcols'] * random_multiplier)
    params_copy['wrows'] = int(params_copy['wrows'] * random_multiplier)

    return params_copy


def main(image_dir,
         scene_dir,
         truth_dir,
         base_output_path,
         image_urls,
         raytracer_url,
         count=None):
    session_output_path = create_session_folder(base_output_path)
    print(f"Starting session {os.path.basename(session_output_path)}...")
    logger = setup_logger(session_output_path)

    image_files = FileManager.list_files(image_dir, count)
    scene_files = FileManager.list_files(scene_dir, count)
    logger.info(
        f"Processing {len(image_files)} images and {len(scene_files)} scenes.")

    image_tasks = [(DataPreparation.encode_image_to_base64(file), url, file,
                    None) for file in image_files for url in image_urls]

    # Multiply the scene files to increase the number of requests
    scene_files = scene_files * 10
    scene_tasks = [(DataPreparation.prepare_json_payload(file), raytracer_url,
                    file, apply_random_multiplier_on_raytracer_params())
                   for file in scene_files]

    all_tasks = image_tasks + scene_tasks
    random.shuffle(all_tasks)

    logger.info("Starting concurrent requests...")
    mixed_results = RequestManager.concurrent_requests(all_tasks)
    logger.info("Requests completed.")

    for (result, duration, error), (_, url, file_path,
                                    _) in zip(mixed_results, all_tasks):
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

    # Comparing test images with truth images with tqdm and color output
    compare_results = compare_images_in_directory(session_output_path,
                                                  truth_dir)
    for file in compare_results:
        are_equal = compare_results[file]
        if are_equal:
            print(Fore.GREEN + f"✓ {file}: Test image matches truth image.")
            logger.info(f"{file}: Test image matches truth image.")
        else:
            print(Fore.RED +
                  f"✕ {file}: Test image does not match truth image.")
            logger.info(f"{file}: Test image does not match truth image.")


if __name__ == "__main__":
    image_dir = "resources/imageproc"
    scene_dir = "resources/raytracer"
    truth_dir = "resources/truth"
    base_output_path = "output_sessions"
    server_url = "ec2-13-53-200-114.eu-north-1.compute.amazonaws.com:8080"
    server_port = "8080"
    image_urls = [
        f"http://{server_url}:{server_port}/{endpoint}"
        for endpoint in ["blurimage", "enhanceimage"]
    ]
    raytracer_url = f"http://{server_url}:{server_port}/raytracer"
    main(image_dir,
         scene_dir,
         truth_dir,
         base_output_path,
         image_urls,
         raytracer_url,
         count=None)
