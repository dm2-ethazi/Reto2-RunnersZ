import argparse

import uvicorn

from .api import app


def main() -> None:
    parser = argparse.ArgumentParser(description="Backend local del Reto2 RunnersZ")
    parser.add_argument("--port", type=int, required=True, help="Puerto local (1-65535)")
    args = parser.parse_args()
    if not 1 <= args.port <= 65535:
        parser.error("--port debe estar entre 1 y 65535")
    uvicorn.run(app, host="127.0.0.1", port=args.port, log_level="warning", access_log=False)


if __name__ == "__main__":
    main()
