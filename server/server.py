from pathlib import Path

from fastapi import FastAPI
from fastapi.responses import FileResponse

app = FastAPI()

MEDIA_DIR = Path("media")


@app.get("/movies")
def get_movies():
    movies = []

    for file in MEDIA_DIR.iterdir():
        if file.is_file():
            movies.append({
                "title": file.stem,
                "filename": file.name
            })

    return movies


@app.get("/movies/{filename}")
def stream_movie(filename: str):
    file_path = MEDIA_DIR / filename

    if not file_path.exists():
        return {"error": "Movie not found"}

    return FileResponse(
        file_path,
        media_type="video/mp4"
    )
