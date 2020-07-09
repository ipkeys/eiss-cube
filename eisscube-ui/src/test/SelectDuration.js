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

const durations = [
	{ value: 20, label: '20 seconds'},
	{ value: 60, label: '1 minute'},
	{ value: 120, label: '2 minutes'},
	{ value: 180, label: '3 minutes'}
];

class SelectDuration extends Component {
	state = {
		duration: 20,
	};

	handleChange = name => event => {
		this.setState({
			[name]: event.target.value,
		});
	};

	render() {
		const { classes, onChange } = this.props;
		const { duration } = this.state;

		onChange(duration);

		return (
			<form className={classes.container} noValidate autoComplete="off">
				<TextField
					id="select-duration"
					select
					label="Select duration"
					className={classes.textField}
					value={this.state.duration}
					onChange={this.handleChange('duration')}
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
}

SelectDuration.propTypes = {
	classes: PropTypes.object.isRequired,
	onChange: PropTypes.func
};

export default withStyles(styles)(SelectDuration);
