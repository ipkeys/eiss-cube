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

const dateranges = [
	{ value: 'd', label: '1 day'},
	{ value: 'w', label: '1 week'},
	{ value: 'm', label: '1 month'},
	{ value: 'y', label: '1 year'}
];

function SelectDateRange({classes, daterange, onChange}) {
	const [value, setValue] = useState(daterange);

	useEffect(() => {
		setValue(daterange);
	}, [daterange]);

	const handleChange = event => {
		setValue(event.target.value);
		onChange(event.target.value);
	};

	return (
		<form className={classes.container} noValidate autoComplete='off'>
			<TextField
				id='select-daterange'
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
				{dateranges.map(option => (
					<MenuItem key={option.value} value={option.value}>
						{option.label}
					</MenuItem>
				))}
			</TextField>
		</form>
	);
}

export default withStyles(styles)(SelectDateRange);
