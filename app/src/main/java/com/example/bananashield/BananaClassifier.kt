package com.example.bananashield

import android.content.Context
import android.graphics.Bitmap
import com.example.bananashield.ml.ModelUnquant
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class Classification(
    val label: String,
    val confidence: Float,
    val diseaseInfo: DiseaseInfo
)

data class DiseaseInfo(
    val name: String,
    val scientificName: String,
    val diseaseType: String,
    val severity: String,
    val confidenceLevel: String,
    val symptoms: List<String>,
    val causes: List<String>,
    val treatmentSteps: List<TreatmentStep>,
    val preventiveMeasures: List<PreventiveMeasure>,
    val safetyNotes: List<String>
)

data class TreatmentStep(
    val title: String,
    val description: String,
    val icon: String // For UI identification
)

data class PreventiveMeasure(
    val category: String,
    val title: String,
    val steps: List<String>,
    val icon: String
)

class BananaClassifier(private val context: Context) {

    private var model: ModelUnquant? = null

    private val labels = listOf(
        "Healthy",
        "Black Sigatoka",
        "Banana Bunchy Top",
        "Fusarium Wilt (TR4)"
    )

    init {
        model = ModelUnquant.newInstance(context)
    }

    fun classify(bitmap: Bitmap): Classification {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)
        val inputFeature = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), org.tensorflow.lite.DataType.FLOAT32)
        inputFeature.loadBuffer(byteBuffer)

        val outputs = model?.process(inputFeature)
        val outputFeature = outputs?.outputFeature0AsTensorBuffer

        val confidences = outputFeature?.floatArray ?: floatArrayOf()
        val maxIndex = confidences.indices.maxByOrNull { confidences[it] } ?: 0
        val label = labels.getOrNull(maxIndex) ?: "Unknown"
        val confidence = confidences.getOrNull(maxIndex) ?: 0f

        return Classification(
            label = label,
            confidence = confidence,
            diseaseInfo = getDiseaseInfo(label, confidence)
        )
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(224 * 224)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until 224) {
            for (j in 0 until 224) {
                val value = intValues[pixel++]
                byteBuffer.putFloat(((value shr 16 and 0xFF) / 255.0f))
                byteBuffer.putFloat(((value shr 8 and 0xFF) / 255.0f))
                byteBuffer.putFloat(((value and 0xFF) / 255.0f))
            }
        }
        return byteBuffer
    }

    private fun getDiseaseInfo(label: String, confidence: Float): DiseaseInfo {
        val severity = when {
            confidence < 0.6f -> "Low Confidence"
            confidence < 0.8f -> "Moderate"
            else -> "High"
        }

        return when {
            label.contains("Black Sigatoka", ignoreCase = true) -> DiseaseInfo(
                name = "Black Sigatoka",
                scientificName = "Mycosphaerella fijiensis",
                diseaseType = "Fungal Disease",
                severity = "Moderate Severity",
                confidenceLevel = "${(confidence * 100).toInt()}%",
                symptoms = listOf(
                    "Dark brown to black streaks on leaves",
                    "Yellow halos around infected areas",
                    "Premature leaf death and drying",
                    "Reduced fruit production and quality"
                ),
                causes = listOf(
                    "Fungal spores spread by wind and rain",
                    "High humidity and warm temperatures",
                    "Dense plant spacing with poor air circulation",
                    "Prolonged leaf wetness duration"
                ),
                treatmentSteps = listOf(
                    TreatmentStep(
                        "Copper-based Fungicide",
                        "Most effective for Black Sigatoka. Apply copper-based or systemic fungicide every 7-14 days",
                        "fungicide"
                    ),
                    TreatmentStep(
                        "Systemic Fungicide",
                        "Alternative treatment option. Rotate fungicide types to prevent resistance",
                        "alternative"
                    )
                ),
                preventiveMeasures = listOf(
                    PreventiveMeasure(
                        "cultural",
                        "Cultural Practices",
                        listOf(
                            "Remove infected leaves regularly",
                            "Maintain proper plant spacing (2-3m)",
                            "Ensure adequate air circulation",
                            "Dispose of infected material properly"
                        ),
                        "plant"
                    ),
                    PreventiveMeasure(
                        "water",
                        "Water Management",
                        listOf(
                            "Improve drainage systems",
                            "Avoid overhead irrigation",
                            "Reduce leaf wetness duration",
                            "Water in early morning"
                        ),
                        "water"
                    ),
                    PreventiveMeasure(
                        "chemical",
                        "Chemical Control",
                        listOf(
                            "Apply preventive fungicidal sprays",
                            "Rotate fungicide types to prevent resistance",
                            "Follow recommended application schedules",
                            "Use protective equipment during application"
                        ),
                        "spray"
                    ),
                    PreventiveMeasure(
                        "monitoring",
                        "Regular Monitoring",
                        listOf(
                            "Scout plants weekly for early symptoms",
                            "Monitor weather conditions",
                            "Keep records of disease occurrence",
                            "Check neighboring plantations"
                        ),
                        "monitor"
                    )
                ),
                safetyNotes = listOf(
                    "Always wear protective equipment (gloves, mask) when handling fungicides",
                    "Follow product instructions and safety guidelines",
                    "Keep fungicides away from children and pets",
                    "Wash hands thoroughly after application"
                )
            )

            label.contains("Bunchy Top", ignoreCase = true) -> DiseaseInfo(
                name = "Banana Bunchy Top Disease",
                scientificName = "Banana Bunchy Top Virus (BBTV)",
                diseaseType = "Viral Disease",
                severity = "Severe - Highly Contagious",
                confidenceLevel = "${(confidence * 100).toInt()}%",
                symptoms = listOf(
                    "Dark green streaks on leaf stems and midribs",
                    "Stunted growth with bunched leaves at top",
                    "Narrow, brittle, and upright leaves",
                    "No fruit production in infected plants"
                ),
                causes = listOf(
                    "Transmitted by banana aphids (Pentalonia nigronervosa)",
                    "Spread through infected planting material",
                    "Movement of infected plants between areas",
                    "Aphid vectors moving from infected plants"
                ),
                treatmentSteps = listOf(
                    TreatmentStep(
                        "Immediate Plant Removal",
                        "Remove and destroy infected plants immediately to prevent spread. Dig out entire root system",
                        "remove"
                    ),
                    TreatmentStep(
                        "Aphid Control",
                        "Apply systemic insecticides to control aphid vectors. Treat surrounding healthy plants",
                        "insecticide"
                    )
                ),
                preventiveMeasures = listOf(
                    PreventiveMeasure(
                        "cultural",
                        "Cultural Practices",
                        listOf(
                            "Use only virus-free certified planting material",
                            "Remove all infected plants immediately",
                            "Maintain weed-free environment",
                            "Isolate new plants before planting"
                        ),
                        "plant"
                    ),
                    PreventiveMeasure(
                        "chemical",
                        "Vector Control",
                        listOf(
                            "Apply systemic insecticides for aphid control",
                            "Use reflective mulches to deter aphids",
                            "Monitor aphid populations regularly",
                            "Treat buffer zones around plantations"
                        ),
                        "spray"
                    ),
                    PreventiveMeasure(
                        "resistant",
                        "Resistant Varieties",
                        listOf(
                            "Plant BBTV-resistant banana cultivars",
                            "Consider hybrid varieties with tolerance",
                            "Research local resistant varieties",
                            "Consult agricultural extension services"
                        ),
                        "variety"
                    ),
                    PreventiveMeasure(
                        "monitoring",
                        "Early Detection",
                        listOf(
                            "Inspect plants weekly for symptoms",
                            "Monitor aphid activity closely",
                            "Mark and track suspicious plants",
                            "Report outbreaks to authorities immediately"
                        ),
                        "monitor"
                    )
                ),
                safetyNotes = listOf(
                    "Disinfect tools between plants to prevent spread",
                    "Burn or bury infected plant material deeply",
                    "Do not compost infected plants",
                    "Follow biosecurity protocols when moving between plantations"
                )
            )

            label.contains("Fusarium", ignoreCase = true) || label.contains("TR4", ignoreCase = true) -> DiseaseInfo(
                name = "Fusarium Wilt (TR4)",
                scientificName = "Fusarium oxysporum f. sp. cubense TR4",
                diseaseType = "Soil-borne Fungal Disease",
                severity = "Critical - Highly Devastating",
                confidenceLevel = "${(confidence * 100).toInt()}%",
                symptoms = listOf(
                    "Yellowing and wilting of older leaves progressing upward",
                    "Vascular discoloration (reddish-brown) in pseudostem",
                    "Leaf collapse and eventual plant death",
                    "Internal browning visible when pseudostem is cut"
                ),
                causes = listOf(
                    "Soil-borne fungus with long-term survival (decades)",
                    "Spread through contaminated soil and water",
                    "Movement of infected planting material",
                    "Contaminated tools, vehicles, and footwear"
                ),
                treatmentSteps = listOf(
                    TreatmentStep(
                        "Quarantine & Removal",
                        "Immediately quarantine affected area. Remove and destroy all infected plants including roots",
                        "quarantine"
                    ),
                    TreatmentStep(
                        "Soil Management",
                        "No cure available. Focus on containment and prevention. Consider soil solarization in small areas",
                        "soil"
                    )
                ),
                preventiveMeasures = listOf(
                    PreventiveMeasure(
                        "resistant",
                        "Resistant Varieties",
                        listOf(
                            "Plant only TR4-resistant banana varieties",
                            "Use certified disease-free planting material",
                            "Consider Cavendish alternatives (GCTCV-219)",
                            "Consult research institutions for resistant cultivars"
                        ),
                        "variety"
                    ),
                    PreventiveMeasure(
                        "biosecurity",
                        "Biosecurity Measures",
                        listOf(
                            "Disinfect all tools and equipment thoroughly",
                            "Clean footwear and vehicle tires before entry/exit",
                            "Prevent soil movement from infected areas",
                            "Establish footbaths with disinfectant at entry points"
                        ),
                        "security"
                    ),
                    PreventiveMeasure(
                        "cultural",
                        "Farm Hygiene",
                        listOf(
                            "Maintain proper field drainage",
                            "Avoid waterlogging conditions",
                            "Remove and destroy all infected plant debris",
                            "Implement strict visitor protocols"
                        ),
                        "plant"
                    ),
                    PreventiveMeasure(
                        "monitoring",
                        "Surveillance & Reporting",
                        listOf(
                            "Conduct regular farm inspections",
                            "Document and report suspected cases immediately",
                            "Coordinate with agricultural authorities",
                            "Participate in area-wide monitoring programs"
                        ),
                        "monitor"
                    )
                ),
                safetyNotes = listOf(
                    "TR4 is a quarantine disease - report immediately to authorities",
                    "Do NOT move soil, plants, or equipment from infected areas",
                    "Strict biosecurity is essential to prevent spread",
                    "Follow all government containment regulations"
                )
            )

            else -> DiseaseInfo(
                name = "Healthy Plant",
                scientificName = "No Pathogen Detected",
                diseaseType = "Healthy Status",
                severity = "Excellent Condition",
                confidenceLevel = "${(confidence * 100).toInt()}%",
                symptoms = listOf(
                    "Vibrant green leaves with no discoloration",
                    "Strong upright plant structure",
                    "No visible damage or abnormalities",
                    "Optimal growth and development"
                ),
                causes = listOf(
                    "Good agricultural practices maintained",
                    "Proper nutrition and water management",
                    "Effective disease prevention measures",
                    "Healthy growing environment"
                ),
                treatmentSteps = listOf(
                    TreatmentStep(
                        "Maintain Current Practices",
                        "Continue current management practices. No treatment needed for healthy plants",
                        "maintain"
                    )
                ),
                preventiveMeasures = listOf(
                    PreventiveMeasure(
                        "monitoring",
                        "Continuous Monitoring",
                        listOf(
                            "Inspect plants regularly for any changes",
                            "Monitor for early disease symptoms",
                            "Keep detailed farm records",
                            "Stay alert to neighboring farm conditions"
                        ),
                        "monitor"
                    ),
                    PreventiveMeasure(
                        "nutrition",
                        "Plant Nutrition",
                        listOf(
                            "Maintain balanced fertilization program",
                            "Ensure adequate potassium levels",
                            "Monitor soil pH and nutrient status",
                            "Apply organic matter regularly"
                        ),
                        "nutrition"
                    ),
                    PreventiveMeasure(
                        "cultural",
                        "Good Agricultural Practices",
                        listOf(
                            "Maintain proper plant spacing",
                            "Ensure good drainage and irrigation",
                            "Practice field sanitation",
                            "Use clean planting materials"
                        ),
                        "plant"
                    ),
                    PreventiveMeasure(
                        "biosecurity",
                        "Preventive Biosecurity",
                        listOf(
                            "Limit unnecessary farm access",
                            "Clean tools and equipment regularly",
                            "Quarantine new planting material",
                            "Implement visitor protocols"
                        ),
                        "security"
                    )
                ),
                safetyNotes = listOf(
                    "Maintain vigilance even with healthy plants",
                    "Early detection is key to disease management",
                    "Share best practices with neighboring farmers",
                    "Stay informed about disease outbreaks in the region"
                )
            )
        }
    }

    fun close() {
        model?.close()
    }
}
