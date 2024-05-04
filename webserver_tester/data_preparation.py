import base64
import json


class DataPreparation:

    @staticmethod
    def encode_image_to_base64(image_path):
        """Encode an image to a base64 string."""
        with open(image_path, "rb") as image_file:
            encoded_string = base64.b64encode(image_file.read()).decode('utf-8')
        return f"data:image/jpg;base64,{encoded_string}"

    @staticmethod
    def prepare_json_payload(scene_path, texture_path=None):
        """Prepare the JSON payload from the scene description and optional texture map."""
        with open(scene_path, 'r') as file:
            scene_data = file.read()
        payload: dict = {'scene': scene_data}
        if texture_path:
            with open(texture_path, 'rb') as file:
                texture_data = file.read()
            texture_hex = list(texture_data)
            payload['texmap'] = texture_hex
        return json.dumps(payload)
