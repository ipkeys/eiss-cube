import { Box } from '@mui/material';
import ChevronLeft from '@mui/icons-material/ChevronLeft';
import {
    Create,
    SimpleForm,
	DateTimeInput,
    NumberInput,
    SelectInput,
    ReferenceInput,
    AutocompleteInput,
    FormDataConsumer,
    TopToolbar,
    ListButton,
    required,
} from 'react-admin';
import moment from 'moment';
import DutyCycleInput from './fields/DutyCycleInput';
import { cmds, edges, checkCommandForInputCycle, checkCommandForRelayCycle, checkCommandForInputCount } from '.';

const CommandCreateActions = () => (
	<TopToolbar sx={{mt: 1}}>
		<ListButton label="Back" icon={<ChevronLeft />} />
	</TopToolbar>
);

const validateCommandCreation = (values: any) => {
    const errors = {} as any;

    if (values.completeCycle < 1) {
        errors.completeCycle = 'Must be positive more then 1 seconds';
    }

    let s = (values.startTime) ? values.startTime : null;
    let e = (values.endTime) ? values.endTime : null;

    if (s !== null) {
        let sDate = moment(s);
        if (moment(sDate).isBefore()) {
            errors.startTime = 'Must be after of the current moment';
        }
    }
    if (e !== null) {
        let eDate = moment(e);
        if (moment(eDate).isBefore()) {
            errors.endTime = 'Must be after of the current moment';
        }
    }
    if (s !== null && e !== null) {
        let sDate = moment(s);
        let eDate = moment(e);
        if (moment(eDate).isSameOrBefore(sDate)) {
            errors.endTime = 'Must be after Start Date, Time';
        }
    }

    return errors;
};

export const CommandCreate = () => (
	<Create title='Create a new Command'
		redirect='list'
		actions={<CommandCreateActions />}
	>
		<SimpleForm validate={validateCommandCreation} >

			<ReferenceInput source='cubeID' reference='cubes' validate={required()} >
				<AutocompleteInput label='for EISS™Cube' optionText='name' sx={{minWidth: '20em'}} />
			</ReferenceInput>

			<SelectInput label='Command' source='command' choices={cmds.filter(value => value.id !== 'reboot')} validate={required()} sx={{minWidth: '20em'}} />

			<FormDataConsumer>
			{({ formData, ...rest }) => checkCommandForRelayCycle(formData.command) &&
				<Box display={'inline-flex'}>
					<Box flex={1} mr={2} minWidth={'20em'} >
						<NumberInput label='Cycle (sec)' source='completeCycle' step={1} min={1} fullWidth {...rest} />
					</Box>
					<Box flex={1} minWidth={'20em'} >
						<DutyCycleInput source='dutyCycle' fullWidth {...rest} />
					</Box>
				</Box>
			}
			</FormDataConsumer>

			<FormDataConsumer>
			{({ formData, ...rest }) => checkCommandForInputCount(formData.command) &&
				<Box display={'inline-flex'}>
					<Box flex={1} mr={2} minWidth={'20em'} >
						<NumberInput label='Cycle (sec)' source='completeCycle' step={1} min={1} fullWidth {...rest} />
					</Box>
					<Box flex={1} minWidth={'20em'} >
						<SelectInput label='Transition' source='transition' choices={edges} validate={required()} fullWidth {...rest} />
					</Box>
				</Box>
			}
			</FormDataConsumer>

			<FormDataConsumer>
			{({ formData, ...rest }) => checkCommandForInputCycle(formData.command) &&
				<SelectInput label='Transition' source='transition' choices={edges} validate={required()} sx={{minWidth: '20em'}} {...rest} />
			}
			</FormDataConsumer>

			<Box display={'inline-flex'}>
				<Box flex={1} mr={2} minWidth={'20em'} >
					<DateTimeInput fullWidth
						label='Start Date, Time'
						source='startTime'
					/>
				</Box>
				<Box flex={1} minWidth={'20em'} >
					<DateTimeInput fullWidth
						label='End Date, Time'
						source='endTime'
					/>
				</Box>
			</Box>

		</SimpleForm>
	</Create>
);

export default CommandCreate;
