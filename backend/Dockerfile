FROM python:3.12-slim
WORKDIR /app
COPY pyproject.toml .
RUN pip install --no-cache-dir fastapi uvicorn[standard] python-multipart cryptography pydantic
COPY src ./src
RUN useradd -m appuser
USER appuser
EXPOSE 8000
CMD ["uvicorn","runnersz.main:app","--host","0.0.0.0","--port","8000"]
