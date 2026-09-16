package com.lexora.service.core.domain

import com.lexora.service.core.model.AdditionalWorkApprovalStatus
import com.lexora.service.core.model.BuildTrace
import com.lexora.service.core.model.DispatchCandidate
import com.lexora.service.core.model.DispatchDecision
import com.lexora.service.core.model.InventoryBalance
import com.lexora.service.core.model.OptimizedRoute
import com.lexora.service.core.model.PaymentStatus
import com.lexora.service.core.model.QualityControlStatus
import com.lexora.service.core.model.RequestPriority
import com.lexora.service.core.model.RouteStop
import com.lexora.service.core.model.RuntimeEnvironment
import com.lexora.service.core.model.ScreenState
import com.lexora.service.core.model.ServiceRequest
import com.lexora.service.core.model.SlaEvaluation
import com.lexora.service.core.model.SlaRule
import com.lexora.service.core.model.SyncOperation
import com.lexora.service.core.model.SyncOperationStatus
import com.lexora.service.core.model.UiContentState
import com.lexora.service.core.model.UiMessage
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class SlaEngine {
    fun evaluate(
        request: ServiceRequest,
        rule: SlaRule?,
        createdAtEpochMs: Long,
        firstReactionAtEpochMs: Long? = null,
        nowEpochMs: Long,
        riskWindowMinutes: Int = 30,
    ): SlaEvaluation {
        if (rule == null) {
            return SlaEvaluation(request.id, null, request.slaDeadlineEpochMs, atRisk = false, breached = false)
        }
        val reactionDeadline = createdAtEpochMs + rule.reactionMinutes.minutesToMillis()
        val resolutionDeadline = request.slaDeadlineEpochMs
            ?: createdAtEpochMs + rule.resolutionMinutes.minutesToMillis()
        val reactionBreached = firstReactionAtEpochMs?.let { it > reactionDeadline }
            ?: (nowEpochMs > reactionDeadline)
        val resolutionBreached = request.closedAtEpochMs?.let { it > resolutionDeadline }
            ?: (nowEpochMs > resolutionDeadline)
        val riskWindow = riskWindowMinutes.minutesToMillis()
        val atRisk = !resolutionBreached && resolutionDeadline - nowEpochMs in 0..riskWindow
        return SlaEvaluation(
            requestId = request.id,
            reactionDeadlineEpochMs = reactionDeadline,
            resolutionDeadlineEpochMs = resolutionDeadline,
            atRisk = atRisk,
            breached = reactionBreached || resolutionBreached,
        )
    }

    private fun Int.minutesToMillis(): Long = this.toLong() * 60_000L
}

class AutoDispatchPolicy {
    fun choose(
        request: ServiceRequest,
        requiredSkills: Set<String>,
        candidates: List<DispatchCandidate>,
        nowEpochMs: Long,
    ): DispatchDecision {
        val scored = candidates.mapNotNull { candidate ->
            if (request.branchId != null && candidate.branchId != null && request.branchId != candidate.branchId) {
                return@mapNotNull null
            }
            if (!candidate.skillCodes.containsAll(requiredSkills)) return@mapNotNull null
            if (candidate.availableFromEpochMs != null && nowEpochMs < candidate.availableFromEpochMs) return@mapNotNull null
            if (candidate.availableToEpochMs != null && nowEpochMs > candidate.availableToEpochMs) return@mapNotNull null

            var score = 100
            val reasons = mutableListOf<String>()
            if (candidate.branchId == request.branchId && request.branchId != null) {
                score += 20
                reasons += "совпадает филиал"
            }
            if (requiredSkills.isNotEmpty()) {
                score += requiredSkills.size * 5
                reasons += "подходят навыки"
            }
            score -= candidate.activeWorkload * 10
            reasons += "текущая загрузка ${candidate.activeWorkload}"
            score += when (request.priority) {
                RequestPriority.URGENT -> 10
                RequestPriority.HIGH -> 5
                RequestPriority.NORMAL, RequestPriority.LOW -> 0
            }
            Triple(candidate, score, reasons)
        }
        val best = scored.maxWithOrNull(compareBy<Triple<DispatchCandidate, Int, List<String>>> { it.second }.thenByDescending { -it.first.activeWorkload })
            ?: return DispatchDecision(request.id, null, 0, listOf("нет подходящего исполнителя"), automatic = true)
        return DispatchDecision(
            requestId = request.id,
            employeeId = best.first.employeeId,
            score = best.second,
            reasons = best.third,
            automatic = true,
        )
    }
}

class RoutePlanner {
    /**
     * Deterministic nearest-neighbour baseline. Server-side optimizers may replace this
     * without changing the route contract.
     */
    fun optimize(
        employeeId: String,
        branchId: String?,
        stops: List<RouteStop>,
        startLatitude: Double? = null,
        startLongitude: Double? = null,
        generatedAtEpochMs: Long,
    ): OptimizedRoute {
        if (stops.size <= 1 || startLatitude == null || startLongitude == null) {
            return OptimizedRoute(employeeId, branchId, stops.sortedWith(stopComparator()), generatedAtEpochMs)
        }
        val remaining = stops.toMutableList()
        val result = mutableListOf<RouteStop>()
        var lat = startLatitude
        var lon = startLongitude
        while (remaining.isNotEmpty()) {
            val next = remaining
                .filter { it.latitude != null && it.longitude != null }
                .minWithOrNull(compareBy<RouteStop> { distanceKm(lat, lon, it.latitude!!, it.longitude!!) }.then(stopComparator()))
                ?: remaining.minWith(stopComparator())
            result += next
            remaining.remove(next)
            lat = next.latitude ?: lat
            lon = next.longitude ?: lon
        }
        return OptimizedRoute(employeeId, branchId, result, generatedAtEpochMs)
    }

    private fun stopComparator(): Comparator<RouteStop> =
        compareByDescending<RouteStop> { it.priority.ordinal }
            .thenBy { it.windowStartEpochMs ?: Long.MAX_VALUE }
            .thenBy { it.requestId }

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}

class InventoryPolicy {
    fun available(balance: InventoryBalance): Double =
        (balance.quantity - balance.reservedQuantity).coerceAtLeast(0.0)

    fun canIssue(balance: InventoryBalance, quantity: Double): Boolean =
        quantity > 0 && available(balance) >= quantity

    fun belowMinimum(balance: InventoryBalance, minimumStock: Double?): Boolean =
        minimumStock != null && available(balance) < minimumStock
}

class SyncRetryPolicy(
    private val baseDelayMs: Long = 15_000L,
    private val maxDelayMs: Long = 60L * 60L * 1000L,
) {
    fun nextDelayMs(attemptCount: Int): Long {
        val exponent = attemptCount.coerceIn(0, 12)
        return (baseDelayMs * 2.0.pow(exponent).toLong()).coerceAtMost(maxDelayMs)
    }

    fun shouldRetry(operation: SyncOperation, maxAttempts: Int = 8): Boolean =
        operation.status in setOf(SyncOperationStatus.FAILED, SyncOperationStatus.RETRY_WAIT) &&
            operation.attemptCount < maxAttempts
}

data class CompletionRequirements(
    val requireResult: Boolean = true,
    val requireChecklist: Boolean = false,
    val requirePhotos: Boolean = false,
    val requireAdditionalWorkApproval: Boolean = false,
    val requireQualityControl: Boolean = false,
    val requirePayment: Boolean = false,
)

data class CompletionContext(
    val resultPresent: Boolean,
    val checklistCompleted: Boolean,
    val photosPresent: Boolean,
    val approvalStatus: AdditionalWorkApprovalStatus,
    val qualityStatus: QualityControlStatus,
    val paymentStatus: PaymentStatus?,
)

data class CompletionGuardResult(val allowed: Boolean, val missing: List<String>)

class RequestCompletionGuard {
    fun validate(requirements: CompletionRequirements, context: CompletionContext): CompletionGuardResult {
        val missing = buildList {
            if (requirements.requireResult && !context.resultPresent) add("результат работ")
            if (requirements.requireChecklist && !context.checklistCompleted) add("чек-лист")
            if (requirements.requirePhotos && !context.photosPresent) add("фотофиксация")
            if (requirements.requireAdditionalWorkApproval && context.approvalStatus != AdditionalWorkApprovalStatus.APPROVED) add("согласование дополнительных работ")
            if (requirements.requireQualityControl && context.qualityStatus != QualityControlStatus.PASSED) add("контроль качества")
            if (requirements.requirePayment && context.paymentStatus != PaymentStatus.PAID) add("оплата")
        }
        return CompletionGuardResult(missing.isEmpty(), missing)
    }
}

class ClientDuplicatePolicy {
    fun normalizePhone(value: String?): String? = value
        ?.filter(Char::isDigit)
        ?.let { digits -> if (digits.length == 11 && digits.startsWith("8")) "7${digits.drop(1)}" else digits }
        ?.takeIf { it.length >= 10 }

    fun normalizeName(value: String): String = value.trim().lowercase().replace(Regex("\\s+"), " ")

    fun isLikelyDuplicate(
        leftName: String,
        leftPhone: String?,
        leftTaxId: String?,
        rightName: String,
        rightPhone: String?,
        rightTaxId: String?,
    ): Boolean {
        val leftTax = leftTaxId?.filter(Char::isDigit)?.takeIf { it.isNotBlank() }
        val rightTax = rightTaxId?.filter(Char::isDigit)?.takeIf { it.isNotBlank() }
        if (leftTax != null && rightTax != null && leftTax == rightTax) return true
        val leftNormalizedPhone = normalizePhone(leftPhone)
        val rightNormalizedPhone = normalizePhone(rightPhone)
        if (leftNormalizedPhone != null && leftNormalizedPhone == rightNormalizedPhone) return true
        return normalizeName(leftName) == normalizeName(rightName) && (leftNormalizedPhone != null || leftTax != null)
    }
}

data class PriceComponent(val quantity: Double, val unitPriceMinor: Long)
data class PriceAdjustment(val amountMinor: Long, val reason: String)
data class PriceCalculation(
    val subtotalMinor: Long,
    val adjustmentsMinor: Long,
    val totalMinor: Long,
)

class PricingEngine {
    fun calculate(
        works: List<PriceComponent>,
        materials: List<PriceComponent>,
        adjustments: List<PriceAdjustment> = emptyList(),
    ): PriceCalculation {
        val subtotal = (works + materials).sumOf { component ->
            (component.quantity * component.unitPriceMinor.toDouble()).toLong()
        }
        val adjustmentTotal = adjustments.sumOf { it.amountMinor }
        return PriceCalculation(
            subtotalMinor = subtotal,
            adjustmentsMinor = adjustmentTotal,
            totalMinor = (subtotal + adjustmentTotal).coerceAtLeast(0L),
        )
    }
}

class SearchPolicy {
    fun normalize(text: String): String = text.trim().lowercase().replace(Regex("\\s+"), " ")

    fun matches(query: String, vararg fields: String?): Boolean {
        val normalized = normalize(query)
        if (normalized.isBlank()) return true
        return fields.filterNotNull().any { normalize(it).contains(normalized) }
    }
}

class ScreenStateFactory {
    fun <T> loading(): ScreenState<T> = ScreenState(UiContentState.LOADING)
    fun <T> content(data: T): ScreenState<T> = ScreenState(UiContentState.CONTENT, data)
    fun <T> empty(data: T? = null): ScreenState<T> = ScreenState(UiContentState.EMPTY, data)
    fun <T> offline(data: T? = null): ScreenState<T> = ScreenState(
        UiContentState.OFFLINE,
        data,
        UiMessage("offline", "Нет подключения. Изменения будут синхронизированы позже."),
    )
    fun <T> error(code: String, text: String, recoverable: Boolean = true): ScreenState<T> = ScreenState(
        UiContentState.ERROR,
        message = UiMessage(code, text, recoverable),
    )
    fun <T> noPermission(): ScreenState<T> = ScreenState(
        UiContentState.NO_PERMISSION,
        message = UiMessage("forbidden", "Недостаточно прав для выполнения операции.", recoverable = false),
    )
}

class BuildTracePolicy {
    fun validate(trace: BuildTrace): List<String> = buildList {
        if (trace.versionName.isBlank()) add("versionName пуст")
        if (trace.versionCode <= 0) add("versionCode должен быть положительным")
        if (trace.environment == RuntimeEnvironment.PRODUCTION && trace.commitSha.isNullOrBlank()) {
            add("production-сборка должна быть связана с commit SHA")
        }
        if (trace.environment == RuntimeEnvironment.PRODUCTION && trace.buildId.isNullOrBlank()) {
            add("production-сборка должна иметь build ID")
        }
    }
}
