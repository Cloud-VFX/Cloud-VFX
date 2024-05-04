import base64


class ResultProcessor:

    @staticmethod
    def decode_base64_to_image(base64_data, output_path):
        """Decode a base64 string to an image file, handling both str and bytes."""
        base64_bytes = base64_data
        if isinstance(base64_data, str):
            # If it's a str, we need to convert it to bytes
            base64_bytes = base64_data.encode()

        # Extract the base64 part after the comma (for data URLs)
        image_data = base64.b64decode(base64_bytes.split(b',')[1])

        with open(output_path, "wb") as image_file:
            image_file.write(image_data)
