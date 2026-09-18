from pathlib import Path

from fastapi import FastAPI, HTTPException
from fastapi.responses import FileResponse

from poster import generate_poster


app = FastAPI()

MEDIA_DIR = Path("media")


@app.get("/movies")
def get_movies():
    movies = []

    for file in MEDIA_DIR.iterdir():

        if file.is_file() and file.suffix.lower() == ".mp4":

            poster = generate_poster(file)

            movies.append(
                {
                    "title": file.stem,
                    "filename": file.name,
                    "poster": poster.name if poster else None,
                }
            )

    return movies


@app.get("/movies/{filename}")
def get_media(filename: str):
    file_path = MEDIA_DIR / filename

    if not file_path.exists() or not file_path.is_file():
        raise HTTPException(
            status_code=404,
            detail="File not found",
        )

    return FileResponse(file_path)
