import React, { Component } from 'react';
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
    green: { 
        color: green[500] 
    },
    red: { 
        color: red[500] 
    },
    blue: { 
        color: lightBlue[800] 
    },
    grey: { 
        color: 'rgba(0, 0, 0, 0.54)' 
    },
    legend: {
        fontSize: '0.7em'
    }
});

class CycleAndDutyCycleInput extends Component {
    constructor(props) {
        super(props);
        this.state = {
            cycle: 0,
            duty: 0
        };
    }

    render() {
        const { classes, input: { onChange }, meta: { touched, error }, options, margin } = this.props;
        const { cycle, duty } = this.state;

        const on = isNaN(cycle) && isNaN(duty) ? 0 :  Math.round(cycle * duty / 100);
        const off = isNaN(cycle) ? 0 : Math.round(cycle - on);

        return (
            <FormGroup row>
                <TextField
                    className={classes.textInput}
                    type="number"
                    step={1}
                    error={!!(touched && error)}
                    helperText={touched && error}    
                    onChange={(event) => {
                        this.setState({cycle: parseInt(event.target.value)});
                        onChange(`${cycle}/${duty}`);
                    }}
                    label={<FieldTitle label="Cycle (sec)" />}
                    margin={margin ? margin : 'normal'}
                    {...options}
                />
                <FormControl component="fieldset" className={classes.sliderInput} >
                    <FormLabel component="legend" className={classes.legend} >
                        <span className={classes.grey}>Duty Cycle </span>{duty}% (<span className={classes.green}>{on} sec ON</span> / <span className={classes.red}>{off} sec OFF</span>)
                    </FormLabel>
                    <Slider
                            className={classes.slider}
                            step={1}
                            onChange={(event, value) => {
                                this.setState({ duty: value });
                                onChange(`${cycle}/${duty}`);
                            }}
                            value={duty}
                        />
                </FormControl>
            </FormGroup>
        );
    }
}

export default withStyles(styles)(CycleAndDutyCycleInput);
