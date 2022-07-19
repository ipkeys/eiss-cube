import { useState } from 'react';
import { Form } from 'react-admin';
import { TextField, MenuItem } from '@mui/material';

const durations = [
	{ value: 60, label: '1 minute'},
	{ value: 120, label: '2 minutes'},
	{ value: 180, label: '3 minutes'}
];

const SelectDuration = (props: any) => {
	const { onChange } = props;
	const [duration, setDuration] = useState<number>(60);

	onChange(duration);

	return (
		<Form noValidate>
			<TextField
				id="select-duration"
				select
				label="Select duration"
				sx={{mt: -1, ml: 1, mr: 1, width: 140}}
				value={duration}
				onChange={(event) => setDuration(parseInt(event.target.value))}
				margin="normal"
			>
				{durations.map(option => (
					<MenuItem key={option.value} value={option.value}>
						{option.label}
					</MenuItem>
				))}
			</TextField>
		</Form>
	);
}

export default SelectDuration;
