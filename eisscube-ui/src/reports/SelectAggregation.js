import React, { useState, useEffect } from 'react';
import { withStyles } from '@material-ui/core/styles';
import MenuItem from '@material-ui/core/MenuItem';
import TextField from '@material-ui/core/TextField';

const styles = theme => ({
	container: {
		marginTop: 0,
		marginBottom: 0,
		display: 'inline-flex',
		flexWrap: 'wrap'
	},
	textField: {
		marginLeft: theme.spacing(1),
		marginRight: theme.spacing(1),
		width: 120
	},
	menu: {
		width: 120
	}
});

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

function SelectAggregation({classes, aggregation, daterange, onChange}) {
	const [value, setValue] = useState(aggregation);

	useEffect(() => {
		setValue(aggregation);
	}, [aggregation]);

	const handleChange = event => {
		setValue(event.target.value);
		onChange(event.target.value);
	};
	
	let renderDropDown;
	switch(daterange) {
		case 'w':
			renderDropDown = week_aggregations.map(option => (
				<MenuItem key={option.value} value={option.value}>
					{option.label}
				</MenuItem>
			));
			break;
		case 'm':
			renderDropDown = month_aggregations.map(option => (
				<MenuItem key={option.value} value={option.value}>
					{option.label}
				</MenuItem>
			));
			break;
		case 'y':
			renderDropDown = year_aggregations.map(option => (
				<MenuItem key={option.value} value={option.value}>
					{option.label}
				</MenuItem>
			));
			break;
		case 'd':
		default:
			renderDropDown = day_aggregations.map(option => (
				<MenuItem key={option.value} value={option.value}>
					{option.label}
				</MenuItem>
			));
			break;
	}

	return (
		<form className={classes.container} noValidate autoComplete='off'>
			<TextField
				id='select-aggregation'
				select
				label={false}
				className={classes.textField}
				value={value}
				onChange={handleChange}
				SelectProps={{
					MenuProps: {
						className: classes.menu,
					},
				}}
				margin='dense'
			>
				{renderDropDown}
			</TextField>
		</form>
	);
}

export default withStyles(styles)(SelectAggregation);
