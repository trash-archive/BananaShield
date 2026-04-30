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
    val allConfidences: Map<String, Float> = emptyMap(),
    val diseaseInfo: DiseaseInfo,
    val bbtvVerdict: BBTVVerdict? = null,
    val bbtvScore: Int = -1,
    val bbtvStreakAnswer: String = "",
    val bbtvTimelineAnswer: String = "",
    val bbtvSpreadAnswer: String = "",
    val bbtvAphidAnswer: String = ""
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
        val resizedBitmap = centerCropBitmap(bitmap)
        val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)
        val inputFeature = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), org.tensorflow.lite.DataType.FLOAT32)
        inputFeature.loadBuffer(byteBuffer)

        val outputs = model?.process(inputFeature)
        val outputFeature = outputs?.outputFeature0AsTensorBuffer

        val confidences = outputFeature?.floatArray ?: floatArrayOf()

        val maxIndex = confidences.indices.maxByOrNull { confidences[it] } ?: 0
        val label = labels.getOrNull(maxIndex) ?: "Unknown"
        val confidence = confidences.getOrNull(maxIndex) ?: 0f

        val allConfidences = labels.mapIndexed { i, lbl -> lbl to (confidences.getOrNull(i) ?: 0f) }.toMap()
        val diseaseInfo = getDiseaseInfo(label, confidence)
        // Fix confidenceLevel to reflect the actual post-softmax confidence
        val correctedDiseaseInfo = diseaseInfo.copy(confidenceLevel = "${(confidence * 100).toInt()}%")

        return Classification(
            label = label,
            confidence = confidence,
            allConfidences = allConfidences,
            diseaseInfo = correctedDiseaseInfo
        )
    }

    private fun centerCropBitmap(bitmap: Bitmap, targetSize: Int = 224): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        val cropped = Bitmap.createBitmap(bitmap, x, y, size, size)
        return Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
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
                byteBuffer.putFloat((value shr 16 and 0xFF) / 255.0f)
                byteBuffer.putFloat((value shr 8 and 0xFF) / 255.0f)
                byteBuffer.putFloat((value and 0xFF) / 255.0f)
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
                    "Dark green 'dot-dash' streaks on the midrib, petiole, and leaf stem — the most definitive sign of BBTV",
                    "Leaves progressively become shorter, narrower, and more upright with each new emergence",
                    "Leaf margins roll upward and leaves feel stiff or brittle",
                    "Severely stunted plant with leaves bunched tightly at the top (bunchy top appearance)",
                    "Chlorosis (yellowing) along leaf margins, especially on younger leaves",
                    "No fruit production — infected plants rarely reach flowering stage"
                ),
                causes = listOf(
                    "Primary vector: banana aphid (Pentalonia nigronervosa) — transmits the virus from infected to healthy plants",
                    "Use of infected suckers or cuttings taken from a BBTV-positive plant or farm",
                    "Movement of infected planting material between farms or regions",
                    "Aphid colonies spreading from nearby infected plants or weeds hosting aphids"
                ),
                treatmentSteps = listOf(
                    TreatmentStep(
                        "Confirm Before Acting",
                        "Do NOT uproot immediately. Verify at least 3 key signs: dot-dash streaking on midrib, progressive worsening, and multiple plants affected. Uprooting a healthy plant is irreversible.",
                        "remove"
                    ),
                    TreatmentStep(
                        "Isolate the Affected Plant",
                        "Prevent people, tools, and animals from moving between the suspected plant and healthy ones. Do not take suckers from this plant.",
                        "quarantine"
                    ),
                    TreatmentStep(
                        "Control Aphid Vectors Immediately",
                        "Apply a systemic insecticide (e.g., imidacloprid) to the affected plant and all plants within 5 meters to eliminate aphid vectors before they spread the virus further.",
                        "insecticide"
                    ),
                    TreatmentStep(
                        "Remove and Destroy Confirmed Infected Plants",
                        "Once BBTV is confirmed, inject the pseudostem with kerosene or glyphosate to kill the plant in place, then dig out the entire corm and root system. Do not leave debris.",
                        "remove"
                    )
                ),
                preventiveMeasures = listOf(
                    PreventiveMeasure(
                        "cultural",
                        "Use Clean Planting Material",
                        listOf(
                            "Only use suckers or tissue-cultured plants from certified BBTV-free sources",
                            "Never take planting material from farms with unknown disease history",
                            "Inspect new planting material for dot-dash streaking before planting",
                            "Quarantine new plants for 2–4 weeks before introducing to your farm"
                        ),
                        "plant"
                    ),
                    PreventiveMeasure(
                        "chemical",
                        "Aphid Vector Control",
                        listOf(
                            "Apply systemic insecticides (imidacloprid or thiamethoxam) at planting and every 3 months",
                            "Use reflective silver mulch around plants to deter aphid landing",
                            "Inspect the base of pseudostems regularly for aphid colonies",
                            "Treat a buffer zone of at least 10 meters around any suspected plant"
                        ),
                        "spray"
                    ),
                    PreventiveMeasure(
                        "monitoring",
                        "Early Detection & Surveillance",
                        listOf(
                            "Walk your plantation weekly and look specifically for dot-dash streaking on midribs",
                            "Mark any suspicious plant with a stake and monitor it for 1–2 weeks before deciding",
                            "Keep a simple record of which plants were checked and when",
                            "Report confirmed or suspected BBTV to your local agricultural office immediately"
                        ),
                        "monitor"
                    ),
                    PreventiveMeasure(
                        "biosecurity",
                        "Farm Biosecurity",
                        listOf(
                            "Disinfect cutting tools with 10% bleach solution between each plant",
                            "Do not share tools between farms without disinfecting",
                            "Limit visitor access to your plantation during an outbreak",
                            "Remove and destroy weed hosts that may harbor aphids near the plantation"
                        ),
                        "security"
                    )
                ),
                safetyNotes = listOf(
                    "Do not uproot a plant based on the AI scan alone — confirm with the dot-dash streak sign and questionnaire result first",
                    "Burn or bury destroyed plant material at least 50 cm deep — do not compost",
                    "Wear gloves when handling suspected infected plants and disinfect afterward",
                    "BBTV is a notifiable disease in many countries — contact your agricultural extension officer if confirmed"
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
