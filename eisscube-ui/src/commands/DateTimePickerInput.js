import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { addField, FieldTitle } from 'react-admin';
import MomentUtils from '@date-io/moment';
import { 
	MuiPickersUtilsProvider, 
	KeyboardDateTimePicker
} from '@material-ui/pickers';

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
						error={!!(touched && error)}
						helperText={touched && error}
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
		options: { },
		resource: '',
		source: '',
		className: '',
		providerOptions: {
			utils: MomentUtils,
			locale: undefined
		},
	};

	return _makePicker;
};

export const DateTimeFilterInput = addField(makePicker(KeyboardDateTimePicker));
export const DateTimeFormInput = addField(makePicker(KeyboardDateTimePicker));

