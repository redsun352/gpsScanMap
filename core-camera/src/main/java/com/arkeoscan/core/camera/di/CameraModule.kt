package com.arkeoscan.core.camera.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * CameraSurveyCapture ve ExifGpsTool zaten @Inject constructor ile Hilt'e
 * tanıtıldığından (field/constructor injection), bu modül şu an için sadece
 * gelecekteki provider'lar (örn. harici GPR/manyetometre köprüsü) için
 * yer tutucudur.
 */
@Module
@InstallIn(SingletonComponent::class)
object CameraModule
