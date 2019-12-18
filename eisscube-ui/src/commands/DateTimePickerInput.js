import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { addField, FieldTitle } from 'react-admin';
import MomentUtils from 'material-ui-pickers/utils/moment-utils';

import { 
	MuiPickersUtilsProvider, 
	DatePicker, 
	TimePicker, 
	DateTimePicker,
	InlineDateTimePicker
} from 'material-ui-pickers';

const makePicker = (PickerComponent) => {
	class _makePicker extends Component {
		render() {
			const {
				input,
				options,
				label,
				source,
				resource,
				isRequired,
				className,
				meta,
				providerOptions,
			} = this.props;

			const { touched, error } = meta;
			const { value, onChange } = input;

			return (
				<MuiPickersUtilsProvider {...providerOptions}>
					<PickerComponent
						{...options}
						label={
							<FieldTitle
								label={label}
								source={source}
								resource={resource}
								isRequired={isRequired}
							/>
						}
						margin="normal"
						error={!!(touched && error)}
						helperText={touched && error}
						ref={(node) => { this.picker = node; }}
						className={className}
						value={value ? value : null}
						onChange={(date) => { 
							if (date) {
								onChange(date._d); 
							} else {
								onChange(null); 
							}
						}}
					/>
				</MuiPickersUtilsProvider>
			);
    	}
	}

	_makePicker.propTypes = {
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

	_makePicker.defaultProps = {
		input: {},
		isRequired: false,
		label: '',
		meta: { touched: false, error: false },
		options: {},
		resource: '',
		source: '',
		labelTime: '',
		className: '',
		providerOptions: {
			utils: MomentUtils,
			locale: undefined
		},
	};

	return _makePicker;
};

export const DateInput = addField(makePicker(DatePicker));
export const TimeInput = addField(makePicker(TimePicker));
export const DateTimeInput = addField(makePicker(DateTimePicker));
export const DateTimeInlineInput = addField(makePicker(InlineDateTimePicker));

