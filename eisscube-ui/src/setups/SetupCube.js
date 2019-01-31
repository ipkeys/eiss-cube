import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { withStyles } from '@material-ui/core/styles';
import Stepper from '@material-ui/core/Stepper';
import Step from '@material-ui/core/Step';
import StepButton from '@material-ui/core/StepButton';
import StepContent from '@material-ui/core/StepContent';
import BackIcon from '@material-ui/icons/ArrowUpward';
import SaveIcon from '@material-ui/icons/Save';
import NextIcon from '@material-ui/icons/ArrowDownward';

import { Button, GET_ONE, CREATE } from 'react-admin';
import { dataProvider } from '../App';

import RelayForm from './RelayForm';
import InputForm from './InputForm';

const styles = theme => ({
    btnRoot: {
        marginTop: theme.spacing.unit
    },
    btnPadding: {
        marginRight: theme.spacing.unit
    },
    stepContent: {
        padding: 0
    }
});

class SetupEissCube extends Component {

    constructor(props) {
        super(props);
        this.state = {
            stepIndex: 0
        };

        this.handleNext = this.handleNext.bind(this);
        this.handlePrev = this.handlePrev.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    componentWillMount() {
        dataProvider(GET_ONE, `setup`, {
            id: this.props.deviceID
        })
        .then(response => response.data)
        .then(data => {
            if (data) {
                this.setState({
                    data
                });
            }
        });
    }

    handleSubmit = (values) => {
        dataProvider(CREATE, `setup`, {
            data: { ...values, deviceID: this.props.deviceID}
        })
        .then(response => response.data)
        .then(data => {
            if (data) {
                this.setState({
                    data: data
                });
            }
        });
    };

    handleNext = () => {
        const {stepIndex} = this.state;
        this.setState({
            stepIndex: stepIndex + 1
        });
    };

    handlePrev = () => {
        const {stepIndex} = this.state;
        if (stepIndex > 0) {
            this.setState({
                stepIndex: stepIndex - 1
            });
        }
    };

    render() {
        const { classes } = this.props;
        const { data, stepIndex } = this.state;

        return (
            <Stepper nonLinear activeStep={stepIndex} orientation="vertical">
                <Step>
                    <StepButton onClick={() => this.setState({stepIndex: 0})}>
                        RELAY
                    </StepButton>
                    <StepContent className={classes.stepContent} >
                        <RelayForm
                            data={data}
                            step={stepIndex}
                            onSubmit={this.handleSubmit}
                            next={this.handleNext}
                            back={this.handlePrev}
                        />
                    </StepContent>
                </Step>
                <Step>
                    <StepButton onClick={() => this.setState({stepIndex: 1})}>
                        INPUT
                    </StepButton>
                    <StepContent className={classes.stepContent} >
                        <InputForm
                            data={data}
                            step={stepIndex}
                            onSubmit={this.handleSubmit}
                            next={this.handleNext}
                            back={this.handlePrev}
                        />
                    </StepContent>
                </Step>
            </Stepper>
        );
    }
}

SetupEissCube.propTypes = {
    deviceID: PropTypes.string
};

export default withStyles(styles)(SetupEissCube);

export const SetupFormButton = withStyles(styles)(
        ({ classes, step, onNext, onBack, onSave, pristine, submitting }) => (
        <div className={classes.btnRoot}>
            <Button
                label='Back'
                disabled={step === 0}
                onClick={onBack}
                className={classes.btnPadding}
            >
                <BackIcon />
            </Button>
            <Button
                label='Save'
                variant="contained"
                disabled={pristine || submitting}
                onClick={onSave}
                className={classes.btnPadding}
            >
                <SaveIcon />
            </Button>
            <Button
                label='Next'
                disabled={step === 1}
                onClick={onNext}
                className={classes.btnPadding}
            >
                <NextIcon />
            </Button>
        </div>
    )
)
