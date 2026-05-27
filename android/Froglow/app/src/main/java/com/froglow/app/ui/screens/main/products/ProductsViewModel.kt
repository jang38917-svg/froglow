package com.froglow.app.ui.screens.main.products

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.froglow.app.data.SkinTypeStore
import com.froglow.app.network.ApiService
import com.froglow.app.network.ProductDto
import com.froglow.app.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductsViewModel(application: Application) : AndroidViewModel(application) {

    private val api: ApiService = RetrofitClient.create(getApplication())

    private val _allProducts = MutableStateFlow<List<ProductUi>>(emptyList())

    private val _bestMatch3 = MutableStateFlow<List<ProductUi>>(emptyList())
    val bestMatch3: StateFlow<List<ProductUi>> = _bestMatch3.asStateFlow()

    private val _products = MutableStateFlow<List<ProductUi>>(emptyList())
    val products: StateFlow<List<ProductUi>> = _products.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedSkinTypes = MutableStateFlow<Set<SkinTypeFilter>>(emptySet())
    val selectedSkinTypes: StateFlow<Set<SkinTypeFilter>> = _selectedSkinTypes.asStateFlow()

    private val _selectedProductTypes = MutableStateFlow<Set<ProductTypeFilter>>(emptySet())
    val selectedProductTypes: StateFlow<Set<ProductTypeFilter>> = _selectedProductTypes.asStateFlow()

    init {
        loadProducts()
    }

    fun retryLoadProducts() {
        loadProducts()
    }

    fun resetCategoryFilters() {
        _selectedSkinTypes.value = emptySet()
        _selectedProductTypes.value = emptySet()
        applyFiltersAndSorting()
    }

    fun applyCategoryFilters(
        skins: Set<SkinTypeFilter>,
        products: Set<ProductTypeFilter>
    ) {
        _selectedSkinTypes.value = skins
        _selectedProductTypes.value = products
        applyFiltersAndSorting()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val savedSkinType = SkinTypeStore.getSkinType(getApplication())
                val skinType = normalizeSkinType(savedSkinType)

                val list: List<ProductDto> = api.getRecommendedProducts(skinType)

                val mapped = list.map { dto ->
                    val score = dto.recommend_score ?: 0.0

                    ProductUi(
                        id = dto.product_id,
                        name = dto.product_name,
                        subTag = dto.concerns ?: "",
                        rating = score,
                        imageUrl = dto.product_image_url,
                        acneScore = gradeToScore(dto.acne_grade),
                        rednessScore = gradeToScore(dto.redness_grade),
                        spotScore = gradeToScore(dto.spot_grade),
                        wrinkleScore = gradeToScore(dto.wrinkle_grade)
                    )
                }

                _allProducts.value = mapped
                _bestMatch3.value = mapped.sortedByDescending { it.rating }.take(3)
                applyFiltersAndSorting()

                if (mapped.isEmpty()) {
                    _error.value = "추천 결과가 없습니다."
                }

            } catch (e: Exception) {
                e.printStackTrace()

                _allProducts.value = emptyList()
                _bestMatch3.value = emptyList()
                _products.value = emptyList()
                _error.value = "${e::class.java.simpleName}: ${e.message ?: "상품 추천 조회 실패"}"
            } finally {
                _loading.value = false
            }
        }
    }

    private fun normalizeSkinType(value: String?): String {
        return when (value?.trim()?.uppercase()) {
            "DRY" -> "DRY"
            "OILY" -> "OILY"
            "COMB", "COMBINATION" -> "COMB"
            else -> "OILY"
        }
    }

    private fun applyFiltersAndSorting() {
        var list = _allProducts.value

        val productTypes = _selectedProductTypes.value
        if (productTypes.isNotEmpty()) {
            list = list.filter { p ->
                productTypes.any { type ->
                    val key = type.label.replace("/", "").replace(" ", "")
                    p.name.replace(" ", "").contains(key)
                }
            }
        }

        list = list.sortedByDescending { it.rating }
        _products.value = list
    }

    private fun gradeToScore(grade: String?): Int {
        return when (grade?.trim()?.uppercase()) {
            "A" -> 8
            "B" -> 6
            "C" -> 4
            "D" -> 2
            "E" -> 1
            else -> 0
        }
    }
}