package com.hossam.carenote.feature.record

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme
import com.hossam.carenote.core.domain.vitals.VitalsValidator

/** The finished note: the transcript and every vital can still be corrected before saving. */
@Composable
internal fun ReviewForm(state: RecordState, onIntent: (RecordIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.sm),
    ) {
        OutlinedTextField(
            value = state.transcript,
            onValueChange = { onIntent(RecordIntent.TranscriptChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.record_transcript_label)) },
            minLines = 3,
        )

        Text(
            text = stringResource(R.string.record_vitals_title),
            style = MaterialTheme.typography.titleMedium,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.sm)) {
            VitalTextField(
                field = VitalField.Systolic,
                form = state.vitals,
                label = R.string.record_vital_systolic,
                unit = R.string.record_unit_mmhg,
                onIntent = onIntent,
                modifier = Modifier.weight(1f),
            )
            VitalTextField(
                field = VitalField.Diastolic,
                form = state.vitals,
                label = R.string.record_vital_diastolic,
                unit = R.string.record_unit_mmhg,
                onIntent = onIntent,
                modifier = Modifier.weight(1f),
            )
        }
        VitalTextField(
            field = VitalField.Pulse,
            form = state.vitals,
            label = R.string.record_vital_pulse,
            unit = R.string.record_unit_bpm,
            onIntent = onIntent,
            modifier = Modifier.fillMaxWidth(),
        )
        VitalTextField(
            field = VitalField.Temperature,
            form = state.vitals,
            label = R.string.record_vital_temperature,
            unit = R.string.record_unit_celsius,
            onIntent = onIntent,
            modifier = Modifier.fillMaxWidth(),
        )
        VitalTextField(
            field = VitalField.OxygenSaturation,
            form = state.vitals,
            label = R.string.record_vital_oxygen_saturation,
            unit = R.string.record_unit_percent,
            onIntent = onIntent,
            modifier = Modifier.fillMaxWidth(),
            imeAction = ImeAction.Done,
        )
    }
}

/** Save, or throw the note away and dictate it again. */
@Composable
internal fun ReviewActions(isSaving: Boolean, onIntent: (RecordIntent) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = { onIntent(RecordIntent.RecordClicked) },
            enabled = !isSaving,
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.record_again))
        }
        Button(
            onClick = { onIntent(RecordIntent.SaveClicked) },
            enabled = !isSaving,
            modifier = Modifier.weight(1f),
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(CareNoteTheme.dimens.iconSm))
            } else {
                Text(stringResource(R.string.record_save))
            }
        }
    }
}

/** "Next" on the keyboard moves to the following field; the last one uses [ImeAction.Done]. */
@Composable
private fun VitalTextField(
    field: VitalField,
    form: VitalsForm,
    @StringRes label: Int,
    @StringRes unit: Int,
    onIntent: (RecordIntent) -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
) {
    val error = form.errors[field]
    OutlinedTextField(
        value = form[field],
        onValueChange = { onIntent(RecordIntent.VitalChanged(field, it)) },
        modifier = modifier,
        label = { Text(stringResource(label)) },
        suffix = { Text(stringResource(unit)) },
        supportingText = error?.let { { Text(errorMessage(field, it)) } },
        isError = error != null,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (field == VitalField.Temperature) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = imeAction,
        ),
        singleLine = true,
    )
}

@Composable
private fun errorMessage(field: VitalField, error: FieldError): String = when (error) {
    FieldError.NotANumber -> stringResource(R.string.record_field_error_not_a_number)
    FieldError.Missing -> stringResource(R.string.record_field_error_missing)
    FieldError.NotBelowSystolic -> stringResource(R.string.record_field_error_not_below_systolic)
    FieldError.OutOfRange -> {
        val (min, max) = field.plausibleRange()
        stringResource(R.string.record_field_error_range, min, max)
    }
}

/** The same ranges the save use case checks, so the message always matches the rule. */
private fun VitalField.plausibleRange(): Pair<String, String> = when (this) {
    VitalField.Systolic -> VitalsValidator.SYSTOLIC.let { "${it.first}" to "${it.last}" }
    VitalField.Diastolic -> VitalsValidator.DIASTOLIC.let { "${it.first}" to "${it.last}" }
    VitalField.Pulse -> VitalsValidator.PULSE.let { "${it.first}" to "${it.last}" }
    VitalField.Temperature -> VitalsValidator.TEMPERATURE_CELSIUS.let { "${it.start}" to "${it.endInclusive}" }
    VitalField.OxygenSaturation -> VitalsValidator.OXYGEN_SATURATION.let { "${it.first}" to "${it.last}" }
}
