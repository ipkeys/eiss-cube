import { useState } from 'react';
import { makeStyles, Theme } from '@material-ui/core/styles';
import { Box, TextField, FormGroup, FormControl } from '@material-ui/core';
import Slider from '@material-ui/core/Slider';
import { grey, green, red, lightBlue } from '@material-ui/core/colors';
import { FieldTitle, useInput } from 'react-admin';
import { alpha } from '@material-ui/core/styles/colorManipulator';

const useStyles = makeStyles((theme: Theme) => ({ 
    textInput: {
        minWidth: theme.spacing(32),
        marginRight: theme.spacing(2), 
        marginButtom: theme.spacing(2), 
    },
    sliderInput: {
        marginTop: 8,
        paddingTop: 4,
        width: theme.spacing(32),
        height: 42,
        position: 'relative',
        backgroundColor: 'rgba(0, 0, 0, 0.02)',
        borderTopLeftRadius: 4,
        borderTopRightRadius: 4,
        '&:hover': {
            backgroundColor: alpha(grey[400], 0.4),
            '@media (hover: none)': {
                backgroundColor: 'transparent',
            },
        }
    },
    slider: {
        marginTop: 13,
        paddingRight: 0,
        paddingLeft: 0 
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
        color: grey[600] 
    },
    legend: {
        fontSize: '0.7em',
        paddingTop: 4,
        paddingLeft: 12
    }
}));

const CycleAndDutyCycleInput = (props: any) => {
    const classes = useStyles();
    const { options } = props;
    const {
        input: { onChange },
        meta: { touched, error }
    } = useInput(props);

    const [cycle, setCycle] = useState<number>(0);
    const [duty, setDuty] = useState<number>(0);

    const on = Math.round(cycle * duty / 100);
    const off = Math.round(cycle - on);

    onChange(`${cycle}/${duty}`);

    return (
        <FormGroup row>
            <TextField
                className={classes.textInput}
                type='number'
                // @ts-ignore
                step={1}
                error={!!(touched && error)}
                helperText={touched && error}    
                onChange={(event) => {
                    setCycle(parseInt(event.target.value));
                }}
                label={<FieldTitle label='Cycle (sec)' />}
                {...options}
            />
            <FormControl className={classes.sliderInput} >
                <Box className={classes.legend} >
                    <span className={classes.grey}>Duty Cycle </span>{duty}% (<span className={classes.green}>{on} sec ON</span> / <span className={classes.red}>{off} sec OFF</span>)
                </Box>
                <Slider className={classes.slider}
                    step={1}
                    onChange={(event, value) => {
                        setDuty(Array.isArray(value) ? value[0] : value);
                    }}
                    value={duty}
                />
            </FormControl>
        </FormGroup>
    );
}

export default CycleAndDutyCycleInput;
