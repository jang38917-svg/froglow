def calculate_area(x1, y1, x2, y2):
    return max(0, (x2 - x1)) * max(0, (y2 - y1))


def calculate_center(x1, y1, x2, y2):
    return {
        "x": round((x1 + x2) / 2, 2),
        "y": round((y1 + y2) / 2, 2)
    }