from pathlib import Path
import subprocess


def generate_poster(video_path: Path):
    poster_path = video_path.with_suffix(".jpg")

    # Don't regenerate if the poster already exists
    if poster_path.exists():
        return poster_path

    try:
        # Get video duration
        result = subprocess.run(
            [
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                str(video_path),
            ],
            capture_output=True,
            text=True,
            check=True,
        )

        duration = float(result.stdout.strip())

        # 10% into the video
        timestamp = duration * 0.10

        # Extract one frame
        subprocess.run(
            [
                "ffmpeg",
                "-y",
                "-ss", str(timestamp),
                "-i", str(video_path),
                "-frames:v", "1",
                "-q:v", "2",
                str(poster_path),
            ],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            check=True,
        )

        print(f"Generated poster: {poster_path.name}")

        return poster_path

    except Exception as e:
        print(f"Could not generate poster for {video_path.name}: {e}")
        return None
