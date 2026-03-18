def score_to_grade(score: float) -> str:
    if score <= 2:
        return "A"
    elif score <= 4:
        return "B"
    elif score <= 6:
        return "C"
    elif score <= 8:
        return "D"
    else:
        return "E"
