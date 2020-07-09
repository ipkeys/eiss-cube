import React, { Component } from 'react';
import PropTypes from 'prop-types';
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

const cycles = [
	{ value: 5, label: '5 seconds'},
	{ value: 10, label: '10 seconds'},
	{ value: 15, label: '15 seconds'},
	{ value: 30, label: '30 seconds'}
];

class SelectCycle extends Component {
	state = {
		cycle: 5,
	};

	handleChange = name => event => {
		this.setState({
			[name]: event.target.value,
		});
	};

	render() {
		const { classes, onChange } = this.props;
		const { cycle } = this.state;

		onChange(cycle);

		return (
			<form className={classes.container} noValidate autoComplete="off">
				<TextField
					id="select-cycle"
					select
					label="Select cycle"
					className={classes.textField}
					value={this.state.cycle}
					onChange={this.handleChange('cycle')}
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
}

SelectCycle.propTypes = {
	classes: PropTypes.object.isRequired,
	onChange: PropTypes.func
};

export default withStyles(styles)(SelectCycle);
