import { Box } from '@mui/material';
import ChevronLeft from '@mui/icons-material/ChevronLeft';
import {
	Create,
	SelectInput,
	SimpleForm,
	TextInput,
	TopToolbar,
	ListButton,
	required,
	maxLength
} from 'react-admin';
import { PropertyTypes } from '../common';

const PropertyCreateActions = () => (
	<TopToolbar>
		<ListButton label="Back" icon={<ChevronLeft />} />
	</TopToolbar>
);

const validateValue = [required(), maxLength(20)];

const PropertyCreate = () => (
	<Create title='Create a new Property'
		redirect='list'
		actions={<PropertyCreateActions />}
	>
		<SimpleForm>
			<SelectInput source='type' choices={PropertyTypes} sx={{minWidth: '20em'}}/>

			<Box display={'inline-flex'}>
				<Box flex={1} mr={2} minWidth={'20em'} >
					<TextInput label='Name' source='name' validate={validateValue} fullWidth />
				</Box>
				<Box flex={1} minWidth={'20em'} >
					<TextInput label='Label' source='label' validate={validateValue} fullWidth />
				</Box>
			</Box>

			<TextInput label='Description' source='description' validate={[maxLength(50)]} sx={{minWidth: '41em'}} />
		</SimpleForm>
	</Create>
);

export default PropertyCreate;
