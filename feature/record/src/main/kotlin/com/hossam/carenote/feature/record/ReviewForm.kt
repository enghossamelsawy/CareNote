package com.hossam.carenote.feature.record

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.hossam.carenote.core.designsystem.icon.CareNoteIcons
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme

/**
 * The finished note: the transcript and every vital can still be corrected before saving.
 * [footer] scrolls with the form, at its end.
 */
@Composable
internal fun ReviewForm(
    state: RecordState,
    onIntent: (RecordIntent) -> Unit,
    modifier: Modifier = Modifier,
    footer: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.sm),
    ) {
        OutlinedTextField(
            value = state.transcript,
            onValueChange = { onIntent(RecordIntent.TranscriptChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving,
            label = { Text(stringResource(R.string.record_transcript_label)) },
            minLines = 3,
        )

        Text(
            text = stringResource(R.string.record_vitals_title),
            style = CareNoteTheme.typography.titleMedium,
        )

        Text(
            text = stringResource(R.string.record_vital_bp_title),
            style = CareNoteTheme.typography.labelMedium,
            color = CareNoteTheme.colors.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.sm)) {
            VitalTextField(
                field = VitalField.Systolic,
                form = state.vitals,
                enabled = !state.isSaving,
                label = R.string.record_vital_systolic,
                description = R.string.record_vital_systolic_description,
                onIntent = onIntent,
                modifier = Modifier.weight(1f),
            )
            VitalTextField(
                field = VitalField.Diastolic,
                form = state.vitals,
                enabled = !state.isSaving,
                label = R.string.record_vital_diastolic,
                description = R.string.record_vital_diastolic_description,
                onIntent = onIntent,
                modifier = Modifier.weight(1f),
            )
        }
        VitalTextField(
            field = VitalField.Pulse,
            form = state.vitals,
            enabled = !state.isSaving,
            label = R.string.record_vital_pulse,
            unit = R.string.record_unit_bpm,
            onIntent = onIntent,
            modifier = Modifier.fillMaxWidth(),
        )
        VitalTextField(
            field = VitalField.Temperature,
            form = state.vitals,
            enabled = !state.isSaving,
            label = R.string.record_vital_temperature,
            unit = R.string.record_unit_celsius,
            onIntent = onIntent,
            modifier = Modifier.fillMaxWidth(),
        )
        VitalTextField(
            field = VitalField.OxygenSaturation,
            form = state.vitals,
            enabled = !state.isSaving,
            label = R.string.record_vital_oxygen_saturation,
            unit = R.string.record_unit_percent,
            onIntent = onIntent,
            modifier = Modifier.fillMaxWidth(),
            imeAction = ImeAction.Done,
        )
        footer()
    }
}

/**
 * Save; dictate more and add it to this note, e.g. one vital at a time; or throw the note away and start over.
 * "Record more" is the bigger choice, because adding is what carers do most while taking vitals.
 */
@Composable
internal fun ReviewActions(isSaving: Boolean, onIntent: (RecordIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.sm),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onIntent(RecordIntent.RecordClicked) }, enabled = !isSaving) {
                Text(stringResource(R.string.record_start_over))
            }
            OutlinedButton(
                onClick = { onIntent(RecordIntent.RecordMoreClicked) },
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = CareNoteIcons.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(CareNoteTheme.dimens.iconSm),
                )
                Text(
                    text = stringResource(R.string.record_more),
                    modifier = Modifier.padding(start = CareNoteTheme.spacing.sm),
                )
            }
        }
        Button(
            onClick = { onIntent(RecordIntent.SaveClicked) },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(CareNoteTheme.dimens.iconSm))
            } else {
                Text(stringResource(R.string.record_save))
            }
        }
    }
}

/**
 * "Next" on the keyboard moves to the following field; the last one uses [ImeAction.Done]. The [unit] is a trailing
 * icon because Material hides a suffix on an empty field. [description] is for a screen reader, when [label] only
 * makes sense next to a heading.
 */
@Composable
private fun VitalTextField(
    field: VitalField,
    form: VitalsForm,
    enabled: Boolean,
    @StringRes label: Int,
    onIntent: (RecordIntent) -> Unit,
    modifier: Modifier = Modifier,
    @StringRes unit: Int? = null,
    @StringRes description: Int? = null,
    imeAction: ImeAction = ImeAction.Next,
) {
    val error = form.errors[field]
    val descriptionText = description?.let { stringResource(it) }
    OutlinedTextField(
        value = form[field],
        onValueChange = { onIntent(RecordIntent.VitalChanged(field, it)) },
        modifier = if (descriptionText == null) {
            modifier
        } else {
            modifier.semantics { contentDescription = descriptionText }
        },
        enabled = enabled,
        label = { Text(stringResource(label)) },
        trailingIcon = unit?.let {
            { Text(stringResource(it), modifier = Modifier.padding(end = CareNoteTheme.spacing.md)) }
        },
        supportingText = error?.let { { Text(errorMessage(it)) } },
        isError = error != null,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (field == VitalField.Temperature) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = imeAction,
        ),
        singleLine = true,
    )
}

@Composable
private fun errorMessage(error: FieldError): String = when (error) {
    FieldError.NotANumber -> stringResource(R.string.record_field_error_not_a_number)
    FieldError.Missing -> stringResource(R.string.record_field_error_missing)
    FieldError.NotBelowSystolic -> stringResource(R.string.record_field_error_not_below_systolic)
    is FieldError.OutOfRange -> stringResource(R.string.record_field_error_range, error.min, error.max)
}
