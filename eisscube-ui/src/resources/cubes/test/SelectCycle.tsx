import { useState } from 'react';
import { Form } from 'react-admin';
import { TextField, MenuItem } from '@mui/material';

const cycles = [
	{ value: 10, label: '10 seconds'},
	{ value: 15, label: '15 seconds'},
	{ value: 30, label: '30 seconds'}
];

const SelectCycle = (props: any) => {
	const { onChange } = props;
	const [cycle, setCycle] = useState<number>(10);

	onChange(cycle);

	return (
		<Form noValidate>
			<TextField
				id="select-cycle"
				select
				label="Select cycle"
				sx={{mt: -1, ml: 1, mr: 1, width: 140}}
				value={cycle}
				onChange={(event) => setCycle(parseInt(event.target.value))}
				margin="normal"
			>
				{cycles.map(option => (
					<MenuItem key={option.value} value={option.value}>
						{option.label}
					</MenuItem>
				))}
			</TextField>
		</Form>
	);
}

export default SelectCycle;
