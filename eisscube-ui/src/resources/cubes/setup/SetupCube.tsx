import { useState, useCallback, useEffect } from 'react';
import { makeStyles, Theme } from '@material-ui/core/styles';
import {
    Button,
    useDataProvider,
} from 'react-admin';
import { 
    Stepper,
    Step,
    StepButton,
    StepContent
} from '@material-ui/core';
import BackIcon from '@material-ui/icons/ArrowUpward';
import SaveIcon from '@material-ui/icons/Save';
import NextIcon from '@material-ui/icons/ArrowDownward';
import { useForm } from 'react-final-form';

import RelayForm from './RelayForm';
import InputForm from './InputForm';

const useStyles = makeStyles((theme: Theme) => ({ 
    btnRoot: {
        marginTop: theme.spacing(1)
    },
    btnPadding: {
        marginRight: theme.spacing(1)
    },
    stepContent: {
        padding: 0
    }
}));

const SetupEissCube = (props: any) => {
    const { cubeID, deviceType } = props;
    const classes = useStyles();
    const [step, setStep] = useState<number>(0);
    const [setup, setSetup] = useState({});
    const dataProvider = useDataProvider();

	useEffect(() => {
        dataProvider.getOne('setup', {
            id: cubeID
        })
        .then(response => response.data)
        .then(data => setSetup(data));
	}, [cubeID, dataProvider]);

    const handleSubmit = (values: any) => {
        dataProvider.create('setup', {
            data: {
                ...values, 
                cubeID: cubeID,
                deviceType: deviceType
            }
        })
        .then(response => response.data)
        .then(data => setSetup(data));
    };

    const handleNext = () => {
        setStep(step + 1);
    };

    const handlePrev = () => {
        if (step > 0) {
            setStep(step - 1);
        }
    };

    return (
        <Stepper nonLinear activeStep={step} orientation="vertical">
            <Step>
                <StepButton onClick={() => setStep(0)}>
                    RELAY
                </StepButton>
                <StepContent className={classes.stepContent} >
                    <RelayForm
                        data={setup}
                        step={step}
                        onSubmit={handleSubmit}
                        next={handleNext}
                        back={handlePrev}
                    />
                </StepContent>
            </Step>
            <Step>
                <StepButton onClick={() => setStep(1)}>
                    INPUT
                </StepButton>
                <StepContent className={classes.stepContent} >
                    <InputForm
                        data={setup}
                        step={step}
                        onSubmit={handleSubmit}
                        next={handleNext}
                        back={handlePrev}
                    />
                </StepContent>
            </Step>
        </Stepper>
    );
}

export default SetupEissCube;

export const SetupFormButton = (props: any) => {
    const { step, onNext, onBack, pristine, submitting } = props;
    const classes = useStyles();
    const form = useForm();

    const handleClick = useCallback(() => {
        form.submit();
    }, [form]);

    return (
        <div className={classes.btnRoot}>
            <Button
                label='Back'
                disabled={step === 0 || !(pristine || submitting)}
                onClick={onBack}
                className={classes.btnPadding}
            >
                <BackIcon />
            </Button>
            <Button
                label='Save'
                variant="contained"
                disabled={pristine || submitting}
                onClick={handleClick}
                className={classes.btnPadding}
            >
                <SaveIcon />
            </Button>
            <Button
                label='Next'
                disabled={step === 1 || !(pristine || submitting)}
                onClick={onNext}
                className={classes.btnPadding}
            >
                <NextIcon />
            </Button>
        </div>
    );
}
