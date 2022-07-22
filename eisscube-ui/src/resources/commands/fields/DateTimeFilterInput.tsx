import { useState } from 'react';
import { useInput, InputHelperText } from 'react-admin';
import { useFormContext } from 'react-hook-form';
import { TextField } from '@mui/material';
import { AdapterMoment } from '@mui/x-date-pickers/AdapterMoment';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { DesktopDateTimePicker } from '@mui/x-date-pickers/DesktopDateTimePicker';
import { DateTimeMomentFormat } from '../../../App';

const DateTimeFilterInput = (props: any) => {
	const [datetime, setDatetime] = useState<Date | null>(null);
	const { setValue } = useFormContext();
	const { label, source, onChange, onBlur, helperText, ...rest } = props;
    const {
        field,
        fieldState: { isTouched, error },
        formState: { isSubmitted }
    } = useInput({
        onChange,
        onBlur,
        ...props,
    });
	return (
		<LocalizationProvider dateAdapter={AdapterMoment}>
			<DesktopDateTimePicker
				label={label}
				value={datetime}
				ampm={false}
				closeOnSelect={false}
				views={['year', 'month', 'day', 'hours', 'minutes', 'seconds']}
				inputFormat={DateTimeMomentFormat}
				mask='__/__/____, __:__:__'
				onChange={(newValue) => {
					setDatetime(newValue);
					setValue(source, newValue?.toISOString());
				}}
				renderInput={(params) =>
					<TextField
						{...field}
						{...params}
						{...rest}
						onBlur={onBlur}
						error={(isTouched || isSubmitted) && error}
						helperText={
							<InputHelperText
								touched={isTouched || isSubmitted}
								error={error?.message}
								helperText={helperText}
							/>
						}
					/>
				}
				componentsProps={{
					actionBar: {
						actions: ['clear', 'accept'],
					}
				}}
				PopperProps={{
					placement: 'bottom-start'
				}}
			/>
		</LocalizationProvider>
	)
}

export default DateTimeFilterInput;
