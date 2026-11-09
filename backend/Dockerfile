FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy \
    PATH="/app/.venv/bin:$PATH" \
    RZ_TMP_DIR=/var/tmp/runnersz

WORKDIR /app

# Dependencias fijadas por uv.lock, como exige el contrato (§3.2).
RUN pip install --no-cache-dir uv
COPY pyproject.toml uv.lock ./
RUN uv sync --locked --no-dev --no-install-project

COPY src ./src

# Usuario sin privilegios y volumen efímero para los .part.
RUN useradd --create-home --uid 10001 appuser \
    && mkdir -p "$RZ_TMP_DIR" \
    && chown appuser:appuser "$RZ_TMP_DIR"
VOLUME ["/var/tmp/runnersz"]
USER appuser

EXPOSE 8000
HEALTHCHECK --interval=10s --timeout=3s --retries=5 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8000/api/v1/health')"

CMD ["uvicorn", "runnersz.main:app", "--app-dir", "src", "--host", "0.0.0.0", "--port", "8000"]
