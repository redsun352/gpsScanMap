package com.arkeoscan.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "surface_analysis_results",
    foreignKeys = [
        ForeignKey(
            entity = CameraSurveyPhotoEntity::class,
            parentColumns = ["id"],
            childColumns = ["photo_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("photo_id", unique = true)]
)
data class SurfaceAnalysisResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "photo_id")
    val photoId: Long,

    @ColumnInfo(name = "mean_red")
    val meanRed: Float,

    @ColumnInfo(name = "mean_green")
    val meanGreen: Float,

    @ColumnInfo(name = "mean_blue")
    val meanBlue: Float,

    @ColumnInfo(name = "brightness_variance")
    val brightnessVariance: Float,

    @ColumnInfo(name = "vegetation_index")
    val vegetationIndex: Float,

    @ColumnInfo(name = "texture_roughness")
    val textureRoughness: Float,

    @ColumnInfo(name = "noteworthy_surface_patch")
    val noteworthySurfacePatch: Boolean,

    // Virgülle ayrılmış SurfaceNoteReason enum adları (örn. "VEGETATION_CONTRAST,TEXTURE_CONTRAST")
    @ColumnInfo(name = "note_reasons")
    val noteReasons: String,

    @ColumnInfo(name = "analyzed_at_millis")
    val analyzedAtMillis: Long
)
