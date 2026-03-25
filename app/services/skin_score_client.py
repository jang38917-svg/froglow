from __future__ import annotations

import json
from typing import Any
import httpx


class SkinScoreClientError(Exception):
    pass


async def call_skin_score_api(
    *,
    image_path: str,
    detections: list[dict[str, Any]],
    api_url: str,
    timeout: float = 30.0,
) -> dict[str, Any]:
    try:
        with open(image_path, "rb") as f:
            files = {
                "file": ("front.jpg", f, "image/jpeg"),
            }

            data = {
                "detections": json.dumps(detections, ensure_ascii=False),
            }

            async with httpx.AsyncClient(timeout=timeout) as client:
                response = await client.post(api_url, data=data, files=files)

        response.raise_for_status()
        result = response.json()

    except httpx.HTTPError as e:
        raise SkinScoreClientError(f"AI server HTTP error: {e}") from e
    except Exception as e:
        raise SkinScoreClientError(f"AI server call failed: {e}") from e

    if "raw_scores" not in result:
        raise SkinScoreClientError("Invalid AI response")

    return result