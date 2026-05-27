package com.froglow.app.ui.screens.main.products

enum class SkinTypeFilter(val label: String) {
    ALL("피부타입"),
    DRY("건성"),
    OILY("지성"),
    COMBINATION("복합성"),
    SENSITIVE("민감성"),
    DEHYDRATED_OILY("수부지"),
}

enum class ProductTypeFilter(val label: String) {
    ALL("제품 종류"),
    TONER("스킨/토너"),
    LOTION("로션/에멀전"),
    CREAM("크림"),
    SUNSCREEN("선크림"),
    ESSENCE("에센스/세럼"),
    CLEANSER("클렌저"),
}

enum class ConcernFilter(val label: String) {
    ALL("고민"),
    ACNE("여드름 완화"),
    REDNESS("홍조 완화"),
    SPOTS("잡티 개선"),
    WRINKLES("주름 개선"),
}