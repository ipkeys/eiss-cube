import { useState, useEffect } from 'react';
import { Box, TextField, MenuItem } from '@mui/material';

const day_aggregations = [
	{ value: '1m',  label: '1 minute'},
	{ value: '5m',  label: '5 minutes'},
	{ value: '15m', label: '15 minutes'},
	{ value: '30m', label: '30 minutes'},
	{ value: '1h',  label: '1 hour'}
];
const week_aggregations = [
	{ value: '1h',  label: '1 hour'}
];
const month_aggregations = [
	{ value: '1h',  label: '1 hour'}
];
const year_aggregations = [
	{ value: '1h',  label: '1 hour'}
];

const SelectAggregation = (props: any) => {
	const { label, aggregation, daterange, onChange} = props;
	const [value, setValue] = useState(aggregation);

	useEffect(() => {
		setValue(aggregation);
	}, [aggregation]);

	const handleChange = (event: any) => {
		onChange(event.target.value);
	};

	const toMenu = (option: any) => (
		<MenuItem key={option.value} value={option.value}>
			{option.label}
		</MenuItem>
	);

	let renderDropDown;
	switch(daterange) {
		case 'w':
			renderDropDown = week_aggregations.map(toMenu);
			break;
		case 'm':
			renderDropDown = month_aggregations.map(toMenu);
			break;
		case 'y':
			renderDropDown = year_aggregations.map(toMenu);
			break;
		case 'd':
		default:
			renderDropDown = day_aggregations.map(toMenu);
			break;
	}

	return (
		<Box
			component='form'
			autoComplete='off'
			noValidate
		>
			<TextField sx={{mr: 2, width: '150px'}}
				id='select-aggregation'
				select
				label={label}
				value={value}
				onChange={handleChange}
				size='small'
			>
				{renderDropDown}
			</TextField>
		</Box>
	);
}

export default SelectAggregation;
