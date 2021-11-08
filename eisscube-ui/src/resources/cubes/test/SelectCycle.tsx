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

const cycles = [
	{ value: 10, label: '10 seconds'},
	{ value: 15, label: '15 seconds'},
	{ value: 30, label: '30 seconds'}
];

const SelectCycle = (props: any) => {
	const { onChange } = props;
	const classes = useStyles();
	const [cycle, setCycle] = useState<number>(10);

	onChange(cycle);

	return (
		<form className={classes.container} noValidate autoComplete="off">
			<TextField
				id="select-cycle"
				select
				label="Select cycle"
				className={classes.textField}
				value={cycle}
				onChange={(event) => setCycle(parseInt(event.target.value))}
				SelectProps={{
					MenuProps: {
						className: classes.menu,
					},
				}}
				margin="normal"
			>
				{cycles.map(option => (
					<MenuItem key={option.value} value={option.value}>
						{option.label}
					</MenuItem>
				))}
			</TextField>
		</form>
	);
}

SelectCycle.propTypes = {
	onChange: PropTypes.func
};

export default SelectCycle;
