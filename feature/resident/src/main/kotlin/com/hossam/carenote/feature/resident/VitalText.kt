package com.hossam.carenote.feature.resident

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hossam.carenote.core.domain.vitals.VitalKind
import com.hossam.carenote.core.model.Vitals

/** One vital as text, in parts, so each screen can lay it out its own way. */
internal class VitalText(val label: String, val value: String, val unit: String)

@Composable
internal fun vitalLabel(kind: VitalKind): String = stringResource(
    when (kind) {
        VitalKind.BloodPressure -> R.string.resident_vital_bp_label
        VitalKind.Pulse -> R.string.resident_vital_pulse_label
        VitalKind.Temperature -> R.string.resident_vital_temperature_label
        VitalKind.OxygenSaturation -> R.string.resident_vital_spo2_label
    },
)

/** Formatted for the reader's locale, e.g. "36,8" in German; null when [vitals] has no value for [kind]. */
@Composable
internal fun vitalText(kind: VitalKind, vitals: Vitals): VitalText? {
    val label = vitalLabel(kind)
    return when (kind) {
        VitalKind.BloodPressure -> {
            val systolic = vitals.systolic
            val diastolic = vitals.diastolic
            if (systolic == null || diastolic == null) return null
            VitalText(
                label = label,
                value = stringResource(R.string.resident_vital_bp_value, systolic, diastolic),
                unit = stringResource(R.string.resident_vital_unit_mmhg),
            )
        }

        VitalKind.Pulse -> vitals.pulse?.let { pulse ->
            VitalText(
                label = label,
                value = stringResource(R.string.resident_vital_whole_value, pulse),
                unit = stringResource(R.string.resident_vital_unit_bpm),
            )
        }

        VitalKind.Temperature -> vitals.temperatureCelsius?.let { temperature ->
            VitalText(
                label = label,
                value = stringResource(R.string.resident_vital_temperature_value, temperature),
                unit = stringResource(R.string.resident_vital_unit_celsius),
            )
        }

        VitalKind.OxygenSaturation -> vitals.oxygenSaturation?.let { oxygenSaturation ->
            VitalText(
                label = label,
                value = stringResource(R.string.resident_vital_whole_value, oxygenSaturation),
                unit = stringResource(R.string.resident_vital_unit_percent),
            )
        }
    }
}
