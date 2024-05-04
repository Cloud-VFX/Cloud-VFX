import os
import random


class FileManager:

    @staticmethod
    def list_files(directory, count=None):
        """List all files in the directory. 
        If count is specified, return a random subset."""
        files = [
            os.path.join(directory, file)
            for file in os.listdir(directory)
            if not file.startswith('.')
        ]
        if count is not None and count < len(files):
            return random.sample(files, count)
        return files
