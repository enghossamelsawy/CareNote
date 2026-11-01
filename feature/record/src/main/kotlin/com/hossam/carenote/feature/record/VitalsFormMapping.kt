package com.hossam.carenote.feature.record

import com.hossam.carenote.core.domain.vitals.VitalsProblem
import com.hossam.carenote.core.model.Vitals

/** Parsed vitals as editable text; missing values become empty fields. */
internal fun Vitals.asForm(): VitalsForm = VitalsForm(
    values = mapOf(
        VitalField.Systolic to systolic?.toString(),
        VitalField.Diastolic to diastolic?.toString(),
        VitalField.Pulse to pulse?.toString(),
        VitalField.Temperature to temperatureCelsius?.toString(),
        VitalField.OxygenSaturation to oxygenSaturation?.toString(),
    ).mapNotNull { (field, value) -> value?.let { field to it } }.toMap(),
)

/** Fields whose text is not empty and not a number. */
internal fun VitalsForm.unreadableFields(): Set<VitalField> =
    VitalField.entries.filter { field -> this[field].isNotBlank() && readNumber(field) == null }.toSet()

/** Call only when [unreadableFields] is empty. */
internal fun VitalsForm.toVitals(): Vitals = Vitals(
    systolic = readNumber(VitalField.Systolic)?.toInt(),
    diastolic = readNumber(VitalField.Diastolic)?.toInt(),
    pulse = readNumber(VitalField.Pulse)?.toInt(),
    temperatureCelsius = readNumber(VitalField.Temperature)?.toDouble(),
    oxygenSaturation = readNumber(VitalField.OxygenSaturation)?.toInt(),
)

/** Each domain problem shown next to the field the user has to change. */
internal fun VitalsForm.errorsFor(problems: Set<VitalsProblem>): Map<VitalField, FieldError> =
    problems.associate { problem ->
        when (problem) {
            VitalsProblem.BloodPressureIncomplete -> {
                val missing = if (this[VitalField.Systolic].isBlank()) VitalField.Systolic else VitalField.Diastolic
                missing to FieldError.Missing
            }

            VitalsProblem.SystolicOutOfRange -> VitalField.Systolic to FieldError.OutOfRange
            VitalsProblem.DiastolicOutOfRange -> VitalField.Diastolic to FieldError.OutOfRange
            VitalsProblem.DiastolicNotBelowSystolic -> VitalField.Diastolic to FieldError.NotBelowSystolic
            VitalsProblem.PulseOutOfRange -> VitalField.Pulse to FieldError.OutOfRange
            VitalsProblem.TemperatureOutOfRange -> VitalField.Temperature to FieldError.OutOfRange
            VitalsProblem.OxygenSaturationOutOfRange -> VitalField.OxygenSaturation to FieldError.OutOfRange
        }
    }

/** Whole numbers, except temperature, which also accepts a decimal comma ("36,8"). */
private fun VitalsForm.readNumber(field: VitalField): Number? {
    val text = this[field].trim()
    if (text.isEmpty()) return null
    return when (field) {
        VitalField.Temperature -> text.replace(',', '.').toDoubleOrNull()
        else -> text.toIntOrNull()
    }
}
