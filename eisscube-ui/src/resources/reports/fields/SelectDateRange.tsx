import { useState, useEffect } from 'react';
import { Box, TextField, MenuItem } from '@mui/material';

const dateranges = [
	{ value: 'd', label: '1 day'},
	{ value: 'w', label: '1 week'},
	{ value: 'm', label: '1 month'},
	{ value: 'y', label: '1 year'}
];

const SelectDateRange = (props: any) => {
	const { label, daterange, onChange } = props;
	const [value, setValue] = useState(daterange);

	useEffect(() => {
		setValue(daterange);
	}, [daterange]);

	const handleChange = (event: any) => {
		onChange(event.target.value);
	};

	return (
		<Box
			component='form'
			autoComplete='off'
			noValidate
		>
			<TextField sx={{mr: 2, width: '150px'}}
				id='select-daterange'
				select
				label={label}
				value={value}
				onChange={handleChange}
				size='small'
			>
				{dateranges.map(option => (
					<MenuItem key={option.value} value={option.value}>
						{option.label}
					</MenuItem>
				))}
			</TextField>
		</Box>
	);
}

export default SelectDateRange;
