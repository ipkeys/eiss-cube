import { useState } from 'react';
import {
	Box,
	Slider,
	alpha
} from '@mui/material';
import { grey, green, red } from '@mui/material/colors';
import { useInput } from 'react-admin';
import { useWatch, useFormContext } from 'react-hook-form';

const DutyCycleInput = (props: any) => {
	const { setValue } = useFormContext();
	const { field } = useInput(props);
	const cycle = useWatch({ name: 'completeCycle', defaultValue: 0 });
    const [duty, setDuty] = useState<number>(0);

    const on = Math.round(cycle * duty / 100);
    const off = Math.round(cycle - on);

	return (
		<Box sx={{
			mt: 1,
			width: '20em',
			height: '48px',
			position: 'relative',
			backgroundColor: 'rgba(0, 0, 0, 0.04)',
			borderTopLeftRadius: 4,
			borderTopRightRadius: 4,
			'&:hover': {
				backgroundColor: alpha(grey[400], 0.4),
				'@media (hover: none)': {
					backgroundColor: 'transparent',
				},
			}
		}}
		>
			<Box sx={{fontSize: '0.7em', pt: 1, pl: 1.5}} >
				<span style={{color: grey[700]}}>Duty Cycle </span>{duty}% (<span style={{color: green[500]}}>{on} sec ON</span> / <span style={{color: red[500]}}>{off} sec OFF</span>)
			</Box>
			<Slider sx={{p: '24px 0'}}
				{...field}
				step={1}
				onChange={(_event: any, value: any) => {
					setDuty(Array.isArray(value) ? value[0] : value);
					setValue('dutyCycle', duty);
				}}
				value={duty}
			/>
		</Box>
	);
};

export default DutyCycleInput;
