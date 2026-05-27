package com.froglow.app.ui.screens.camera

object CapturedPhotoTempStore {
    private var photoUris: List<String> = emptyList()

    fun setPhotos(uris: List<String>) {
        photoUris = uris
    }

    fun getPhotos(): List<String> {
        return photoUris
    }

    fun clear() {
        photoUris = emptyList()
    }
}