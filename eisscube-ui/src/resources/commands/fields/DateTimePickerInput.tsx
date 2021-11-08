import { useCallback } from 'react';
import PropTypes from 'prop-types';
import { useInput, FieldTitle } from 'ra-core';
import { KeyboardDateTimePicker, DatePicker, TimePicker, DateTimePicker, MuiPickersUtilsProvider } from '@material-ui/pickers';
import MomentUtils from '@date-io/moment';

// @ts-ignore
const Picker = ({ PickerComponent, ...fieldProps }) => {

	const {
		options,
		label,
		source,
		resource,
		className,
		isRequired,
		providerOptions,
	} = fieldProps;

	const { input, meta } = useInput({ source });
	
	const { touched, error } = meta;
	
	const handleChange = useCallback(value => {
		Date.parse(value) ? input.onChange(value.toISOString()) : input.onChange(null);
	}, [input]);

	return (
		<div className="picker">
			<MuiPickersUtilsProvider {...providerOptions}>
				<PickerComponent
					{...options}
					label={<FieldTitle
						label={label}
						source={source}
						resource={resource}
						isRequired={isRequired}
					/>}
					error={!!(touched && error)}
					helperText={touched && error}
					className={className}
					value={input.value ? new Date(input.value) : null}
					onChange={(date: any) => handleChange(date)}
					// @ts-ignore
					onBlur={() => input.onBlur(input.value ? new Date(input.value).toISOString() : null)}
				/>
			</MuiPickersUtilsProvider>
		</div>
	)
}

Picker.propTypes = {
	input: PropTypes.object,
	isRequired: PropTypes.bool,
	label: PropTypes.string,
	meta: PropTypes.object,
	options: PropTypes.object,
	resource: PropTypes.string,
	source: PropTypes.string,
	labelTime: PropTypes.string,
	className: PropTypes.string,
	providerOptions: PropTypes.shape({
		utils: PropTypes.func,
		locale: PropTypes.oneOfType([PropTypes.object, PropTypes.string]),
	}),
};

Picker.defaultProps = {
	input: {},
	isRequired: false,
	meta: { touched: false, error: false },
	options: {},
	resource: '',
	source: '',
	labelTime: '',
	className: '',
	providerOptions: {
		utils: MomentUtils,
		locale: undefined,
	},
};

export const DateInput = (props: any) => <Picker PickerComponent={DatePicker} {...props} />
export const TimeInput = (props: any) => <Picker PickerComponent={TimePicker} {...props} />
export const DateTimeInput = (props: any) => <Picker PickerComponent={DateTimePicker} {...props} />

export const DateTimeFilterInput = (props: any) => <Picker PickerComponent={KeyboardDateTimePicker} {...props} />
export const DateTimeFormInput = (props: any) => <Picker PickerComponent={KeyboardDateTimePicker} {...props} />
