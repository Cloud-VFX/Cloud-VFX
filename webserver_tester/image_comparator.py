from PIL import Image
import os


def images_are_equal(img1_path, img2_path):
    """
    Compare two images and return True if they are the same, False otherwise.
    """
    try:
        with Image.open(img1_path) as img1, Image.open(img2_path) as img2:
            if img1.size != img2.size:
                return False
            return list(img1.getdata()) == list(img2.getdata())
    except IOError:
        return False


def compare_images_in_directory(test_images_directory, truth_images_directory):
    """
    Compare all images in the test directory with their counterparts in the truth directory.
    """
    results = {}
    for filename in os.listdir(test_images_directory):
        test_image_path = os.path.join(test_images_directory, filename)
        truth_image_path = os.path.join(truth_images_directory, filename)
        # Exclude .log files
        if os.path.isfile(test_image_path) and filename.endswith(".log"):
            continue

        results[filename] = images_are_equal(test_image_path, truth_image_path)
    return results
