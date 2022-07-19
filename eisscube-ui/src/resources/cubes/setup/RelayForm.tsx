import {
	Form,
	BooleanInput,
	SelectInput,
	TextInput,
	FormDataConsumer
} from 'react-admin';
import { SetupFormButton } from './SetupCube';

const RelaySettings = ({formData}: {formData: any}) => (
	formData && formData.relay && formData.relay.connected ?
	<>
		<SelectInput label='To contacts' source='relay.contacts' choices={[
			{ id: 'NO', name: 'Normal Open' },
			{ id: 'NC', name: 'Normal Close' }
		]} fullWidth />
		<TextInput label='Label' source='relay.label' fullWidth />
		<TextInput label='Description' source='relay.description' fullWidth />
	</>
	: null
);

const RelayForm = (props: any) => {
	const { data, onSubmit, step, back, next } = props

	return (
		<Form defaultValues={data} onSubmit={onSubmit} >
			<BooleanInput label='Connected' source='relay.connected' />

			<FormDataConsumer>
				{formDataProps => <RelaySettings {...formDataProps} /> }
			</FormDataConsumer>

			<SetupFormButton step={step} onNext={next} onBack={back} />
		</Form>
	);
}

export default RelayForm;
