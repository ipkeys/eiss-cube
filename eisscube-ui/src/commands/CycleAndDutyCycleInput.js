import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { withStyles } from '@material-ui/core/styles';
import TextField from '@material-ui/core/TextField';
import FormGroup from '@material-ui/core/FormGroup';
import FormControl from '@material-ui/core/FormControl';
import FormLabel from '@material-ui/core/FormLabel';
import Slider from '@material-ui/lab/Slider';
import { green, red, lightBlue } from '@material-ui/core/colors';
import { FieldTitle } from 'react-admin';

const styles = theme => ({
    textInput: {
        minWidth: theme.spacing.unit * 32,
        marginRight: theme.spacing.unit * 2, 
    },
    sliderInput: {
        marginTop: theme.spacing.unit * 2,
        width: theme.spacing.unit * 32
    },
    slider: {
        marginTop: 34 
    },
    green: { color: green[500] },
    red: { color: red[500] },
    blue: { color: lightBlue[800] },
    grey: { color: 'rgba(0, 0, 0, 0.54)' }
});

class CycleAndDutyCycleInput extends Component {
    constructor(props) {
        super(props);

        this.state = {
            cycle: 0,
            duty: 0
        };

        this.handleCycleChange = this.handleCycleChange.bind(this);
        this.handleDutyCycleChange = this.handleDutyCycleChange.bind(this);
    }

    handleCycleChange = (event) => {
        const value = parseInt(event.target.value);
        this.setState({cycle: value});
    }

    handleDutyCycleChange = (event, value) => {
        this.setState({ duty: value });
    };

    render() {
        const { classes, options, source, margin } = this.props;
        const { cycle, duty } = this.state;

        const on = isNaN(cycle) && isNaN(duty) ? 0 :  Math.round(cycle * duty / 100);
        const off = isNaN(cycle) ? 0 : Math.round(cycle - on);

        return (
            <FormGroup row>
                <TextField
                    className={classes.textInput}
                    type="number"
                    step={1}
                    onChange={this.handleCycleChange}
                    label={<FieldTitle label="Cycle (sec)" />}
                    margin={margin ? margin : 'normal'}
                    {...options}
                />
                <FormControl component="fieldset" className={classes.sliderInput}>
                    <FormLabel component="legend" style={{fontSize: '0.7em'}}>
                        <span className={classes.grey}>Duty Cycle </span>{duty}% (<span className={classes.green}>{on} sec ON</span> / <span className={classes.red}>{off} sec OFF</span>)
                    </FormLabel>
                    <Slider
                            className={classes.slider}
                            step={1}
                            onChange={this.handleDutyCycleChange}
                            value={duty}
                        />
                </FormControl>
                <input
                    type="hidden"
                    name={source}
                    value={`${cycle}/${duty}`}
                />
            </FormGroup>
        );
    }
}

CycleAndDutyCycleInput.propTypes = {
  options: PropTypes.object,
  source: PropTypes.string.isRequired
};

export default withStyles(styles)(CycleAndDutyCycleInput);
