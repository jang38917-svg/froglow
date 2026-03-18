#설문 질문/선택지 Seed 데이터 삽입 파일

# 서버 시작 시 DB에 질문이 하나도 없으면 자동으로 삽입
# 이미 데이터가 있으면 중복 삽입 금지


from sqlalchemy.orm import Session
from app.models.survey import SurveyQuestion, SurveyOption


def seed_survey_questions(db: Session) -> None:

    #survey_questions 테이블이 비어있을 때만 seed 데이터 추가
    # 이미 질문이 있으면 seed 생략 (중복 방지)
    exists = db.query(SurveyQuestion).first()
    if exists:
        return


    # 질문/선택지/점수 정의
    questions_data = [
        {
            "order": 1,
            "text": "세안 후 아무것도 바르지 않았을 때 피부 느낌은?",
            "options": [
                {"text": "얼굴 전체가 심하게 당기고 건조하다", "dry": 2, "comb": 0, "oily": 0},
                {"text": "약간 당기지만 시간이 지나면 괜찮아진다", "dry": 0, "comb": 1, "oily": 0},
                {"text": "당김은 거의 없고 편안하다", "dry": 0, "comb": 0, "oily": 0},
                {"text": "오히려 금방 번들거린다", "dry": 0, "comb": 0, "oily": 2},
            ],
        },
        {
            "order": 2,
            "text": "오후(세안 후 3~5시간 뒤) 피부 상태는?",
            "options": [
                {"text": "여전히 건조하거나 각질이 보인다", "dry": 2, "comb": 0, "oily": 0},
                {"text": "볼은 건조한데 T존만 번들거린다", "dry": 0, "comb": 2, "oily": 0},
                {"text": "얼굴 전체가 살짝 번들거린다", "dry": 0, "comb": 0, "oily": 2},
                {"text": "유분 때문에 화장이 쉽게 무너진다", "dry": 0, "comb": 0, "oily": 2},
            ],
        },
        {
            "order": 3,
            "text": "하루 중 유분이 가장 많이 느껴지는 부위는?",
            "options": [
                {"text": "거의 없다", "dry": 2, "comb": 0, "oily": 0},
                {"text": "이마·코만 느껴진다", "dry": 0, "comb": 2, "oily": 0},
                {"text": "얼굴 전체에서 느껴진다", "dry": 0, "comb": 0, "oily": 2},
            ],
        },
        {
            "order": 4,
            "text": "보습제를 안 바르면 어떤가요?",
            "options": [
                {"text": "금방 당기고 불편하다", "dry": 2, "comb": 0, "oily": 0},
                {"text": "볼은 당기고 T존은 괜찮다", "dry": 0, "comb": 2, "oily": 0},
                {"text": "크게 불편하지 않다", "dry": 0, "comb": 0, "oily": 0},
                {"text": "오히려 답답하거나 기름진 느낌이다", "dry": 0, "comb": 0, "oily": 2},
            ],
        },
        {
            "order": 5,
            "text": "피부 트러블이 생기는 패턴은?",
            "options": [
                {"text": "각질, 잔주름, 들뜸 위주", "dry": 2, "comb": 0, "oily": 0},
                {"text": "이마·코에 뾰루지, 볼은 건조", "dry": 0, "comb": 2, "oily": 0},
                {"text": "피지·블랙헤드·여드름 위주", "dry": 0, "comb": 0, "oily": 2},
            ],
        },
        {
            "order": 6,
            "text": "파우더나 기름종이를 사용할 때?",
            "options": [
                {"text": "거의 필요 없다", "dry": 2, "comb": 0, "oily": 0},
                {"text": "T존에만 필요하다", "dry": 0, "comb": 2, "oily": 0},
                {"text": "얼굴 전체에 자주 필요하다", "dry": 0, "comb": 0, "oily": 2},
            ],
        },
    ]

    # DB 삽입
    for q in questions_data:
        question = SurveyQuestion(text=q["text"], order=q["order"])
        db.add(question)
        db.flush()  # question.id 확보

        for opt in q["options"]:
            option = SurveyOption(
                question_id=question.id,
                text=opt["text"],
                dry_score=opt["dry"],
                comb_score=opt["comb"],
                oily_score=opt["oily"],
            )
            db.add(option)

    db.commit()
