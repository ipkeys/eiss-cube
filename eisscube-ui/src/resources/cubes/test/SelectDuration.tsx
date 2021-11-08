import { useState } from 'react';
import { makeStyles, Theme } from '@material-ui/core/styles';
import PropTypes from 'prop-types';
import { TextField, MenuItem } from '@material-ui/core';

const useStyles = makeStyles((theme: Theme) => ({ 
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
}));

const durations = [
	{ value: 60, label: '1 minute'},
	{ value: 120, label: '2 minutes'},
	{ value: 180, label: '3 minutes'}
];

const SelectDuration = (props: any) => {
	const { onChange } = props;
	const classes = useStyles();
	const [duration, setDuration] = useState<number>(60);

	onChange(duration);

	return (
		<form className={classes.container} noValidate autoComplete="off">
			<TextField
				id="select-duration"
				select
				label="Select duration"
				className={classes.textField}
				value={duration}
				onChange={(event) => setDuration(parseInt(event.target.value))}
				SelectProps={{
					MenuProps: {
						className: classes.menu,
					},
				}}
				margin="normal"
			>
				{durations.map(option => (
					<MenuItem key={option.value} value={option.value}>
						{option.label}
					</MenuItem>
				))}
			</TextField>
		</form>
	);
}

SelectDuration.propTypes = {
	onChange: PropTypes.func
};

export default SelectDuration;
