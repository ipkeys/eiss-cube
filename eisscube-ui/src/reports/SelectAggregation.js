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

const aggregations = [
	{ value: '1m',  label: '1 minute'},
	{ value: '5m',  label: '5 minutes'},
	{ value: '15m', label: '15 minutes'},
	{ value: '30m', label: '30 minutes'},
	{ value: '1h',  label: '1 hour'}
];

class SelectAggregation extends Component {
	state = {
		aggregation: '1h',
	};

	handleChange = event => {
		const { onChange } = this.props;

		onChange(event.target.value);
		
		this.setState({
			aggregation: event.target.value,
		});
	};

	render() {
		const { classes } = this.props;
		const { aggregation } = this.state;

		return (
			<form className={classes.container} noValidate autoComplete='off'>
				<TextField
					id='select-aggregation'
					select
					label={false}
					className={classes.textField}
					value={aggregation}
					onChange={this.handleChange}
					SelectProps={{
						MenuProps: {
							className: classes.menu,
						},
					}}
					margin='dense'
				>
					{aggregations.map(option => (
						<MenuItem key={option.value} value={option.value}>
							{option.label}
						</MenuItem>
					))}
				</TextField>
			</form>
		);
	}
}

SelectAggregation.propTypes = {
	classes: PropTypes.object.isRequired,
	onChange: PropTypes.func
};

export default withStyles(styles)(SelectAggregation);
