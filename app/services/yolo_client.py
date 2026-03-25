from __future__ import annotations

from typing import Any
import httpx


class YoloClientError(Exception):
    pass


async def call_yolo_api(
    *,
    image_path: str,
    api_url: str,
    timeout: float = 30.0,
) -> dict[str, Any]:
    try:
        with open(image_path, "rb") as f:
            files = {
                "file": ("front.jpg", f, "image/jpeg"),
            }

            async with httpx.AsyncClient(timeout=timeout) as client:
                response = await client.post(api_url, files=files)

        response.raise_for_status()
        result = response.json()

    except httpx.HTTPError as e:
        raise YoloClientError(f"YOLO server HTTP error: {e}") from e
    except Exception as e:
        raise YoloClientError(f"YOLO server call failed: {e}") from e

    if "detections" not in result:
        raise YoloClientError("Invalid YOLO response")

    return result