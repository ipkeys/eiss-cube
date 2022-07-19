import { useState, useEffect } from 'react';
import {
	Button,
	useDataProvider,
} from 'react-admin';
import {
	Box,
	Stepper,
	Step,
	StepButton,
	StepContent
} from '@mui/material';
import BackIcon from '@mui/icons-material/ArrowUpward';
import SaveIcon from '@mui/icons-material/Save';
import NextIcon from '@mui/icons-material/ArrowDownward';
import RelayForm from './RelayForm';
import InputForm from './InputForm';

const SetupEissCube = (props: any) => {
	const { cubeID, deviceType } = props;
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
				<StepContent>
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
				<StepContent>
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
	const { step, onNext, onBack } = props;

	return (
		<Box sx={{mt: 1}}>
			<Button
				label='Back'
				disabled={step === 0}
				onClick={onBack}
				sx={{mr: 1}}
			>
				<BackIcon />
			</Button>
			<Button
				label='Save'
				type='submit'
				variant="contained"
				sx={{mr: 1}}
			>
				<SaveIcon />
			</Button>
			<Button
				label='Next'
				disabled={step === 1}
				onClick={onNext}
				sx={{mr: 1}}
			>
				<NextIcon />
			</Button>
		</Box>
	);
}
